package org.entando.entando.keycloak.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agiletec.aps.system.services.authorization.Authorization;
import com.agiletec.aps.system.services.authorization.AuthorizationManager;
import com.agiletec.aps.system.services.baseconfig.BaseConfigManager;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.group.GroupManager;
import com.agiletec.aps.system.services.role.Role;
import com.agiletec.aps.system.services.role.RoleManager;
import com.fasterxml.jackson.core.JsonParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.keycloak.services.oidc.OidcMappingService;
import org.entando.entando.keycloak.services.oidc.model.KeycloakUser;
import org.entando.entando.keycloak.services.oidc.model.UserRepresentation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KeycloakAuthorizationManagerTest {

    @Mock private KeycloakUser userDetails;
    @Mock private KeycloakConfiguration configuration;
    @Mock private AuthorizationManager authorizationManager;
    @Mock private GroupManager groupManager;
    @Mock private RoleManager roleManager;
    @Mock private BaseConfigManager configManager;
    private OidcMappingService oidcMappingService = new OidcMappingService();

    private KeycloakAuthorizationManager manager;

    @BeforeEach
    public void setUp() {
        manager = new KeycloakAuthorizationManager(configuration, authorizationManager, groupManager, roleManager, configManager, oidcMappingService);
    }

    @Test
    void testGroupCreation() throws EntException {
        when(configuration.getDefaultAuthorizations()).thenReturn("readers");
        when(groupManager.getGroup(anyString())).thenReturn(null);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());

        manager.processNewUser(userDetails, null, false);

        final ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
        final ArgumentCaptor<String> auth_man_usernameCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> auth_man_groupCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> auth_man_roleCaptor = ArgumentCaptor.forClass(String.class);

        verify(roleManager, times(0)).getRole(anyString());
        verify(groupManager, times(1)).getGroup(eq("readers"));
        verify(groupManager, times(1)).addGroup(groupCaptor.capture());
        verify(authorizationManager).addUserAuthorization(auth_man_usernameCaptor.capture(),
                auth_man_groupCaptor.capture(),auth_man_roleCaptor.capture());

        assertThat(groupCaptor.getValue().getName()).isEqualTo("readers");

        assertThat(groupCaptor.getValue().getName()).isEqualTo("readers");
        assertThat(groupCaptor.getValue().getDescription()).isEqualTo("readers");

        assertThat(auth_man_groupCaptor.getValue()).isEqualTo("readers");
        assertThat(auth_man_roleCaptor.getValue()).isNull();
    }

    @Test
    void testGroupAndRoleCreation() throws EntException {
        when(configuration.getDefaultAuthorizations()).thenReturn("readers:read-all");
        when(groupManager.getGroup(anyString())).thenReturn(null);
        when(roleManager.getRole(anyString())).thenReturn(null);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());

        manager.processNewUser(userDetails, null, false);

        final ArgumentCaptor<Group> groupCaptor = ArgumentCaptor.forClass(Group.class);
        final ArgumentCaptor<Role> roleCaptor = ArgumentCaptor.forClass(Role.class);
        final ArgumentCaptor<String> auth_man_usernameCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> auth_man_groupCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> auth_man_roleCaptor =  ArgumentCaptor.forClass(String.class);

        verify(roleManager, times(1)).getRole(eq("read-all"));
        verify(roleManager, times(1)).addRole(roleCaptor.capture());
        verify(groupManager, times(1)).getGroup(eq("readers"));
        verify(groupManager, times(1)).addGroup(groupCaptor.capture());
        verify(authorizationManager).addUserAuthorization(auth_man_usernameCaptor.capture(),
                auth_man_groupCaptor.capture(), auth_man_roleCaptor.capture());

        assertThat(groupCaptor.getValue().getName()).isEqualTo("readers");
        assertThat(groupCaptor.getValue().getDescription()).isEqualTo("readers");

        assertThat(roleCaptor.getValue().getName()).isEqualTo("read-all");
        assertThat(roleCaptor.getValue().getDescription()).isEqualTo("read-all");

        assertThat(auth_man_groupCaptor.getValue()).isEqualTo("readers");
        assertThat(auth_man_roleCaptor.getValue()).isEqualTo("read-all");
    }

    @Test
    void testVerification() {
        final Authorization readers = authorization("readers", "read-all");
        final Authorization writers = authorization("writers", "write-all");

        when(configuration.getDefaultAuthorizations()).thenReturn("readers:read-all,writers:write-all");
        when(userDetails.getAuthorizations()).thenReturn(Arrays.asList(readers, writers));

        manager.processNewUser(userDetails, null, false);

        verify(roleManager, times(0)).getRole(anyString());
        verify(groupManager, times(0)).getGroup(anyString());
        verify(userDetails, times(0)).addAuthorization(any());
    }

    @Test
    void testDynamicConfigurationRoleOnLogin() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(roleManager.getRole(anyString())).thenReturn(null);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());
        when(userDetails.getUsername()).thenReturn("testuser");
        when(configManager.getConfigItem(anyString())).thenReturn(XML_ROLE_CONF);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setAttributes(Map.of("AD_ROLE", List.of("ruolo")));

        when(userDetails.getUserRepresentation()).thenReturn(userRepresentation);

        manager.init();

        final ArgumentCaptor<Authorization> authCaptor = ArgumentCaptor.forClass(Authorization.class);

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, times(1)).addUserAuthorization(eq("testuser"), authCaptor.capture());

        assertThat(authCaptor.getValue().getRole().getName()).isEqualTo("ruolo");
        assertThat(authCaptor.getValue().getGroup()).isNull();
    }

    @Test
    void testDynamicConfigurationRoleOnLoginFromJwt() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(roleManager.getRole(anyString())).thenReturn(null);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());
        when(userDetails.getUsername()).thenReturn("testuser");
        when(configManager.getConfigItem(anyString())).thenReturn(XML_ROLE_CLAIM);

        manager.init();

        final ArgumentCaptor<Authorization> authCaptor = ArgumentCaptor.forClass(Authorization.class);

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, times(4)).addUserAuthorization(eq("testuser"), authCaptor.capture());
        
        assertThat(authCaptor.getAllValues())
                .extracting(a -> a.getRole().getName())
                .containsOnly("generico","offline_access", "uma_authorization", "default-roles-entando");
        assertThat(authCaptor.getValue().getGroup()).isNull();
    }

    @Test
    void testDynamicConfigurationRoleOnLoginFromJwtNoPersist() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(roleManager.getRole(anyString())).thenReturn(null);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());
        when(userDetails.getUsername()).thenReturn("testuser");
        when(configManager.getConfigItem(anyString())).thenReturn(XML_ROLE_CLAIM_AUTH);

        manager.init();

        final ArgumentCaptor<Authorization> authCaptor = ArgumentCaptor.forClass(Authorization.class);

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, never()).addUserAuthorization(eq("testuser"), any());
        verify(userDetails, times(4)).addAuthorization(authCaptor.capture());

        assertThat(authCaptor.getAllValues())
                .extracting(a -> a.getRole().getName())
                .containsOnly("generico", "offline_access", "uma_authorization", "default-roles-entando");
        assertThat(authCaptor.getValue().getGroup()).isNull();
    }

    @Test
    void testDynamicConfigurationGroupOnLoginFromJwt() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());
        when(userDetails.getUsername()).thenReturn("testuser");
        when(configManager.getConfigItem(anyString())).thenReturn(XML_GROUP_CLAIM);

        manager.init();

        final ArgumentCaptor<Authorization> authCaptor = ArgumentCaptor.forClass(Authorization.class);

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, times(2)).addUserAuthorization(eq("testuser"), authCaptor.capture());

        assertThat(authCaptor.getAllValues())
                .extracting(a -> a.getGroup().getName())
                .containsExactlyInAnyOrder("Gruppo-Microsoft-Importato", "altro-gruppo");
        assertThat(authCaptor.getValue().getRole()).isNull();
    }

    @Test
    void testDynamicConfigurationGroupOnLoginFromJwtNoPersist() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());
        when(userDetails.getUsername()).thenReturn("testuser");
        when(configManager.getConfigItem(anyString())).thenReturn(XML_GROUP_CLAIM_AUTH);

        manager.init();

        final ArgumentCaptor<Authorization> authCaptor = ArgumentCaptor.forClass(Authorization.class);

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, never()).addUserAuthorization(eq("testuser"), any());
        verify(userDetails, times(2)).addAuthorization(authCaptor.capture());

        assertThat(authCaptor.getAllValues())
                .extracting(a -> a.getGroup().getName())
                .containsExactlyInAnyOrder("Gruppo-Microsoft-Importato", "altro-gruppo");
        assertThat(authCaptor.getValue().getRole()).isNull();
    }

    @Test
    void testDynamicConfigurationRoleOnLoginWithWrongJwt() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_ROLE_CLAIM);

        manager.init();

        final ArgumentCaptor<Authorization> authCaptor = ArgumentCaptor.forClass(Authorization.class);

        manager.processNewUser(userDetails, JWT_NO_ROLE, false);

        verify(authorizationManager, never()).addUserAuthorization(eq("testuser"), authCaptor.capture());
    }

    @Test
    void testDynamicConfigurationGroupOnLogin() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_GROUP_CONF);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setAttributes(Map.of("AD_GROUP", List.of("group")));

        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getUserRepresentation()).thenReturn(userRepresentation);

        manager.init();

        final ArgumentCaptor<Authorization> authCaptor = ArgumentCaptor.forClass(Authorization.class);

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, times(1)).addUserAuthorization(eq("testuser"), authCaptor.capture());

        assertThat(authCaptor.getValue().getGroup().getName()).isEqualTo("group");
        assertThat(authCaptor.getValue().getRole()).isNull();
    }

    @Test
    void testDynamicConfigurationGroupOnLoginNoPersist() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_GROUP_CONF_NO_PERSIST);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setAttributes(Map.of("AD_GROUP", List.of("group")));

        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getUserRepresentation()).thenReturn(userRepresentation);

        final ArgumentCaptor<Authorization> authCaptor = ArgumentCaptor.forClass(Authorization.class);

        manager.init();
        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, never()).addUserAuthorization(eq("testuser"), any());
        verify(userDetails, times(1)).addAuthorization(authCaptor.capture());

        assertThat(authCaptor.getValue().getGroup().getName()).isEqualTo("group");
        assertThat(authCaptor.getValue().getRole()).isNull();
    }

    @Test
    void testDynamicConfigurationGroupRoleOnLogin() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_GROUP_ROLE_CONF);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setAttributes(Map.of("AD_GROUPROLE", List.of("agroup_r_arole")));

        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getUserRepresentation()).thenReturn(userRepresentation);

        manager.init();

        final ArgumentCaptor<Authorization> authCaptor = ArgumentCaptor.forClass(Authorization.class);

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, times(1)).addUserAuthorization(eq("testuser"), authCaptor.capture());

        assertThat(authCaptor.getValue().getGroup().getName()).isEqualTo("agroup");
        assertThat(authCaptor.getValue().getRole().getName()).isEqualTo("arole");
    }

    @Test
    void testDynamicConfigurationGroupRoleOnLoginNoPersist() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_GROUP_ROLE_CONF_NO_PERSIST);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setAttributes(Map.of("AD_GROUPROLE", List.of("agroup_r_arole")));

        when(userDetails.getUserRepresentation()).thenReturn(userRepresentation);

        manager.init();

        final ArgumentCaptor<Authorization> authCaptor = ArgumentCaptor.forClass(Authorization.class);

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, never()).addUserAuthorization(eq("testuser"), any());
        verify(userDetails, times(1)).addAuthorization(authCaptor.capture());

        assertThat(authCaptor.getValue().getGroup().getName()).isEqualTo("agroup");
        // role doesn't exist, so it's not associated
        assertNull(authCaptor.getValue().getRole());
    }

    @Test
    void testDynamicConfigurationGroupRoleOnLoginAlreadyPresent() throws Exception {
        Group group = new Group();
        Role role = new Role();
        Authorization auth = new Authorization(group, role);

        group.setName("agroup");
        group.setDescription("agroup");
        role.setName("arole");
        role.setDescription("arole");

        when(authorizationManager.getUserAuthorizations(anyString())).thenReturn(List.of(auth));
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_GROUP_ROLE_CONF);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setAttributes(Map.of("AD_GROUPROLE", List.of("agroup_r_arole")));

        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getUserRepresentation()).thenReturn(userRepresentation);

        manager.init();

        final ArgumentCaptor<Authorization> authCaptor = ArgumentCaptor.forClass(Authorization.class);

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, never()).addUserAuthorization(eq("testuser"), authCaptor.capture());
    }

    @Test
    void testDynamicConfigurationNoGroupOnlyRoleOnLogin() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_GROUP_ROLE_CONF);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setAttributes(Map.of("AD_GROUPROLE", List.of("_r_arole")));

        when(userDetails.getUserRepresentation()).thenReturn(userRepresentation);

        manager.init();

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, never()).addUserAuthorization(anyString(), any());
    }

    @Test
    void testDynamicConfigurationOnlyGroupNoRoleOnLogin() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_GROUP_ROLE_CONF);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setAttributes(Map.of("AD_GROUPROLE", List.of("group_r_")));

        when(userDetails.getUserRepresentation()).thenReturn(userRepresentation);

        manager.init();

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, never()).addUserAuthorization(anyString(), any());
    }

    @Test
    void testDynamicConfigurationNoMapping() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_NO_MAPPING);

        manager.init();

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, never()).addUserAuthorization(anyString(), any());
    }

    @Test
    void testDynamicConfigurationMalformedMapping() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_MALFORMED_MAPPING);

        assertThrows(JsonParseException.class, () -> manager.init());

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, never()).addUserAuthorization(anyString(), any());
    }

    @Test
    void testDynamicConfigurationWrongMapping() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_WRONG_CONF);

        manager.init();
        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, never()).addUserAuthorization(anyString(), any());
    }

    @Test
    void testDynamicConfigurationGroupRoleOnLoginConflict() throws Exception {
        Group group = new Group();
        Role role = new Role();
        group.setName("agroup");
        group.setDescription("agroup");
        role.setName("arole");
        role.setDescription("arole");

        when(authorizationManager.getUserAuthorizations(anyString())).thenReturn(List.of());
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_GROUP_ROLE_CONF);
        when(groupManager.getGroup(anyString())).thenReturn(group);
        when(roleManager.getRole(anyString())).thenReturn(role);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setAttributes(Map.of("AD_GROUPROLE", List.of("agroup_r_arole")));

        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getUserRepresentation()).thenReturn(userRepresentation);

        // Simulate a conflict exception
        org.mockito.Mockito.doThrow(new EntException("Conflict"))
                .when(authorizationManager).addUserAuthorization(eq("testuser"), any());

        manager.init();

        // This should not throw an exception because it's caught in persistAuthIfMissing
        manager.processNewUser(userDetails, JWT, true);

        verify(authorizationManager, times(1)).addUserAuthorization(eq("testuser"), any());
    }

    @Test
    void testDynamicConfigurationRoleGroupOnLoginFromJwt() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(roleManager.getRole(anyString())).thenReturn(null);
        when(groupManager.getGroup(anyString())).thenReturn(null);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());
        when(userDetails.getUsername()).thenReturn("testuser");
        when(configManager.getConfigItem(anyString())).thenReturn(XML_ROLEGROUP_CLAIM);

        manager.init();

        final ArgumentCaptor<Authorization> authCaptor = ArgumentCaptor.forClass(Authorization.class);

        manager.processNewUser(userDetails, JWT_ROLEGROUP, false);

        verify(authorizationManager, times(2)).addUserAuthorization(eq("testuser"), authCaptor.capture());

        List<Authorization> capturedAuths = authCaptor.getAllValues();
        assertThat(capturedAuths).hasSize(2);

        assertThat(capturedAuths)
                .anySatisfy(auth -> {
                    assertThat(auth.getRole().getName()).isEqualTo("role1");
                    assertThat(auth.getGroup().getName()).isEqualTo("group1");
                })
                .anySatisfy(auth -> {
                    assertThat(auth.getRole().getName()).isEqualTo("role2");
                    assertThat(auth.getGroup().getName()).isEqualTo("group2");
                });
    }

    @Test
    void testDynamicConfigurationRoleGroupOnLoginFromJwtEdgeCases() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(roleManager.getRole(anyString())).thenReturn(null);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());
        when(userDetails.getUsername()).thenReturn("testuser");
        when(configManager.getConfigItem(anyString())).thenReturn(XML_ROLEGROUP_CLAIM);

        manager.init();

        final ArgumentCaptor<Authorization> authCaptor = ArgumentCaptor.forClass(Authorization.class);

        manager.processNewUser(userDetails, JWT_ROLEGROUP_EDGE, false);

        // NOTE!!! "group1" -> tokens.length < 2 -> treated as a ROLE "group1" with NO group
        // "_SEP_group2" -> tokens = ["", "group2"] -> roleName = "" -> isBlank -> skipped
        verify(authorizationManager, times(1)).addUserAuthorization(eq("testuser"), any(Authorization.class));

        verify(userDetails, times(1)).addAuthorization(authCaptor.capture());

        List<Authorization> capturedAuths = authCaptor.getAllValues();
        assertThat(capturedAuths).hasSize(1);

        assertThat(capturedAuths.get(0).getRole().getName()).isEqualTo("group1");
        assertThat(capturedAuths.get(0).getGroup()).isNull();
    }

    @Test
    void testDynamicConfigurationRoleGroupOnLoginFromJwtNoPersist() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());
        when(userDetails.getUsername()).thenReturn("testuser");
        when(configManager.getConfigItem(anyString())).thenReturn(XML_ROLEGROUP_CLAIM_AUTH);

        manager.init();

        final ArgumentCaptor<Authorization> authCaptor = ArgumentCaptor.forClass(Authorization.class);

        manager.processNewUser(userDetails, JWT_ROLEGROUP, false);

        verify(authorizationManager, never()).addUserAuthorization(eq("testuser"), any());
        verify(userDetails, times(2)).addAuthorization(authCaptor.capture());

        List<Authorization> capturedAuths = authCaptor.getAllValues();
        assertThat(capturedAuths).hasSize(2);

        assertThat(capturedAuths)
                .anySatisfy(auth -> {
                    assertThat(auth.getRole().getName()).isEqualTo("role1");
                    assertThat(auth.getGroup().getName()).isEqualTo("group1");
                })
                .anySatisfy(auth -> {
                    assertThat(auth.getRole().getName()).isEqualTo("role2");
                    assertThat(auth.getGroup().getName()).isEqualTo("group2");
                });
    }

    private Authorization authorization(final String groupName, final String roleName) {
        final Group group = new Group();
        group.setName(groupName);
        final Role role = new Role();
        role.setName(roleName);
        return new Authorization(group, role);
    }

    private static final String XML_ROLE_CONF = "<mappings>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <attribute>AD_ROLE</attribute>"
            + "  <kind>ROLE</kind>"
            + "  <persist>FULL</persist>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_GROUP</attribute>"
            + "  <kind>GROUP</kind>"
            + "  <persist>FULL</persist>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_GROUPROLE</attribute>"
            + "  <kind>GROUPROLE</kind>"
            + "  <separator>_r_</separator>"
            + "  <persist>FULL</persist>"
            + " </mapping>"
            + "</mappings>";

    private static final String XML_GROUP_CONF = "<mappings>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_ROLE</attribute>"
            + "  <kind>ROLE</kind>"
            + "  <persist>FULL</persist>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <attribute>AD_GROUP</attribute>"
            + "  <kind>GROUP</kind>"
            + "  <persist>FULL</persist>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_GROUPROLE</attribute>"
            + "  <kind>GROUPROLE</kind>"
            + "  <separator>_r_</separator>"
            + "  <persist>FULL</persist>"
            + " </mapping>"
            + "</mappings>";

    private static final String XML_GROUP_CONF_NO_PERSIST = "<mappings>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_ROLE</attribute>"
            + "  <kind>ROLE</kind>"
            + "  <persist>FULL</persist>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <attribute>AD_GROUP</attribute>"
            + "  <kind>GROUP</kind>"
            + "  <persist>NONE</persist>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_GROUPROLE</attribute>"
            + "  <kind>GROUPROLE</kind>"
            + "  <separator>_r_</separator>"
            + "  <persist>FULL</persist>"
            + " </mapping>"
            + "</mappings>";

    private static final String XML_GROUP_ROLE_CONF = "<mappings>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_ROLE</attribute>"
            + "  <kind>ROLE</kind>"
            + "  <persist>FULL</persist>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_GROUP</attribute>"
            + "  <kind>GROUP</kind>"
            + "  <persist>FULL</persist>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <attribute>AD_GROUPROLE</attribute>"
            + "  <kind>GROUPROLE</kind>"
            + "  <separator>_r_</separator>"
            + "  <persist>FULL</persist>"
            + " </mapping>"
            + "</mappings>";

    private static final String XML_GROUP_ROLE_CONF_NO_PERSIST = "<mappings>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_ROLE</attribute>"
            + "  <kind>ROLE</kind>"
            + "  <persist>FULL</persist>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_GROUP</attribute>"
            + "  <kind>GROUP</kind>"
            + "  <persist>FULL</persist>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <attribute>AD_GROUPROLE</attribute>"
            + "  <kind>GROUPROLE</kind>"
            + "  <separator>_r_</separator>"
            + "  <persist>NONE</persist>"
            + " </mapping>"
            + "</mappings>";

    private static final String XML_ROLE_CLAIM = "<mappings>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <path>realm_access.roles</path>"
            + "  <kind>ROLECLAIM</kind>"
            + "  <persist>FULL</persist>"
            + " </mapping>"
            + "</mappings>";

    private static final String XML_ROLE_CLAIM_AUTH = "<mappings>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <path>realm_access.roles</path>"
            + "  <kind>ROLECLAIM</kind>"
            + "  <persist>AUTH</persist>"
            + " </mapping>"
            + "</mappings>";

    private static final String XML_GROUP_CLAIM = "<mappings>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <path>groups</path>"
            + "  <kind>GROUPCLAIM</kind>"
            + "  <persist>FULL</persist>"
            + " </mapping>"
            + "</mappings>";

    private static final String XML_GROUP_CLAIM_AUTH = "<mappings>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <path>groups</path>"
            + "  <kind>GROUPCLAIM</kind>"
            + "  <persist>AUTH</persist>"
            + " </mapping>"
            + "</mappings>";

    private static final String XML_ROLEGROUP_CLAIM = "<mappings>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <path>realm_access.roles</path>"
            + "  <kind>ROLEGROUPCLAIM</kind>"
            + "  <persist>FULL</persist>"
            + "  <separator>_SEP_</separator>"
            + " </mapping>"
            + "</mappings>";

    private static final String XML_ROLEGROUP_CLAIM_AUTH = "<mappings>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <path>realm_access.roles</path>"
            + "  <kind>ROLEGROUPCLAIM</kind>"
            + "  <persist>AUTH</persist>"
            + "  <separator>_SEP_</separator>"
            + " </mapping>"
            + "</mappings>";

    private static final String XML_NO_MAPPING = "<mappings>"
            + "</mappings>";

    private static final String XML_MALFORMED_MAPPING = "<mappings>"
            + "</mappings";

    private static final String XML_WRONG_CONF = "<mappings>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_ROLE</attribute>"
