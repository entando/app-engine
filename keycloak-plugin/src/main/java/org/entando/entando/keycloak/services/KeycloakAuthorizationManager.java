package org.entando.entando.keycloak.services;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static org.entando.entando.keycloak.services.mapping.DynamicMappingKind.CLIENTROLE;
import static org.entando.entando.keycloak.services.mapping.DynamicMappingKind.GROUP;
import static org.entando.entando.keycloak.services.mapping.DynamicMappingKind.GROUPROLE;
import static org.entando.entando.keycloak.services.mapping.DynamicMappingKind.ROLE;

import com.agiletec.aps.system.common.AbstractService;
import com.agiletec.aps.system.services.authorization.Authorization;
import com.agiletec.aps.system.services.authorization.AuthorizationManager;
import com.agiletec.aps.system.services.baseconfig.BaseConfigManager;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.group.GroupManager;
import com.agiletec.aps.system.services.role.Role;
import com.agiletec.aps.system.services.role.RoleManager;
import com.agiletec.aps.system.services.user.UserDetails;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.keycloak.services.mapping.DynamicMapping;
import org.entando.entando.keycloak.services.mapping.DynamicMappingElement;
import org.entando.entando.keycloak.services.oidc.model.KeycloakUser;
import org.springframework.beans.factory.annotation.Autowired;

//@Service
public class KeycloakAuthorizationManager extends AbstractService {

    private static final EntLogger log = EntLogFactory.getSanitizedLogger(KeycloakAuthorizationManager.class);
    private static final String DEFAULT_SEPARATOR = "_";

    private final KeycloakConfiguration configuration;
    private final AuthorizationManager authorizationManager;
    private final GroupManager groupManager;
    private final RoleManager roleManager;
    private final BaseConfigManager configManager;

    private static final int GROUP_POSITION = 0;
    private static final int ROLE_POSITION = 1;

    private final ObjectMapper mapper = new ObjectMapper();
    private final XmlMapper xmlMapper = new XmlMapper();

    private final ReadWriteLock configUpdateLock = new ReentrantReadWriteLock();
    private final Lock readLock = configUpdateLock.readLock();
    private final Lock writeLock = configUpdateLock.writeLock();

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
    private List<DynamicMappingElement> activeMappings;

