package org.entando.entando.keycloak.services;

import static com.agiletec.aps.system.SystemConstants.ADMIN_USER_NAME;
import static java.util.Optional.ofNullable;
import static org.entando.entando.keycloak.services.mapping.DynamicMappingKind.GROUP;
import static org.entando.entando.keycloak.services.mapping.DynamicMappingKind.ROLE;
import static org.entando.entando.keycloak.services.mapping.DynamicMappingKind.ROLEGROUP;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.tenants.RefreshableBeanTenantAware;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.keycloak.services.mapping.DynamicMapping;
import org.entando.entando.keycloak.services.mapping.DynamicMappingElement;
import org.entando.entando.keycloak.services.mapping.DynamicMappingKind;
import org.entando.entando.keycloak.services.mapping.PersistKind;
import org.entando.entando.keycloak.services.oidc.OidcMappingHelper;
import org.entando.entando.keycloak.services.oidc.model.KeycloakUser;
import org.springframework.beans.factory.annotation.Autowired;


public class KeycloakAuthorizationManager extends AbstractService implements RefreshableBeanTenantAware {

    private static final EntLogger log = EntLogFactory.getSanitizedLogger(KeycloakAuthorizationManager.class);

    private static final String DEFAULT_SEPARATOR = "_SEP_";

    // This is a fallback for the non-multitenant instance
    private transient final KeycloakConfiguration KeycloackConfiguration;

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

    private int cleanBatchSize;


    @Autowired
    public KeycloakAuthorizationManager(final KeycloakConfiguration configuration,
                                        final AuthorizationManager authorizationManager,
                                        final GroupManager groupManager,
            final RoleManager roleManager,
            final BaseConfigManager configManager) {
        this.KeycloackConfiguration = configuration;
        this.authorizationManager = authorizationManager;
        this.groupManager = groupManager;
        this.roleManager = roleManager;
        this.configManager = configManager;
    }

    /**
     * Immutable list of mapping elements that are currently active. The configuration is constantly updated
     * either by reloading the global configuration or after a certain amount of time (by default,
     * one minute)
     */
    private final transient Map<String, KeycloakImportConfig> config = new ConcurrentHashMap<>();

    @Override
    public void init() throws Exception {
       initTenantAware();
    }

