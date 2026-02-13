package org.entando.entando.keycloak.services;

import static java.util.Optional.ofNullable;
import static org.entando.entando.keycloak.services.mapping.DynamicMappingKind.GROUP;
import static org.entando.entando.keycloak.services.mapping.DynamicMappingKind.GROUPROLE;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import org.entando.entando.keycloak.services.oidc.OidcMappingService;
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
    private final OidcMappingService oidcMappingService;

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
            final BaseConfigManager configManager1,
            final OidcMappingService oidcMappingService) {
        this.configuration = configuration;
        this.authorizationManager = authorizationManager;
        this.groupManager = groupManager;
        this.roleManager = roleManager;
        this.configManager = configManager1;
        this.oidcMappingService = oidcMappingService;
    }

    /**
     * Immutable list of mapping elements that are currently active. The configuration is constantly updated
     * either by reloading the global configuration or after a certain amount of time (by default,
     * one minute)
     */
    private transient List<DynamicMappingElement> profileMappings;
    private transient List<DynamicMappingElement> jwtMappings;
    private transient List<String> ignore;

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
                                        .collect(Collectors.partitioningBy(
                                                item -> item.kind.isJwtMapping()
                                        ));
                        profileMappings = List.copyOf(partitioned.get(false));
                        jwtMappings = List.copyOf(partitioned.get(true));
                        log.debug("{} dynamic auth mapping found, {} profileMappings",
                                dynConf.mapping.size(), profileMappings.size());
                    }
                    ignore = dynConf.ignore;
                }
            }
            if (profileMappings != null) {
                profileMappings.forEach(m -> log.debug("mapping active: {}", m.toString()));
            }
        } catch (Exception e) {
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
        if (StringUtils.isBlank(elem.separator) && (elem.kind == GROUPROLE || elem.kind == ROLEGROUPCLAIM)) {
            log.error("invalid dynamic mapping element, 'separator' is blank for {} kind", elem.kind);
            return false;
        }
        return true;
    }

    public void processNewUser(final UserDetails user, final String token, final boolean decode) {
        processNewUser(user);
        readLock.lock();
        try {
            // process path role claims, if any...
            if (StringUtils.isNotBlank(token) && !jwtMappings.isEmpty()) {
                for (DynamicMappingElement cur: jwtMappings) {
                    processJwtClaimAttributes(user, token, decode, cur);
                }
            }
            // ...then process attributes coming from the user profile, if needed
            if (user instanceof KeycloakUser
                    && profileMappings != null
                    && !profileMappings.isEmpty()) {
                processProfileAttributes((KeycloakUser) user);
            }
        }  finally {
            readLock.unlock();
        }
    }

    /**
     * Analyze the JWT looking for known mappings to translate into Entando roles
     * @param user logged in user
     * @param token access token
     * @param decode is true the access token is decoded from the base64 form
     * @param claimMapper the mapping configuration
     */
    private void processJwtClaimAttributes(final UserDetails user, final String token, final boolean decode, final DynamicMappingElement claimMapper) {
        final List<String> authorizations = oidcMappingService.extractAuthorizationsFromJwt(token, decode, claimMapper, user.getUsername());

        if (user instanceof KeycloakUser && !authorizations.isEmpty()) {
            KeycloakUser kcUser = (KeycloakUser) user;
            if (claimMapper.kind == DynamicMappingKind.ROLECLAIM) {
                finalizeRoleAssociation(kcUser, claimMapper, authorizations);
            } else if (claimMapper.kind == DynamicMappingKind.GROUPCLAIM) {
                finalizeGroupAssociation(kcUser, claimMapper, authorizations);
            } else {
                finalizeGroupRoleAssociation(kcUser, claimMapper, authorizations);
            }
        }
    }

    private void finalizeGroupRoleAssociation(KeycloakUser user, DynamicMappingElement elem, List<String> authorizations) {
        if (authorizations == null) return;

        for (String candidate : authorizations) {
            try {
                if (StringUtils.isBlank(candidate)) {
                    continue;
                }

                final String sep = StringUtils.isNotBlank(elem.separator) ? elem.separator : DEFAULT_SEPARATOR;
                final String[] tokens = candidate.split(sep);

                if (tokens.length < 2) {
                    // treat as a role
                    finalizeRoleAssociation(user, elem, List.of(candidate.trim()));
                    continue;
                }

                final String roleName = tokens[0].trim();
                final String groupName = tokens[1].trim();

                if (StringUtils.isBlank(roleName)) {
                    log.warn("Invalid role name extracted from candidate '{}' for user {}", candidate, user.getUsername());
                    continue;
                }

                // skip if the couple has already been assigned
                if (isGroupRoleAlreadyAssigned(user, roleName, groupName)) {
                    log.debug("Role {} and group {} already assigned to user {}", roleName, groupName, user.getUsername());
                    continue;
                }

                Authorization auth;

                if (elem.persist == PersistKind.AUTH || elem.persist == PersistKind.FULL) {
                    final Group group = StringUtils.isNotBlank(groupName) ? findOrCreateGroup(groupName) : null;
                    final Role role = findOrCreateRole(roleName);

                    auth = new Authorization(group, role);
                } else {
                    final Group group = createTransientGroup(groupName);
                    final Role role = createTransientRole(roleName);

                    auth = new Authorization(group, role);
                }

                if (elem.persist == PersistKind.FULL) {
                    persistAuthIfMissing(user, auth);
                }

                user.addAuthorization(auth);
                log.info("Successfully assigned group-role {} to user {}", candidate, user.getUsername());
            } catch (Exception e) {
                log.error("Error processing dynamic group-role '{}' for user {}", candidate, user.getUsername(), e);
            }
        }
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
        } catch (EntException e) {
            log.debug("Error persisting group {} ( It might have been already added by another process).",
                    groupName);
            return groupManager.getGroup(groupName);
        }
    }

    private Role findOrCreateRole(final String roleName) {
        Role role = roleManager.getRole(roleName);

        if (role != null) {
            return role;
        }

        role = new Role();
        role.setName(roleName);
        role.setDescription(roleName);
        try {
            roleManager.addRole(role);
            return role;
        } catch (EntException e) {
            log.debug("Error persisting role {} (It might have been already added by another process).",
                    roleName);
            return roleManager.getRole(roleName);
        }
    }

    /**
     * Map dynamically, optionally persisting, authorization coming from the user profile in
     * keycloak
     * @param user the currently logged user
     */
    private void processProfileAttributes(final KeycloakUser user) {
        profileMappings.forEach(m -> {
            if (m.kind == ROLE) {
                doProcessRole(user, m);
            }
            if (m.kind == GROUP) {
                doProcessGroup(user, m);
            }
            if (m.kind == GROUPROLE) {
                doProcessGroupRole(user, m);
            }
        });
    }

    private void doProcessGroupRole(KeycloakUser user, DynamicMappingElement elem) {
        final String separator = StringUtils.isBlank(elem.separator) ?
                DEFAULT_SEPARATOR : elem.separator;

        try {
            final List<String> authorizations = oidcMappingService.extractAuthorizationsFromProfile(user, elem);

            if (authorizations == null) {
                return;
            }
            for (String groupRoleToken : authorizations) {
                parseAuthForGroupRole(user, elem, groupRoleToken, separator);
            }
        } catch (Exception e) {
            log.error("error processing dynamic GRUOPROLE association", e);
        }
    }

    private void parseAuthForGroupRole(KeycloakUser user, DynamicMappingElement elem, String groupRoleToken, String separator)
            throws EntException {
        final String[] tokens = groupRoleToken.split(separator);

        if (tokens.length != 2
                || StringUtils.isBlank(tokens[0])
                || StringUtils.isBlank(tokens[1])) {
            log.error("invalid dynamic config configuration detected");
            return;
        }

        final String groupName = tokens[0];
        final String roleName = tokens[1];
        final boolean shouldPersistAuth = (elem.persist == PersistKind.AUTH || elem.persist == PersistKind.FULL);

        Group group = shouldPersistAuth
                ? findOrCreateGroup(groupName)
                : createTransientGroup(groupName);

        Role role = shouldPersistAuth
                ? findOrCreateRole(roleName)
                : roleManager.getRole(roleName);

        Authorization authorization = new Authorization(group, role);

        if (elem.persist == PersistKind.FULL) {
            persistAuthIfMissing(user, authorization);
        }

        user.addAuthorization(authorization);
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
     */
    private void doProcessRole(KeycloakUser user, DynamicMappingElement elem) {
        final List<String> authorizations = oidcMappingService.extractAuthorizationsFromProfile(user, elem);
        finalizeRoleAssociation(user, elem, authorizations);
    }

    private void finalizeRoleAssociation(KeycloakUser user, DynamicMappingElement elem, List<String> authorizations) {
        if (authorizations == null) return;

        for (String roleName : authorizations) {
            try {
                if (isRoleAlreadyAssigned(user, roleName)) {
                    log.debug("Role {} already assigned to user {}", roleName, user.getUsername());
                    continue;
                }

                Authorization auth = (elem.persist == PersistKind.AUTH || elem.persist == PersistKind.FULL)
                        ? new Authorization(null, findOrCreateRole(roleName))
                        : createTransientRoleAuthorization(roleName);

                if (elem.persist == PersistKind.FULL) {
                    persistAuthIfMissing(user, auth);
                }

                user.addAuthorization(auth);
                log.info("Successfully assigned role {} to user {}", roleName, user.getUsername());

            } catch (Exception e) {
                log.error("Error processing dynamic role '{}' for user {}", roleName, user.getUsername(), e);
            }
        }
    }

    private boolean isRoleAlreadyAssigned(final KeycloakUser user, final String roleName) {
        return user.getAuthorizations().stream()
                .anyMatch(a -> a.getRole() != null && roleName.equals(a.getRole().getName()));
    }

    private boolean isGroupRoleAlreadyAssigned(final KeycloakUser user, final String roleName, final String groupName) {
        return user.getAuthorizations().stream()
                .anyMatch(a -> {
                    final String existingRoleName = (a.getRole() != null) ? a.getRole().getName() : null;
                    final String existingGroupName = (a.getGroup() != null) ? a.getGroup().getName() : null;

                    return Objects.equals(existingRoleName, roleName)
                            && Objects.equals(existingGroupName, groupName);
                });
    }

    private Authorization createTransientRoleAuthorization(String roleName) {
        final Role role = createTransientRole(roleName);
        return new Authorization(null, role);
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
     */
    private void doProcessGroup(KeycloakUser user, DynamicMappingElement elem) {
        final List<String> authorizations = oidcMappingService.extractAuthorizationsFromProfile(user, elem);
        if (authorizations == null) {
            return;
        }
        finalizeGroupAssociation(user, elem, authorizations);
    }

    private void finalizeGroupAssociation(KeycloakUser user, DynamicMappingElement elem, List<String> authorizations) {
        if (authorizations == null) return;

        for (String groupName : authorizations) {
            try {
                if (isGroupAlreadyAssigned(user, groupName)) {
                    log.debug("Group {} already assigned to user {}", groupName, user.getUsername());
                    continue;
                }

                Authorization auth = (elem.persist == PersistKind.AUTH || elem.persist == PersistKind.FULL)
                        ? new Authorization(findOrCreateGroup(groupName), null)
                        : createTransientGroupAuthorization(groupName);

                // optionally persist
                if (elem.persist == PersistKind.FULL) {
                    persistAuthIfMissing(user, auth);
                }

                user.addAuthorization(auth);
                log.info("Successfully assigned group {} to user {}", groupName, user.getUsername());

            } catch (Exception e) {
                log.error("Error processing dynamic group '{}' for user {}", groupName, user.getUsername(), e);
            }
        }
    }

    private boolean isGroupAlreadyAssigned(KeycloakUser user, String groupName) {
        return user.getAuthorizations().stream()
                .anyMatch(a -> a.getGroup() != null && groupName.equals(a.getGroup().getName()));
    }

    private Authorization createTransientGroupAuthorization(String groupName) {
        Group group = new Group();
        group.setName(groupName);
        group.setDescription(groupName);
        return new Authorization(group, null);
    }

    /**
     * Process dynamic configuration element for a user. If the attribute is missing, it skips processing.
     * @param user the Keycloak user
     * @param elem the dynamic mapping element
     * @return the list of processed attribute tokens or null if the attribute is missing
     */

    /**
     * To avoid creating duplicate records, we check if the authorization already exists. 
     * In a replicated environment or under high concurrency, there is still the possibility 
     * to attempt to create multiple, identical associations. This is handled by a database
     * unique constraint and a try-catch block.
     * @param user the user being processed
     * @param auth the authorization to persist
     * @throws EntException in case of errors
     */
    private void persistAuthIfMissing(KeycloakUser user, Authorization auth) throws EntException {
        final String username = user.getUsername();
        final List<Authorization> existing = authorizationManager.getUserAuthorizations(username);

        final String targetGroupName = (null != auth.getGroup()) ? auth.getGroup().getName() : null;
        final String targetRoleName = (null != auth.getRole()) ? auth.getRole().getName() : null;

        boolean alreadyExists = existing.stream().anyMatch(a -> {
            String existingGroupName = (null != a.getGroup()) ? a.getGroup().getName() : null;
            String existingRoleName = (null != a.getRole()) ? a.getRole().getName() : null;

            return Objects.equals(existingGroupName, targetGroupName) &&
                    Objects.equals(existingRoleName, targetRoleName);
        });

        if (!alreadyExists) {
            log.debug("Persisting new authorization for user '{}': group={}, role={}",
                    username, targetGroupName, targetRoleName);
            try {
                authorizationManager.addUserAuthorization(username, auth);
            } catch (EntException e) {
                log.debug("Error persisting authorization for user '{}': group={}, role={} "
                        + "(it might have been already added by another process).",
                        username, targetGroupName, targetRoleName);
            }
        } else {
            log.debug("Authorization already exists for user '{}': group={}, role={}. Skipping persistence.",
                    username, targetGroupName, targetRoleName);
        }
    }

    /**
     * Process user attribute of the Keycloak profile. If it's a list, it will be flattened and split by whitespace.
     * If it's a string, it will be split by whitespace.
     * @param attribute the attribute data 
     * @return the list of the processed attribute tokens
     */

}