//            + "  <kind>ROLE</kind>"  // kind null
            + "  <persist>FULL</persist>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <attribute>AD_GROUP</attribute>"
            + "  <kind>GROUP</kind>" // unknown
            + "  <persist>FULL</persist>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
//            + "  <attribute>AD_GROUPROLE</attribute>" // attribute null
            + "  <kind>GROUPROLE</kind>"
            + "  <separator>_r_</separator>"
            + "  <persist>FULL</persist>"
            + " </mapping>"
            
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_ROLE</attribute>"
            + "  <kind>ROLECLAIM</kind>"  // no path
            + "  <persist>FULL</persist>"
            + " </mapping>"

            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_GROUPROLE</attribute>"
            + "  <kind>GROUPROLE</kind>"
//            + "  <separator>_r_</separator>"  // separator null
            + "  <persist>FULL</persist>"
            + " </mapping>"
            
            + "</mappings>";
    
    private static final String JWT_NO_ROLE = "{\n"
            + "  \"header\" : {\n"
            + "    \"alg\" : \"RS256\","
            + "    \"typ\" : \"JWT\","
            + "    \"kid\" : \"l09Wlf_NY_dmMORYBjkr7deFVGVJ5TRLHW1p7DIT1ds\"\n"
            + "  },"
            + "  \"payload\" : {\n"
            + "    \"exp\" : 1768319443,"
            + "    \"iat\" : 1768319143,"
            + "    \"auth_time\" : 1768319142,"
            + "    \"jti\" : \"e64ed1da-aa8c-488f-be10-09e0c2f580c3\","
            + "    \"iss\" : \"https://localhost:8080/auth/realms/entando\","
            + "    \"aud\" : [ \"sim730\", \"account\" ],"
            + "    \"sub\" : \"5e7213c6-ad81-4094-bb24-fead709b05af\","
            + "    \"typ\" : \"Bearer\","
            + "    \"azp\" : \"entando-web\","
            + "    \"nonce\" : \"6a9f89c2-c904-4e9e-80cb-e8c1ccddd1e0\","
            + "    \"session_state\" : \"0503e261-d522-41b6-8096-1debdd2c86e7\","
            + "    \"acr\" : \"1\","
            + "    \"allowed-origins\" : [ \"https://localhost:8080\", \"*\" ],"
            + "    \"realm_access\" : {\n"
            + "      \"roles\" : [ \"offline_access\", \"uma_authorization\", \"default-roles-entando\" ]\n"
            + "    },"
            + "    \"resource_access\" : {\n"
            + "      \"aclient\" : {\n"
            + "        \"roles\" : [ \"generico\" ]\n"
            + "      },"
            + "      \"account\" : {\n"
            + "        \"roles\" : [ \"manage-account\", \"manage-account-links\", \"view-profile\" ]\n"
            + "      }\n"
            + "    },"
            + "    \"scope\" : \"openid profile email\","
            + "    \"sid\" : \"0503e261-d522-41b6-8096-1debdd2c86e7\","
            + "    \"email_verified\" : false,"
            + "    \"name\" : \"User lastname\","
            + "    \"preferred_username\" : \"user@email.it\","
            + "    \"given_name\" : \"User\","
            + "    \"family_name\" : \"lastname\","
            + "    \"email\" : \"user@email.it\","
            + "    \"miei_ruoli_custom\" : [ \"offline_access\", \"uma_authorization\", \"default-roles-entando\" ]\n"
            + "  },"
            + "  \"signature\" : \"dLENSPEPw\"\n"
            + "}";

    private static final String JWT = "{\n"
            + "    \"exp\" : 1768319443,"
            + "    \"iat\" : 1768319143,"
            + "    \"auth_time\" : 1768319142,"
            + "    \"jti\" : \"e64ed1da-aa8c-488f-be10-09e0c2f580c3\","
            + "    \"iss\" : \"https://localhost:8080/auth/realms/entando\","
            + "    \"aud\" : [ \"sim730\", \"account\" ],"
            + "    \"sub\" : \"5e7213c6-ad81-4094-bb24-fead709b05af\","
            + "    \"typ\" : \"Bearer\","
            + "    \"azp\" : \"entando-web\","
            + "    \"nonce\" : \"6a9f89c2-c904-4e9e-80cb-e8c1ccddd1e0\","
            + "    \"session_state\" : \"0503e261-d522-41b6-8096-1debdd2c86e7\","
            + "    \"acr\" : \"1\","
            + "    \"allowed-origins\" : [ \"https://localhost:8080\", \"*\" ],"
            + "    \"realm_access\" : {\n"
            + "      \"roles\" : [ \"offline_access\", \"uma_authorization\", \"default-roles-entando\", \"generico\" ]\n"
            + "    },"
            + "    \"resource_access\" : {\n"
            + "      \"sim730\" : {\n"
            + "        \"roles\" : [ \"generico\" ]\n"
            + "      },"
            + "      \"account\" : {\n"
            + "        \"roles\" : [ \"manage-account\", \"manage-account-links\", \"view-profile\" ]\n"
            + "      }\n"
            + "    },"
            + "    \"scope\" : \"openid profile email\","
            + "    \"sid\" : \"0503e261-d522-41b6-8096-1debdd2c86e7\","
            + "    \"email_verified\" : false,"
            + "    \"name\" : \"User lastname\","
            + "    \"groups\": [\n"
            + "         \"Gruppo-Microsoft-Importato\", \"altro-gruppo\" "
            + "     ],"
            + "    \"preferred_username\" : \"user@email.it\","
            + "    \"given_name\" : \"User\","
            + "    \"family_name\" : \"lastname\","
            + "    \"email\" : \"user@email.it\","
            + "    \"miei_ruoli_custom\" : [ \"offline_access\", \"uma_authorization\", \"default-roles-entando\" ]\n"
            + "  }";

    private static final String JWT_ROLEGROUP = "{\n"
            + "    \"exp\" : 1768319443,"
            + "    \"iat\" : 1768319143,"
            + "    \"auth_time\" : 1768319142,"
            + "    \"jti\" : \"e64ed1da-aa8c-488f-be10-09e0c2f580c3\","
            + "    \"iss\" : \"https://localhost:8080/auth/realms/entando\","
            + "    \"aud\" : [ \"sim730\", \"account\" ],"
            + "    \"sub\" : \"5e7213c6-ad81-4094-bb24-fead709b05af\","
            + "    \"typ\" : \"Bearer\","
            + "    \"azp\" : \"entando-web\","
            + "    \"nonce\" : \"6a9f89c2-c904-4e9e-80cb-e8c1ccddd1e0\","
            + "    \"session_state\" : \"0503e261-d522-41b6-8096-1debdd2c86e7\","
            + "    \"acr\" : \"1\","
            + "    \"allowed-origins\" : [ \"https://localhost:8080\", \"*\" ],"
            + "    \"realm_access\" : {\n"
            + "      \"roles\" : [ \"role1_SEP_group1\", \"role2_SEP_group2\" ]\n"
            + "    },"
            + "    \"scope\" : \"openid profile email\","
            + "    \"sid\" : \"0503e261-d522-41b6-8096-1debdd2c86e7\","
            + "    \"email_verified\" : false,"
            + "    \"name\" : \"User lastname\","
            + "    \"preferred_username\" : \"user@email.it\","
            + "    \"given_name\" : \"User\","
            + "    \"family_name\" : \"lastname\","
            + "    \"email\" : \"user@email.it\""
            + "  }";

    private static final String JWT_ROLEGROUP_EDGE = "{\n"
            + "    \"exp\" : 1768319443,"
            + "    \"iat\" : 1768319143,"
            + "    \"auth_time\" : 1768319142,"
            + "    \"jti\" : \"e64ed1da-aa8c-488f-be10-09e0c2f580c3\","
            + "    \"iss\" : \"https://localhost:8080/auth/realms/entando\","
            + "    \"aud\" : [ \"sim730\", \"account\" ],"
            + "    \"sub\" : \"5e7213c6-ad81-4094-bb24-fead709b05af\","
            + "    \"typ\" : \"Bearer\","
            + "    \"azp\" : \"entando-web\","
            + "    \"nonce\" : \"6a9f89c2-c904-4e9e-80cb-e8c1ccddd1e0\","
            + "    \"session_state\" : \"0503e261-d522-41b6-8096-1debdd2c86e7\","
            + "    \"acr\" : \"1\","
            + "    \"allowed-origins\" : [ \"https://localhost:8080\", \"*\" ],"
            + "    \"realm_access\" : {\n"
            + "      \"roles\" : [ \"group1\", \"_SEP_group2\" ]\n"
            + "    },"
            + "    \"scope\" : \"openid profile email\","
            + "    \"sid\" : \"0503e261-d522-41b6-8096-1debdd2c86e7\","
            + "    \"email_verified\" : false,"
            + "    \"name\" : \"User lastname\","
            + "    \"preferred_username\" : \"user@email.it\","
            + "    \"given_name\" : \"User\","
            + "    \"family_name\" : \"lastname\","
            + "    \"email\" : \"user@email.it\""
            + "  }";

}