    @Override
    public void initTenantAware() throws Exception {
        writeLock.lock();

        List<DynamicMappingElement> profileMappings = new ArrayList<>();
        List<DynamicMappingElement> jwtMappings = new ArrayList<>();
        List<String> ignore = new ArrayList<>();
        List<String> roles = new ArrayList<>();
        List<String> groups = new ArrayList<>();
        List<String> excludeUsers = new ArrayList<>();
        Boolean enabled = false;
        PersistKind persist = PersistKind.FULL;

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
                        log.debug("{} dynamic auth mapping found, {} getConfig().profileMappings",
                                jwtMappings.size(), profileMappings.size());
                    }
                    ignore = ofNullable(dynConf.exclusions)
                            .orElse(List.of());
                    roles = ofNullable(dynConf.roles)
                            .orElseGet(List::of);
                    groups = ofNullable(dynConf.groups)
                            .orElse(List.of());
                    excludeUsers = ofNullable(dynConf.excludeUsers)
                            .orElse(List.of());
                    enabled = ofNullable(dynConf.enabled)
                            .orElse(false);
                    persist = ofNullable(dynConf.persist)
                            .orElse(PersistKind.FULL);
                }
            }
            if (!profileMappings.isEmpty()) {
                profileMappings.forEach(m -> log.debug("profile mapping active: {}", m.toString()));
            }
            if (!jwtMappings.isEmpty()) {
                jwtMappings.forEach(m -> log.debug("jwt mapping active: {}", m.toString()));
            }
            // finally
            KeycloakImportConfig cfg = new KeycloakImportConfig(profileMappings, jwtMappings, ignore, roles, groups, excludeUsers, enabled, persist);

            setImportConfiguration(cfg);
        } catch (Exception e) {
            log.error("Error initializing KeycloakAuthorizationManager", e);
            KeycloakImportConfig cfg = new KeycloakImportConfig(List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), false, PersistKind.NONE);

            setImportConfiguration(cfg);
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

    public void cleanSyncData() {
        Instant tenMinutesAgo = null;
        try {
            if (!this.getImportConfiguration().getEnabled()) {
            return;
        }
            tenMinutesAgo = Instant.now().minusSeconds(600);

            log.info("Cleaning sync data older than {} with a batch size of {}", tenMinutesAgo, cleanBatchSize);
            authorizationManager.externalAuthSyncClean(tenMinutesAgo, cleanBatchSize);
            log.info("Cleaning completed successfully");
        } catch (Exception e) {
            log.error("Error cleaning sync data older than {}", tenMinutesAgo, e);
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
        // safety net! Admin is always exempted from group and roles assignment
        if (ADMIN_USER_NAME.equals(user.getUsername()) || isUserExcluded(user.getUsername())) {
            log.info("User {} is in the excludeUsers list. Skipping synchronization.", user.getUsername());
            return;
        }
        readLock.lock();
        try {
            if (!getImportConfiguration().getEnabled()) return;
            // Authorizations coming from dynamic mapping (that is, external sources)
            final List<Authorization> dynamicAuthorizations = new ArrayList<>();
            final Long iat;
            if (StringUtils.isNotBlank(token)) {
                iat = OidcMappingHelper.extractIssuedAtFromJwt(token, decode, user.getUsername());
            } else {
                iat = 0L;
            }

            if (iat == null) {
                log.debug("Could not extract IAT from JWT, skipping user '{}' synchronization", user.getUsername());
                return;
            }

            // abort if already synced
            if (iat > 0 && authorizationManager.externalAuthSyncCheck(user.getUsername(), iat)) {
                log.debug("user {} already synced (iat: {})", user.getUsername(), iat);
                return;
            }

            // process path role claims, if any...
            if (StringUtils.isNotBlank(token) && !getImportConfiguration().getJwtMappings().isEmpty()) {
                for (DynamicMappingElement cur: getImportConfiguration().getJwtMappings()) {
                    dynamicAuthorizations.addAll(processJwtClaimAttributes(user, token, decode, cur));
                }
            }
            // ...then process attributes coming from the user profile, if needed
            if (user instanceof KeycloakUser
                    && getImportConfiguration().getProfileMappings() != null
                    && !getImportConfiguration().getProfileMappings().isEmpty()) {
                dynamicAuthorizations.addAll(processProfileAttributes((KeycloakUser) user));
            }
            syncAuthorizations(user, dynamicAuthorizations, iat);
        } catch (EntException e) {
            throw new RuntimeException(e);
        } finally {
            readLock.unlock();
        }
    }

    /**
     * @brief Sync user authorizations with Keycloak
     *
     * @param user the currently authenticated user
     * @param dynamicAuthorizations all the dynamic authorizations to be synced
     * @param iat the issue time of the JWT token
     * @throws EntException in case of error
     */
    private void syncAuthorizations(final UserDetails user, final List<Authorization> dynamicAuthorizations, final Long iat) throws EntException {
        //If the dynamic authorization is not already assigned to the user, then it must be added
        List<Authorization> toAdd = dynamicAuthorizations
                .stream()
                .filter(a -> {
                    final String groupName = a.getGroup() != null ? a.getGroup().getName() : null;
                    final String roleName = a.getRole() != null ? a.getRole().getName() : null;

                    return !isAlreadyAssigned((KeycloakUser) user, groupName, roleName);
                })
                .toList();
        // list of the _managed_ authorizations currently assigned to the user
        List<Authorization> existingAuths = ofNullable(user.getAuthorizations())
                .orElse(List.of())
                .stream()
                .filter(a -> (a.getGroup() != null && getImportConfiguration().getGroups().contains(a.getGroup().getName())
                        || (a.getRole() != null && getImportConfiguration().getRoles().contains(a.getRole().getName())))
                )
                .toList();
        // If the existing authorization is not included in the dynamic authorizations, it must be removed
        List<Authorization> toDelete = existingAuths
                .stream()
                .filter(a -> dynamicAuthorizations.stream()
                        .noneMatch(d -> d.equals(a)))
                .toList();

        // update authorizations
        if (getImportConfiguration().getPersist() == PersistKind.FULL) {
            this.authorizationManager.externalAuthSync(user.getUsername(), iat, toAdd, toDelete);
        }
        user.getAuthorizations().removeAll(toDelete);
        user.addAuthorizations(toAdd);
    }

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
//                    result.addAll(finalizeRoleAssociation(user, elem, List.of(candidate.trim())));
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
        if (StringUtils.isNotEmpty(KeycloackConfiguration.getDefaultAuthorizations())) {
            // process group and role coming from the configuration
            final Set<String> defaultAuthorizations = Sets.newHashSet(KeycloackConfiguration.getDefaultAuthorizations().split(","));
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
            groupName = ofNullable(group).map(Group::getName).orElse(null); // null or "" ?
            roleName = ofNullable(role).map(Role::getName).orElse(null); // null or "" ?
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
        getImportConfiguration().getProfileMappings().forEach(m -> {
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
                Authorization auth = parseAuthForRoleGroup(user, groupRoleToken, separator);
                if (auth != null) {
                    result.add(auth);
                }
            }
        } catch (Exception e) {
            log.error("error processing dynamic ROLEGROUP association", e);
        }
        return result;
    }

    private Authorization parseAuthForRoleGroup(KeycloakUser user, String groupRoleToken, String separator) {
        final String[] tokens = groupRoleToken.split(separator);

        if (tokens.length != 2
                || StringUtils.isBlank(tokens[0])
                || StringUtils.isBlank(tokens[1])) {
            log.error("invalid dynamic config configuration detected");
            return null;
        }

        final String groupName = tokens[1];
        final String roleName = tokens[0];

        return finalizeAssociation(user, roleName, groupName, false);
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
        return finalizeAssociation(user, roleName, groupName, true);
    }

    private boolean isIgnored(String name) {
        if (getImportConfiguration().getIgnore() == null || StringUtils.isBlank(name)) {
            return false;
        }
        return getImportConfiguration().getIgnore().contains(name.trim());
    }

    private boolean isUserExcluded(String username) {
        if (getImportConfiguration().getExcludeUsers() == null || StringUtils.isBlank(username)) {
            return false;
        }
        return getImportConfiguration().getExcludeUsers().contains(username.trim());
    }

    private Authorization finalizeAssociation(KeycloakUser user, String roleName, String groupName,
            boolean createRoleIfMissing) {
        // is it excluded?
        if (isIgnored(roleName) || isIgnored(groupName)) {
            log.info("Role {} or Group {} is in the exclusions list. Skipping assignment for user {}", roleName, groupName, user.getUsername());
            return null;
        }
        // are they managed?
        if (StringUtils.isNotBlank(roleName) && !getImportConfiguration().getRoles().contains(roleName)) {
            log.info("Role {} is not managed. Skipping assignment for user {}", roleName, user.getUsername());
            return null;
        }
        if (StringUtils.isNotBlank(groupName) && !getImportConfiguration().getGroups().contains(groupName)) {
            log.info("Group {} is not managed. Skipping assignment for user {}", groupName, user.getUsername());
            return null;
        }
        return createAuthorization(roleName, groupName, createRoleIfMissing);
    }

    private Authorization createAuthorization(String roleName, String groupName, boolean createRoleIfMissing) {
        if (shouldPersistAuthorization()) {
            return createPersistedAuthorization(roleName, groupName);
        }
        return createTransientAuthorization(roleName, groupName, createRoleIfMissing);
    }

    private boolean shouldPersistAuthorization() {
        return this.getImportConfiguration().getPersist() == PersistKind.AUTH || this.getImportConfiguration().getPersist() == PersistKind.FULL;
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

    private Role createTransientRole(String roleName) {
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

    public void setCleanBatchSize(Integer cleanBatchSize) {
        this.cleanBatchSize = cleanBatchSize;
    }

    public KeycloakImportConfig getImportConfiguration() {
        final String tenantCode = this.getTenantCode();
        return config.get(tenantCode);
    }

    public void setImportConfiguration(KeycloakImportConfig config) {
        final String tenantCode = this.getTenantCode();
        this.config.put(tenantCode, config);
    }
}