    @Override
    public void init() throws Exception {
        writeLock.lock();
        try {
            String xml = configManager.getConfigItem("dynamicAuthMapping");
            if (StringUtils.isNotBlank(xml)) {
                DynamicMapping dynConf = xmlMapper.readValue(xml, DynamicMapping.class);
                if (dynConf != null && dynConf.mapping != null) {

                    activeMappings = dynConf.mapping
                            .stream()
                            .filter(this::isValid)
                            .filter(d -> (d.enabled != null && d.enabled))
                            .collect(Collectors.toUnmodifiableList());
                    log.debug("{} dynamic auth mapping found, {} activeMappings",
                            dynConf.mapping.size(), activeMappings.size());
                }
            }
            if (activeMappings != null) {
                log.info("Dynamic configuration processed: {} active elements found", activeMappings.size());
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
        if (StringUtils.isBlank(elem.attribute) && elem.kind != CLIENTROLE) {
            log.error("invalid dynamic mapping element, 'attribute' is blank");
            return false;
        }
        if (StringUtils.isBlank(elem.client) && elem.kind == CLIENTROLE) {
            log.error("invalid dynamic mapping element, 'client' is blank for CLIENTROLE kind");
            return false;
        }
        if (StringUtils.isBlank(elem.separator) && elem.kind == GROUPROLE) {
            log.error("invalid dynamic mapping element, 'separator' is blank for GROUPROLE kind");
            return false;
        }
        return true;
    }

    public void processNewUser(final UserDetails user, final String token, boolean decode) {
        processNewUser(user);
        readLock.lock();
        try {
            final DynamicMappingElement tokenMapper = ofNullable(activeMappings)
                    .orElse(emptyList())
                    .stream()
                    .filter(m -> m.kind == CLIENTROLE)
                    .findFirst().orElse(null);
            // process client claim...
            if (StringUtils.isNotBlank(token) && tokenMapper != null) {
                final String payload = decode ? token.split("\\.")[1] : token;
                final String json = decode ? new String(Base64.getUrlDecoder().decode(payload)) : payload;

                try {
                    final JsonNode root = mapper.readTree(json);

                    JsonNode roleNode = root
                            .path("resource_access")
                            .path(tokenMapper.client)
                            .path("roles");

                    if (roleNode == null) {
                        return;
                    }

                    List<String> roles = StreamSupport.stream(roleNode.spliterator(), false)
                            .map(JsonNode::asText)
                            .collect(Collectors.toList());
                    if (user instanceof KeycloakUser) {
                        finalizeRoleAssociation((KeycloakUser) user, tokenMapper, roles);
                    }

                } catch (Exception e) {
                    log.error("error importing client role into Entando roles", e);
                }
            }
            // ...then process attributes coming from the user profile
            if (user instanceof KeycloakUser
                    && activeMappings != null
                    && !activeMappings.isEmpty()) {
                processProfileAttributes((KeycloakUser) user);
            }
        }  finally {
            readLock.unlock();
        }
    }

    public void processNewUser(final UserDetails user) {
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

    private synchronized Group findOrCreateGroup(final String groupName) {
        Group group = groupManager.getGroup(groupName);
        if (group == null) {
            group = new Group();
            group.setName(groupName);
            group.setDescription(groupName);
            try {
                groupManager.addGroup(group);
            } catch (EntException e) {
                log.error("Failed to create group: {}", groupName, e);
            }
        }
        return group;
    }

    private synchronized Role findOrCreateRole(final String roleName) {
        Role role = roleManager.getRole(roleName);
        if (role == null) {
            role = new Role();
            role.setName(roleName);
            role.setDescription(roleName);
            try {
                roleManager.addRole(role);
            } catch (EntException e) {
                log.error("Failed to create role: {}", roleName, e);
            }
        }
        return role;
    }

    /**
     * Map dynamically, optionally persisting, authorization coming from the user profile in
     * keycloak
     * @param user the currently logged user
     */
    private synchronized void processProfileAttributes(final KeycloakUser user) {
        activeMappings.forEach(m -> {
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
            final List<String> authorizations = processUserProfileAttribute(user, elem);

            if (authorizations == null) {
                return;
            }
            for (String groupRoleToken : authorizations) {
                final String[] tokens = groupRoleToken.split(separator);
                if (tokens.length != 2 || StringUtils.isBlank(tokens[0]) || StringUtils.isBlank(tokens[1])) {
                    log.error("invalid dynamic config configuration detected");
                    return;
                }
                final String groupName = tokens[0];
                final String roleName = tokens[1];

                Authorization authorization;
                Group group = null;
                Role role = null;

                if (elem.persist) {
                    if (StringUtils.isNotBlank(groupName)) {
                        group = findOrCreateGroup(groupName);
                    }

                    if (StringUtils.isNotBlank(roleName)) {
                        role = findOrCreateRole(roleName);
                    }
                    authorization = new Authorization(group, role);

                    persistAuthIfMissing(user, authorization);
                } else {
                    if (StringUtils.isNotBlank(groupName)) {
                        group = new Group();
                        group.setName(groupName);
                        group.setDescription("sys:" + groupName);
                    }
                    if (StringUtils.isNotBlank(roleName)) {
                        // make sure all the permissions are assigned to the current role
                        role = roleManager.getRole(roleName);
                    }
                    authorization = new Authorization(group, role);
                }
                user.addAuthorization(authorization);
            }
        } catch (Exception e) {
            log.error("error processing dynamic GRUOPROLE association", e);
        }
    }

    /**
     * Process the dynamic Role authorization for the given user
     * @param user the currently logging-in user
     * @param elem a single dynamic configuration
     */
    private void doProcessRole(KeycloakUser user, DynamicMappingElement elem) {
        final List<String> authorizations = processUserProfileAttribute(user, elem);
        finalizeRoleAssociation(user, elem, authorizations);
    }

    private void finalizeRoleAssociation(KeycloakUser user, DynamicMappingElement elem, List<String> authorizations) {
        if (authorizations == null) {
            return;
        }
        for (String kca: authorizations) {
            try {
                // skip if the group is already mapped
                if (user.getAuthorizations()
                        .stream()
                        .anyMatch(a -> a.getRole() != null
                                && a.getRole().getName().equals(kca))) {
                    log.debug("Role {} already assigned to user {}", kca, user.getUsername());
                    return;
                }
                final Authorization auth = elem.persist
                        ? createPersistedRoleAuthorization(user, kca)
                        : createTransientRoleAuthorization(kca);

                user.addAuthorization(auth);
                log.info("Successfully assigned role {} to user {}", kca, user.getUsername());
            } catch (Exception e) {
                log.error("Error processing dynamic role '{}' for user {}", kca , user.getUsername(), e);
            }
        }
    }

    private Authorization createPersistedRoleAuthorization(KeycloakUser user, String roleName) throws EntException {
        Role role = findOrCreateRole(roleName);
        Authorization auth = new Authorization(null, role);
        persistAuthIfMissing(user, auth);
        return auth;
    }

    private Authorization createTransientRoleAuthorization(String roleName) {
        Role role = roleManager.getRole(roleName);
        if (role == null) {
            role = new Role();
            role.setName(roleName);
        }
        return new Authorization(null, role);
    }

    /**
     * Process the dynamic Group authorization for the given user
     * @param user the currently logging-in user
     * @param elem a single dynamic configuration
     */
    private void doProcessGroup(KeycloakUser user, DynamicMappingElement elem) {
        final List<String> authorizations = processUserProfileAttribute(user, elem);
        if (authorizations == null) {
            return;
        }
        for (String kca: authorizations) {
            try {
                // skip if the role is already mapped
                if (user.getAuthorizations()
                        .stream()
                        .anyMatch(a -> a.getGroup() != null
                                && a.getGroup().getName().equals(kca))) {
                    log.debug("Group {} already assigned to user {}", kca, user.getUsername());
                    return;
                }
                final Authorization auth = elem.persist
                        ? createPersistedGroupAuthorization(user, kca)
                        : createTransientGroupAuthorization(kca);

                user.addAuthorization(auth);
                log.info("Successfully assigned group {} to user {}", kca, user.getUsername());
            } catch (Exception e) {
                log.error("Error processing dynamic group for user {}", user.getUsername(), e);
            }
        }
    }

    private Authorization createPersistedGroupAuthorization(KeycloakUser user, String groupName) throws EntException {
        final Group group = findOrCreateGroup(groupName);
        final Authorization auth = new Authorization(group, null);
        persistAuthIfMissing(user, auth);
        return auth;
    }

    private Authorization createTransientGroupAuthorization(String groupName) {
        Group group = new Group();
        group.setName(groupName);
        return new Authorization(group, null);
    }

    /**
     * Process dynamic configuration element for a user. If the attribute is missing, it skips processing.
     * @param user the Keycloak user
     * @param elem the dynamic mapping element
     * @return the list of processed attribute tokens or null if the attribute is missing
     */
    private static List<String> processUserProfileAttribute(KeycloakUser user, DynamicMappingElement elem) {
        if (user.getUserRepresentation() == null
                || user.getUserRepresentation().getAttributes() == null
                || !user.getUserRepresentation().getAttributes().containsKey(elem.attribute)) {
            log.info("skipping dynamic processing for user {}", user.getUsername());
            return null;
        }
        final Object kcProfileAttr = user.getUserRepresentation()
                .getAttributes()
                .get(elem.attribute);
        return handleKeycloakAttribute(kcProfileAttr);
    }

    /**
     * To avoid creating duplicate records, we are forced to check if the authorization already exists.
     * @param user the user being processed
     * @param auth the authorization to persist
     * @throws EntException in case of errors
     */
    private synchronized void persistAuthIfMissing(KeycloakUser user, Authorization auth) throws EntException {
        final List<Authorization> existing = authorizationManager.getUserAuthorizations(user.getUsername());
        if (existing.stream()
                .noneMatch(a -> (a.getGroup() != null && a.getRole() == null
                        && auth.getGroup() != null
                        && a.getGroup().getName().equals(auth.getGroup().getName()))
                        ||
                        (a.getRole() != null  && a.getGroup() == null
                                && auth.getRole() != null
                                && a.getRole().getName().equals(auth.getRole().getName()))

                        ||
                        (a.getRole() != null && a.getGroup() != null
                                && auth.getRole() != null
                                && auth.getGroup() != null
                                && a.getRole().getName().equals(auth.getRole().getName())
                                && a.getGroup().getName().equals(auth.getGroup().getName()))
                )
        ) {
            log.debug("dynamically persisting authorization for user '{}' : group {}, role {}", user.getUsername(),
                    auth.getGroup() != null ? auth.getGroup().getName() : "N/A",
                    auth.getRole() != null ? auth.getRole().getName() : "N/A");
            authorizationManager.addUserAuthorization(user.getUsername(), auth);
        }
    }

    /**
     * Process user attribute of the Keycloak profile. If it's a list, it will be flattened and split by whitespace.
     * If it's a string, it will be split by whitespace.
     * @param attribute the attribute data 
     * @return the list of the processed attribute tokens
     */
    protected static List<String> handleKeycloakAttribute(Object attribute) {
        if (attribute instanceof List) {
            List<Object> list = (List) attribute;
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .flatMap(s -> Arrays.stream(s.split("\\s+")))
                    .filter(token -> !token.isBlank())
                    .collect(Collectors.toUnmodifiableList());
        } else if (attribute instanceof String) {
            return Arrays.stream(((String) attribute).split("\\s+"))
                    .filter(token -> !token.isBlank())
                    .collect(Collectors.toUnmodifiableList());
        }
        return new ArrayList<>();
    }

}
