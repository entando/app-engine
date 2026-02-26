package org.entando.entando.keycloak.services;

import static java.util.Optional.ofNullable;
import static org.entando.entando.keycloak.services.mapping.DynamicMappingKind.GROUP;
import static org.entando.entando.keycloak.services.mapping.DynamicMappingKind.ROLEGROUP;
import static org.entando.entando.keycloak.services.mapping.DynamicMappingKind.ROLE;
import static org.entando.entando.keycloak.services.mapping.DynamicMappingKind.ROLEGROUPCLAIM;

import com.agiletec.aps.system.common.AbstractService;
import com.agiletec.aps.system.services.authorization.Authorization;
import com.agiletec.aps.system.services.authorization.AuthorizationManager;
import com.agiletec.aps.system.services.baseconfig.BaseConfigManager;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.group.GroupManager;
import com.agiletec.aps.system.services.role.Role;
import com.agiletec.aps.system.services.role.RoleManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.keycloak.services.mapping.DynamicMapping;
import org.entando.entando.keycloak.services.mapping.DynamicMappingElement;
import org.entando.entando.keycloak.services.mapping.DynamicMappingKind;
import org.entando.entando.keycloak.services.mapping.PersistKind;
import org.entando.entando.keycloak.services.oidc.OidcMappingHelper;
import org.entando.entando.keycloak.services.oidc.model.KeycloakUser;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;

public class KeycloakAuthorizationManager extends AbstractService {

    private static final EntLogger log = EntLogFactory.getSanitizedLogger(KeycloakAuthorizationManager.class);

    private static final String DEFAULT_SEPARATOR = "_SEP_";

    private final KeycloakConfiguration configuration;
    private final AuthorizationManager authorizationManager;
    private final GroupManager groupManager;
    private final RoleManager roleManager;
    private final BaseConfigManager configManager;

    private static final int GROUP_POSITION = 0;
    private static final int ROLE_POSITION = 1;

    private final XmlMapper xmlMapper = new XmlMapper();

    private final transient ReadWriteLock configUpdateLock = new ReentrantReadWriteLock();
    private final transient Lock readLock = configUpdateLock.readLock();
    private final transient Lock writeLock = configUpdateLock.writeLock();

    @Autowired
    public KeycloakAuthorizationManager(final KeycloakConfiguration configuration,
            final AuthorizationManager authorizationManager,
            final GroupManager groupManager,
            final RoleManager roleManager,
            final BaseConfigManager configManager1) {
        this.configuration = configuration;
        this.authorizationManager = authorizationManager;
        this.groupManager = groupManager;
        this.roleManager = roleManager;
        this.configManager = configManager1;
    }

    /**
     * Immutable list of mapping elements that are currently active. The configuration is constantly updated
     * either by reloading the global configuration or after a certain amount of time (by default,
     * one minute)
     */
    private transient List<DynamicMappingElement> profileMappings;
    private transient List<DynamicMappingElement> jwtMappings;
    private transient List<String> ignore;
    private transient List<String> roles;
    private transient List<String> groups;
    private transient Boolean enabled;
    private transient PersistKind persist;

    @Override
    public void init() throws Exception {
        writeLock.lock();

        profileMappings = new ArrayList<>();
        jwtMappings = new ArrayList<>();

        try {
            String xml = configManager.getConfigItem("dynamicAuthMapping");
            if (StringUtils.isNotBlank(xml)) {
                DynamicMapping dynConf = xmlMapper.readValue(xml, DynamicMapping.class);
                if (dynConf != null) {
                    if (dynConf.mapping != null) {

                        final Map<Boolean, List<DynamicMappingElement>> partitioned =
                                dynConf.mapping.stream()
                                        .filter(this::isValid)
                                        .filter(m -> m.enabled)
                                        .collect(Collectors.partitioningBy(
                                                item -> item.kind.isJwtMapping()
                                        ));
                        profileMappings = List.copyOf(partitioned.get(false));
                        jwtMappings = List.copyOf(partitioned.get(true));
                        log.debug("{} dynamic auth mapping found, {} profileMappings",
                                dynConf.mapping.size(), profileMappings.size());
                    }
                    ignore = Optional.ofNullable(dynConf.exclusions)
                            .orElse(List.of());
                    roles = Optional.ofNullable(dynConf.roles)
                            .orElseGet(List::of);
                    groups = Optional.ofNullable(dynConf.groups)
                            .orElse(List.of());
                    enabled = Optional.ofNullable(dynConf.enabled)
                            .orElse(false);
                    persist = Optional.ofNullable(dynConf.persist)
                            .orElse(PersistKind.FULL);
                }
            }
            if (profileMappings != null) {
                profileMappings.forEach(m -> log.debug("profile mapping active: {}", m.toString()));
            }
            if (jwtMappings != null) {
                jwtMappings.forEach(m -> log.debug("jwt mapping active: {}", m.toString()));
            }
        } catch (Exception e) {
            // defaults
            enabled = false;
            roles = new ArrayList<>();
            groups = new ArrayList<>();
            log.error("Error initializing KeycloakAuthorizationManager", e);
            throw e;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * This is invoked after a configured time to update the configuration
     */
    public void refreshConfiguration() {
        try {
            init();
        } catch (Exception e) {
            log.error("Error refreshing dynamic mapping configuration");
        }
    }

    /**
     * Check whether the dynamic configuration element provided is valid
     * @param elem the single dynamic mapping element to validate
     * @return true if the element is valid, false otherwise
     */
    private boolean isValid(DynamicMappingElement elem) {
        if (elem.kind == null) {
            log.error("invalid dynamic mapping element, 'kind' is blank");
            return false;
        }
        if (StringUtils.isBlank(elem.attribute) && !elem.kind.isJwtMapping()) {
            log.error("invalid dynamic mapping element, 'attribute' is blank for kind {}", elem.kind);
            return false;
        }
        if (StringUtils.isBlank(elem.path) && elem.kind.isJwtMapping()) {
            log.error("invalid dynamic mapping element, 'path' is blank for {} kind", elem.kind);
            return false;
        }
        if (StringUtils.isBlank(elem.separator) && (elem.kind == ROLEGROUP || elem.kind == ROLEGROUPCLAIM)) {
            log.error("invalid dynamic mapping element, 'separator' is blank for {} kind", elem.kind);
            return false;
        }
        return true;
    }

    public void processNewUser(final UserDetails user, final String token, final boolean decode) {
        processNewUser(user);
        if (!enabled) return;
        readLock.lock();
        try {
            // Authorizations coming from dynamic mapping (that is, external sources)
            final List<Authorization> dynamicAuthorizations = new ArrayList<>();
            final Long iat;
            if (StringUtils.isNotBlank(token)) {
                iat = OidcMappingHelper.extractIssuedAtFromJwt(token, decode, user.getUsername());
            } else {
                iat = 0L;
            }

            if (iat == null) {
                log.debug("no need to sync user {}", user.getUsername());
                return;
            }

            if (iat > 0 && authorizationManager.checkExternalAuthSync(user.getUsername(), iat)) {
                log.debug("user {} already synced (iat: {})", user.getUsername(), iat);
                return;
            }

            // process path role claims, if any...
            if (StringUtils.isNotBlank(token) && !jwtMappings.isEmpty()) {
                for (DynamicMappingElement cur: jwtMappings) {
                    dynamicAuthorizations.addAll(processJwtClaimAttributes(user, token, decode, cur));
                }
            }
            // ...then process attributes coming from the user profile, if needed
            if (user instanceof KeycloakUser
                    && profileMappings != null
                    && !profileMappings.isEmpty()) {
                dynamicAuthorizations.addAll(processProfileAttributes((KeycloakUser) user));
            }
            syncAuthorizations(user, dynamicAuthorizations, iat);
        } catch (EntException e) {
            throw new RuntimeException(e);
        } finally {
            readLock.unlock();
        }
    }

    private void syncAuthorizations(final UserDetails user, final List<Authorization> dynamicAuthorizations, final Long iat) throws EntException {
        //If the dynamic authorization is not already assigned to the user, then it must be added
        List<Authorization> toAdd = dynamicAuthorizations
                .stream()
                .filter(a -> {
                    final String groupName = a.getGroup() != null ? a.getGroup().getName() : null;
                    final String roleName = a.getRole() != null ? a.getRole().getName() : null;

                    assert user instanceof KeycloakUser;
                    return !isAlreadyAssigned((KeycloakUser) user, groupName, roleName);
                })
                .collect(Collectors.toList());
        // list of the _managed_ authorizations currently assigned to the user
        List<Authorization> existingAuths = Optional.ofNullable(user.getAuthorizations())
                .orElse(List.of())
                .stream()
                .filter(a -> (a.getGroup() != null && groups.contains(a.getGroup().getName())
                        || (a.getRole() != null && roles.contains(a.getRole().getName())))
                )
                .collect(Collectors.toList());
        // If the existing authorization is not included in the dynamic authorizations, it must be removed
        List<Authorization> toDelete = existingAuths
                .stream()
                .filter(a -> {
                    return dynamicAuthorizations.stream()
                            .noneMatch(d -> d.equals(a));
                })
                .collect(Collectors.toList());
//        sillyDebug(user, dynamicAuthorizations, existingAuths, toAdd, toDelete);
        // update authorizations
        if (persist == PersistKind.FULL) {
            this.authorizationManager.externalAuthSync(user.getUsername(), iat, toAdd, toDelete);
            // update current auths
            syncUserAuthorizations(user, toDelete);
        } else if (persist == PersistKind.AUTH || persist == PersistKind.NONE) {
            for (Authorization authorization : toAdd) {
                user.addAuthorization(authorization);
            }
            syncUserAuthorizations(user, toDelete);
        }
    }

    private void syncUserAuthorizations(UserDetails user, List<Authorization> toDelete) throws EntException {
        final List<Integer> index = new ArrayList<>();
        final List<String> rolesToDelete = new ArrayList<>();
        final List<String> groupsToDelete = new ArrayList<>();

        for (Authorization authorization: toDelete) {
            if (authorization.getRole() != null) {
                rolesToDelete.add(authorization.getRole().getName());
            }
            if (authorization.getGroup() != null) {
                groupsToDelete.add(authorization.getGroup().getName());
            }
            index.add(indexOfAuthorization(user, authorization));
        }
        // sync authorizations
        index.sort(Comparator.reverseOrder());
        if (!index.isEmpty()) {
            index.stream()
                    .filter(idx -> idx >= 0)
                    .forEach(idx -> user.getAuthorizations().remove(idx));
        }
    }

    public static int indexOfAuthorization(UserDetails user, Authorization target) {
        if (user == null || target == null) {
            return -1;
        }

        final List<Authorization> authorizations = user.getAuthorizations();

        if (authorizations == null || authorizations.isEmpty()) {
            return -1;
        }

        for (int i = 0; i < authorizations.size(); i++) {
            Authorization current = authorizations.get(i);

            if (current.equals(target)) {
                return i;
            }
        }
        // oops
        return -1;
    }

/*
    private static void sillyDebug(UserDetails user, List<Authorization> dynamicAuthorizations,
            List<Authorization> existingAuths, List<Authorization> toAdd, List<Authorization> toDelete) {
        System.out.println("-------------------\n");
        dynamicAuthorizations.forEach(n-> {
            final String groupName = n.getGroup() != null ? n.getGroup().getName() : null;
            final String roleName = n.getRole() != null ? n.getRole().getName() : null;

            System.out.println("INCOMING " + user.getUsername() + " role " + roleName +  " group " + groupName);
        });
        existingAuths.forEach(n-> {
            final String groupName = n.getGroup() != null ? n.getGroup().getName() : null;
            final String roleName = n.getRole() != null ? n.getRole().getName() : null;

            System.out.println("USER " + user.getUsername() + " role " + roleName +  " group " + groupName);
        });
        toAdd.forEach(n-> {
            final String groupName = n.getGroup() != null ? n.getGroup().getName() : null;
            final String roleName = n.getRole() != null ? n.getRole().getName() : null;

            System.out.println("ADD " + user.getUsername() + " role " + roleName +  " group " + groupName);
        });
        toDelete.forEach(d-> {
            final String groupName = d.getGroup() != null ? d.getGroup().getName() : null;
            final String roleName = d.getRole() != null ? d.getRole().getName() : null;

            System.out.println("DELETE " + user.getUsername() + " role " + roleName +  " group " + groupName);
        });
    }
*/

    /**
     * Analyze the JWT looking for known mappings to translate into Entando roles
     * @param user logged in user
     * @param token access token
     * @param decode is true the access token is decoded from the base64 form
     * @param claimMapper the mapping configuration
     * @return the list of authorizations extracted from the JWT
     */
    private List<Authorization> processJwtClaimAttributes(final UserDetails user, final String token, final boolean decode, final DynamicMappingElement claimMapper) {
        final List<String> authorizations = OidcMappingHelper.extractAuthorizationsFromJwt(token, decode, claimMapper, user.getUsername());
        List<Authorization> jwtAuthorizations = new ArrayList<>();

        if (user instanceof KeycloakUser && !authorizations.isEmpty()) {
            KeycloakUser kcUser = (KeycloakUser) user;
            if (claimMapper.kind == DynamicMappingKind.ROLECLAIM) {
                jwtAuthorizations.addAll(finalizeRoleAssociation(kcUser, claimMapper, authorizations));
            } else if (claimMapper.kind == DynamicMappingKind.GROUPCLAIM) {
                jwtAuthorizations.addAll(finalizeGroupAssociation(kcUser, claimMapper, authorizations));
            } else {
                jwtAuthorizations.addAll(finalizeGroupRoleAssociation(kcUser, claimMapper, authorizations));
            }
        }
        return jwtAuthorizations;
    }

    private List<Authorization> finalizeGroupRoleAssociation(KeycloakUser user, DynamicMappingElement elem, List<String> authorizations) {
        List<Authorization> result = new ArrayList<>();
        if (authorizations == null) return result;

        for (String candidate : authorizations) {
            try {
                if (StringUtils.isBlank(candidate)) {
                    continue;
                }

                final String sep = StringUtils.isNotBlank(elem.separator) ? elem.separator : DEFAULT_SEPARATOR;
                final String[] tokens = candidate.split(sep);

                if (tokens.length < 2) {
                    // treat as a role
                    result.addAll(finalizeRoleAssociation(user, elem, List.of(candidate.trim())));
                    continue;
                }

                final String roleName = tokens[0].trim();
                final String groupName = tokens[1].trim();

                if (StringUtils.isBlank(roleName)) {
                    log.warn("Invalid role name extracted from candidate '{}' for user {}", candidate, user.getUsername());
                    continue;
                }

                Authorization auth = finalizeAssociation(user, elem, roleName, groupName);
                if (auth != null) {
                    result.add(auth);
                }
            } catch (Exception e) {
                log.error("Error processing dynamic group-role '{}' for user {}", candidate, user.getUsername(), e);
            }
        }
        return result;
    }

    private void processNewUser(final UserDetails user) {
        if (StringUtils.isNotEmpty(configuration.getDefaultAuthorizations())) {
            // process group and role coming from the configuration
            final Set<String> defaultAuthorizations = Sets.newHashSet(configuration.getDefaultAuthorizations().split(","));
            final Set<String> userAuthorizations = user.getAuthorizations().stream().map(authorization -> {
                final String group = ofNullable(authorization.getGroup()).map(Group::getName).orElse("");
                final String role = ofNullable(authorization.getRole()).map(Role::getName).orElse("");
                return StringUtils.isEmpty(role) ? group : group + ":" + role;
            }).collect(Collectors.toSet());

            defaultAuthorizations.stream()
                    .filter(defaultGroup -> !userAuthorizations.contains(defaultGroup))
                    .forEach(authorization -> this.assignGroupToUser(authorization, user));
        }
    }

    private void assignGroupToUser(final String authorization, final UserDetails user) {
        final String[] split = authorization.split(":");
        String groupName = split.length > 0 ? split[GROUP_POSITION] : "";
        String roleName = split.length > 1 ? split[ROLE_POSITION] : "";
        try {
            final Group group = ofNullable(groupName).filter(StringUtils::isNotEmpty)
                    .map(this::findOrCreateGroup).orElse(null);
            final Role role = ofNullable(roleName).filter(StringUtils::isNotEmpty)
                    .map(this::findOrCreateRole).orElse(null);
            groupName = ofNullable(group).map(Group::getName).orElse(null); // null or ""?
            roleName = ofNullable(role).map(Role::getName).orElse(null); // null or ""?
            authorizationManager.addUserAuthorization(user.getUsername(), groupName, roleName);
        } catch (EntException e) {
            throw new RuntimeException(e);
        }
    }

    private Group findOrCreateGroup(String groupName) {
        Group group = groupManager.getGroup(groupName);

        if (group != null) {
            return group;
        }

        Group newGroup = new Group();
        newGroup.setName(groupName);
        newGroup.setDescription(groupName);

        try {
            groupManager.addGroup(newGroup);
            return newGroup;
        } catch (Exception e) {
            log.debug("Error persisting group {} ( It might have been already added by another process).",
                    groupName);
            return groupManager.getGroup(groupName);
        }
    }

    private Role findOrCreateRole(final String roleName) {
        Role newRole = roleManager.getRole(roleName);

        if (newRole != null) {
            return newRole;
        }

        newRole = new Role();
        newRole.setName(roleName);
        newRole.setDescription(roleName);
        try {
            roleManager.addRole(newRole);
            return newRole;
        } catch (Exception e) {
            log.debug("Error persisting role {} (It might have been already added by another process).",
                    roleName);
            return roleManager.getRole(roleName);
        }
    }

    /**
     * Map dynamically, optionally persisting, authorization coming from the user profile in
     * keycloak
     * @param user the currently logged user
     * @return the list of authorizations extracted from the user profile
     */
    private List<Authorization> processProfileAttributes(final KeycloakUser user) {
        List<Authorization> result = new ArrayList<>();
        profileMappings.forEach(m -> {
            if (m.kind == ROLE) {
                result.addAll(doProcessRole(user, m));
            }
            if (m.kind == GROUP) {
                result.addAll(doProcessGroup(user, m));
            }
            if (m.kind == ROLEGROUP) {
                result.addAll(doProcessRoleGroup(user, m));
            }
        });
        return result;
    }

    private List<Authorization> doProcessRoleGroup(KeycloakUser user, DynamicMappingElement elem) {
        List<Authorization> result = new ArrayList<>();
        final String separator = StringUtils.isBlank(elem.separator) ?
                DEFAULT_SEPARATOR : elem.separator;

        try {
            final List<String> authorizations = OidcMappingHelper.extractAuthorizationsFromProfile(user, elem);

            if (authorizations == null) {
                return result;
            }
            for (String groupRoleToken : authorizations) {
                Authorization auth = parseAuthForRoleGroup(user, elem, groupRoleToken, separator);
                if (auth != null) {
                    result.add(auth);
                }
            }
        } catch (Exception e) {
            log.error("error processing dynamic GRUOPROLE association", e);
        }
        return result;
    }

    private Authorization parseAuthForRoleGroup(KeycloakUser user, DynamicMappingElement elem, String groupRoleToken, String separator) {
        final String[] tokens = groupRoleToken.split(separator);

        if (tokens.length != 2
                || StringUtils.isBlank(tokens[0])
                || StringUtils.isBlank(tokens[1])) {
            log.error("invalid dynamic config configuration detected");
            return null;
        }

        final String groupName = tokens[1];
        final String roleName = tokens[0];

        return finalizeAssociation(user, elem, roleName, groupName, false);
    }

    private Group createTransientGroup(String groupName) {
        Group group = new Group();
        group.setName(groupName);
        group.setDescription("sys:" + groupName);
        return group;
    }

    /**
     * Process the dynamic Role authorization for the given user
     * @param user the currently logging-in user
     * @param elem a single dynamic configuration
     * @return the list of authorizations extracted from the user profile
     */
    private List<Authorization> doProcessRole(KeycloakUser user, DynamicMappingElement elem) {
        final List<String> authorizations = OidcMappingHelper.extractAuthorizationsFromProfile(user, elem);
        return finalizeRoleAssociation(user, elem, authorizations);
    }

    private List<Authorization> finalizeRoleAssociation(KeycloakUser user, DynamicMappingElement elem, List<String> authorizations) {
        List<Authorization> result = new ArrayList<>();
        if (authorizations == null) return result;

        for (String roleName : authorizations) {
            try {
                Authorization auth = finalizeAssociation(user, elem, roleName, null);
                if (auth != null) {
                    result.add(auth);
                }
            } catch (Exception e) {
                log.error("Error processing dynamic role '{}' for user {}", roleName, user.getUsername(), e);
            }
        }
        return result;
    }

    private Authorization finalizeAssociation(KeycloakUser user, DynamicMappingElement elem, String roleName, String groupName) {
        return finalizeAssociation(user, elem, roleName, groupName, true);
    }

    private boolean isIgnored(String name) {
        if (ignore == null || StringUtils.isBlank(name)) {
            return false;
        }
        return ignore.contains(name.trim());
    }

    private Authorization finalizeAssociation(KeycloakUser user, DynamicMappingElement elem, String roleName, String groupName,
            boolean createRoleIfMissing) {
        // is it excluded?
        if (isIgnored(roleName) || isIgnored(groupName)) {
            log.info("Role {} or Group {} is in the exclusions list. Skipping assignment for user {}", roleName, groupName, user.getUsername());
            return null;
        }
        // are they managed?
        if (StringUtils.isNotBlank(roleName) && !roles.contains(roleName)) {
            log.info("Role {} is not managed. Skipping assignment for user {}", roleName, user.getUsername());
            return null;
        }
        if (StringUtils.isNotBlank(groupName) && !groups.contains(groupName)) {
            log.info("Group {} is not managed. Skipping assignment for user {}", groupName, user.getUsername());
            return null;
        }
        // further optimization
//        if (!isAlreadyAssigned(user, groupName, roleName)) {
            return createAuthorization(roleName, groupName, createRoleIfMissing);
//        } else {
//            return null;
//        }
    }

    private Authorization createAuthorization(String roleName, String groupName, boolean createRoleIfMissing) {
        if (shouldPersistAuthorization()) {
            return createPersistedAuthorization(roleName, groupName);
        }
        return createTransientAuthorization(roleName, groupName, createRoleIfMissing);
    }

    private boolean shouldPersistAuthorization() {
        return this.persist == PersistKind.AUTH || this.persist == PersistKind.FULL;
    }

    private Authorization createPersistedAuthorization(String roleName, String groupName) {
        final Group group = StringUtils.isNotBlank(groupName) ? findOrCreateGroup(groupName) : null;
        final Role role = StringUtils.isNotBlank(roleName) ? findOrCreateRole(roleName) : null;
        return new Authorization(group, role);
    }

    private Authorization createTransientAuthorization(String roleName, String groupName, boolean createRoleIfMissing) {
        final Group group = StringUtils.isNotBlank(groupName) ? createTransientGroup(groupName) : null;
        final Role role = resolveTransientRole(roleName, createRoleIfMissing);
        return new Authorization(group, role);
    }

    private Role resolveTransientRole(String roleName, boolean createRoleIfMissing) {
        if (StringUtils.isBlank(roleName)) {
            return null;
        }
        return createRoleIfMissing ? createTransientRole(roleName) : roleManager.getRole(roleName);
    }

    private boolean isAlreadyAssigned(final KeycloakUser user, final String groupName, final String roleName) {
        return user.getAuthorizations().stream()
                .anyMatch(a -> {
                    final String existingRoleName = (a.getRole() != null) ? a.getRole().getName() : null;
                    final String existingGroupName = (a.getGroup() != null) ? a.getGroup().getName() : null;

                    return Objects.equals(existingRoleName, roleName)
                            && Objects.equals(existingGroupName, groupName);
                });
    }

    private @NonNull Role createTransientRole(String roleName) {
        Role role = roleManager.getRole(roleName);
        if (role == null) {
            role = new Role();
            role.setName(roleName);
            role.setDescription(roleName);
        }
        return role;
    }

    /**
     * Process the dynamic Group authorization for the given user
     * @param user the currently logging-in user
     * @param elem a single dynamic configuration
     * @return the list of authorizations extracted from the user profile
     */
    private List<Authorization> doProcessGroup(KeycloakUser user, DynamicMappingElement elem) {
        final List<String> authorizations = OidcMappingHelper.extractAuthorizationsFromProfile(user, elem);
        if (authorizations == null) {
            return new ArrayList<>();
        }
        return finalizeGroupAssociation(user, elem, authorizations);
    }

    private List<Authorization> finalizeGroupAssociation(KeycloakUser user, DynamicMappingElement elem, List<String> authorizations) {
        List<Authorization> result = new ArrayList<>();
        if (authorizations == null) return result;

        for (String groupName : authorizations) {
            try {
                Authorization auth = finalizeAssociation(user, elem, null, groupName);
                if (auth != null) {
                    result.add(auth);
                }
            } catch (Exception e) {
                log.error("Error processing dynamic group '{}' for user {}", groupName, user.getUsername(), e);
            }
        }
        return result;
    }

}
