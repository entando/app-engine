package org.entando.entando.keycloak.services;

import static com.agiletec.aps.system.SystemConstants.ADMIN_USER_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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

    private KeycloakAuthorizationManager manager;

    @BeforeEach
    public void setUp() throws EntException {
        manager = new KeycloakAuthorizationManager(configuration, authorizationManager, groupManager, roleManager, configManager);
        lenient().when(authorizationManager.externalAuthSyncCheck(anyString(), any(Long.class))).thenReturn(false);
    }

    @Test
    void testGroupCreation() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn("readers");
        when(groupManager.getGroup(anyString())).thenReturn(null);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());
        when(configManager.getConfigItem("dynamicAuthMapping")).thenReturn("<DynamicMapping><enabled>true</enabled></DynamicMapping>");

        manager.init();
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
    void testGroupAndRoleCreation() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn("readers:read-all");
        when(groupManager.getGroup(anyString())).thenReturn(null);
        when(roleManager.getRole(anyString())).thenReturn(null);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());
        when(configManager.getConfigItem("dynamicAuthMapping")).thenReturn("<DynamicMapping><enabled>true</enabled></DynamicMapping>");

        manager.init();
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
    void testVerification() throws Exception {
        final String xmlEmptyGroupsRoles = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<DynamicMapping>"
                + "    <enabled>true</enabled>"
                + "    <persist>FULL</persist>"
                + "    <mappings></mappings>"
                + "    <exclusions></exclusions>"
                + "    <roles></roles>"
                + "    <groups></groups>"
                + "</DynamicMapping>";

        // 2. Mocking del configManager per restituire questo XML
        when(configManager.getConfigItem("dynamicAuthMapping")).thenReturn(xmlEmptyGroupsRoles);
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());

        manager.init();

        final Authorization readers = authorization("readers", "read-all");
        final Authorization writers = authorization("writers", "write-all");

        when(configuration.getDefaultAuthorizations()).thenReturn("readers:read-all,writers:write-all");
        when(userDetails.getAuthorizations()).thenReturn(Arrays.asList(readers, writers));

        manager.processNewUser(userDetails, null, false);

        verify(roleManager, times(0)).getRole(anyString());
        verify(groupManager, times(0)).getGroup(anyString());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Authorization>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(userDetails, times(1)).addAuthorizations(listCaptor.capture());
        assertThat(listCaptor.getValue()).isEmpty();

        verify(authorizationManager, never()).addUserAuthorization(anyString(), any());
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

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, times(1)).externalAuthSync(eq("testuser"), anyLong(), anyList(), anyList());
    }

    @Test
    void testDynamicConfigurationRoleOnLoginFromJwt() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(roleManager.getRole(anyString())).thenReturn(null);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());
        when(userDetails.getUsername()).thenReturn("testuser");
        when(configManager.getConfigItem(anyString())).thenReturn(XML_ROLE_CLAIM);

        manager.init();

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, times(1)).externalAuthSync(eq("testuser"), anyLong(), anyList(), anyList());
    }

    @Test
    void testDynamicConfigurationRoleOnLoginFromJwtNoPersist() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(roleManager.getRole(anyString())).thenReturn(null);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());
        when(userDetails.getUsername()).thenReturn("testuser");
        when(configManager.getConfigItem(anyString())).thenReturn(XML_ROLE_CLAIM_AUTH);

        manager.init();

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, never()).addUserAuthorization(eq("testuser"), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Authorization>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(userDetails, times(1)).addAuthorizations(listCaptor.capture());

        List<Authorization> captured = listCaptor.getValue();
        assertThat(captured)
                .extracting(a -> a.getRole().getName())
                .containsOnly("generico");
        assertThat(captured.get(0).getGroup()).isNull();
    }

    @Test
    void testDynamicConfigurationGroupOnLoginFromJwt() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());
        when(userDetails.getUsername()).thenReturn("testuser");
        when(configManager.getConfigItem(anyString())).thenReturn(XML_GROUP_CLAIM);

        manager.init();

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, times(1)).externalAuthSync(eq("testuser"), anyLong(), anyList(), anyList());
    }

    @Test
    void testDynamicConfigurationGroupOnLoginFromJwtNoPersist() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());
        when(userDetails.getUsername()).thenReturn("testuser");
        when(configManager.getConfigItem(anyString())).thenReturn(XML_GROUP_CLAIM_AUTH);

        manager.init();

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, never()).addUserAuthorization(eq("testuser"), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Authorization>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(userDetails, times(1)).addAuthorizations(listCaptor.capture());

        List<Authorization> captured = listCaptor.getValue();
        assertThat(captured)
                .extracting(a -> a.getGroup().getName())
                .containsExactlyInAnyOrder("Gruppo-Microsoft-Importato", "altro-gruppo");
        captured.forEach(ac -> assertThat(ac.getRole()).isNull());
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

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, times(1)).externalAuthSync(eq("testuser"), anyLong(), anyList(), anyList());
    }

    @Test
    void testDynamicConfigurationGroupOnLoginNoPersist() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_GROUP_CONF_NO_PERSIST);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setAttributes(Map.of("AD_GROUP", List.of("group")));


        when(userDetails.getUserRepresentation()).thenReturn(userRepresentation);

        manager.init();
        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, never()).addUserAuthorization(eq("testuser"), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Authorization>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(userDetails, times(1)).addAuthorizations(listCaptor.capture());

        List<Authorization> captured = listCaptor.getValue();
        assertThat(captured).hasSize(1);
        assertThat(captured.get(0).getGroup().getName()).isEqualTo("group");
        assertThat(captured.get(0).getRole()).isNull();
    }

    @Test
    void testDynamicConfigurationGroupRoleOnLogin() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_GROUP_ROLE_CONF);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setAttributes(Map.of("AD_GROUPROLE", List.of("arole_r_agroup")));

        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getUserRepresentation()).thenReturn(userRepresentation);

        manager.init();

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, times(1)).externalAuthSync(eq("testuser"), anyLong(), anyList(), anyList());
    }

    @Test
    void testDynamicConfigurationGroupRoleOnLoginNoPersist() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_GROUP_ROLE_CONF_NO_PERSIST);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setAttributes(Map.of("AD_GROUPROLE", List.of("arole_r_agroup")));

        when(userDetails.getUserRepresentation()).thenReturn(userRepresentation);

        manager.init();

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, never()).addUserAuthorization(eq("testuser"), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Authorization>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(userDetails, times(1)).addAuthorizations(listCaptor.capture());

        List<Authorization> captured = listCaptor.getValue();
        assertThat(captured).hasSize(1);
        assertThat(captured.get(0).getGroup().getName()).isEqualTo("agroup");
        // role doesn't exist, so it's not associated
        assertNull(captured.get(0).getRole());
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

        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_GROUP_ROLE_CONF);
        when(userDetails.getUsername()).thenReturn("testuser");

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setAttributes(Map.of("AD_GROUPROLE", List.of("arole_r_agroup")));

        when(userDetails.getUserRepresentation()).thenReturn(userRepresentation);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>(List.of(auth)));

        manager.init();

        manager.processNewUser(userDetails, JWT, false);

        // externalAuthSync is called (persist=FULL) but with empty toAdd since auth is already present
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Authorization>> toAddCaptor = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Authorization>> toDeleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(authorizationManager, times(1)).externalAuthSync(
                eq("testuser"), anyLong(), toAddCaptor.capture(), toDeleteCaptor.capture());
        assertThat(toAddCaptor.getValue()).isEmpty();
        assertThat(toDeleteCaptor.getValue()).isEmpty();

        // addAuthorizations called with empty list (nothing new to add)
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Authorization>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(userDetails, times(1)).addAuthorizations(listCaptor.capture());
        assertThat(listCaptor.getValue()).isEmpty();
    }

    @Test
    void testDynamicConfigurationNoGroupOnlyRoleOnLogin() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_GROUP_ROLE_CONF);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setAttributes(Map.of("AD_GROUPROLE", List.of("arole_r_")));

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
        userRepresentation.setAttributes(Map.of("AD_GROUPROLE", List.of("_r_agroup")));

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

        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_GROUP_ROLE_CONF);
        when(groupManager.getGroup(anyString())).thenReturn(group);
        when(roleManager.getRole(anyString())).thenReturn(role);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setAttributes(Map.of("AD_GROUPROLE", List.of("arole_r_agroup")));

        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getUserRepresentation()).thenReturn(userRepresentation);

        // Simulate a conflict exception
        lenient().doThrow(new EntException("Conflict"))
                .when(authorizationManager).addUserAuthorization(eq("testuser"), any());

        manager.init();

        // This should not throw an exception because it's caught in syncAuthorizations
        manager.processNewUser(userDetails, null, true);

        verify(authorizationManager, times(1)).externalAuthSync(eq("testuser"), anyLong(), anyList(), anyList());
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

        manager.processNewUser(userDetails, JWT_ROLEGROUP, false);

        verify(authorizationManager, times(1)).externalAuthSync(eq("testuser"), anyLong(), anyList(), anyList());
    }

    @Test
    void testDynamicConfigurationRoleGroupOnLoginFromJwtEdgeCases() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(roleManager.getRole(anyString())).thenReturn(null);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());
        when(userDetails.getUsername()).thenReturn("testuser");
        when(configManager.getConfigItem(anyString())).thenReturn(XML_ROLEGROUP_CLAIM);

        manager.init();

        manager.processNewUser(userDetails, JWT_ROLEGROUP_EDGE, false);

        verify(authorizationManager, times(1)).externalAuthSync(eq("testuser"), anyLong(), anyList(), anyList());
    }

    @Test
    void testDynamicConfigurationRoleGroupOnLoginFromJwtNoPersist() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());
        when(userDetails.getUsername()).thenReturn("testuser");
        when(configManager.getConfigItem(anyString())).thenReturn(XML_ROLEGROUP_CLAIM_AUTH);

        manager.init();

        manager.processNewUser(userDetails, JWT_ROLEGROUP, false);

        verify(authorizationManager, never()).addUserAuthorization(eq("testuser"), any());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Authorization>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(userDetails, times(1)).addAuthorizations(listCaptor.capture());

        List<Authorization> capturedAuths = listCaptor.getValue();
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
    void testDynamicConfigurationRoleGroupOnLoginForAdmin() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(userDetails.getUsername()).thenReturn(ADMIN_USER_NAME);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_ROLEGROUP_CLAIM_AUTH);

        manager.init();

        manager.processNewUser(userDetails, JWT_ROLEGROUP, false);
        // Per sicurezza l'utente admin non viene MAI modificato
        verify(authorizationManager, never()).addUserAuthorization(eq(ADMIN_USER_NAME), any());
        verify(userDetails, never()).addAuthorizations(anyList());
        verify(authorizationManager, never()).externalAuthSync(eq(ADMIN_USER_NAME), anyLong(), anyList(), anyList());
    }

    @Test
    void testDynamicConfigurationWithIgnoredRoles() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(userDetails.getUsername()).thenReturn("testuser");
        when(configManager.getConfigItem(anyString())).thenReturn(XML_WITH_IGNORE);

        manager.init();

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, times(1)).externalAuthSync(eq("testuser"), anyLong(), anyList(), anyList());
    }

    @Test
    void testDynamicConfigurationWithIgnoredGroups() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(userDetails.getUsername()).thenReturn("testuser");
        when(configManager.getConfigItem(anyString())).thenReturn(XML_WITH_IGNORE_GROUP);

        manager.init();

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, times(1)).externalAuthSync(eq("testuser"), anyLong(), anyList(), anyList());
    }

    @Test
    void testRoleFromProfileAndJwtWithPersistAuth() throws Exception {
        // Configurazione: un mapping per profilo (ROLE) e uno per JWT (ROLECLAIM), entrambi con persist=AUTH
        String xmlConf = "<DynamicMapping>"
                + " <persist>AUTH</persist>"
                + " <enabled>true</enabled>"
                + "<mappings>"
                + " <mapping>"
                + "  <enabled>true</enabled>"
                + "  <attribute>AD_ROLE</attribute>"
                + "  <kind>ROLE</kind>"
                + " </mapping>"
                + " <mapping>"
                + "  <enabled>true</enabled>"
                + "  <path>realm_access.roles</path>"
                + "  <kind>ROLECLAIM</kind>"
                + " </mapping>"
                + "</mappings>"

                + " <exclusions>"
                + "   <exclusion>default-roles-entando-development</exclusion>"
                + "   <exclusion>offline_access</exclusion>"
                + "   <exclusion>uma_authorization</exclusion>"
                + "  </exclusions>"
                + "  <roles>"
                + "   <role>generico</role>"
                + "   <role>role_from_profile</role>"
                + "  </roles>"
                + "  <groups>"
                + "   <group>imported_group</group>"
                + "   <group>imported_group2</group>"
                + "  </groups>"

                + "</DynamicMapping>";

        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(xmlConf);

        // Ruolo dal profilo
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setAttributes(Map.of("AD_ROLE", List.of("role_from_profile")));
        when(userDetails.getUserRepresentation()).thenReturn(userRepresentation);
        when(userDetails.getUsername()).thenReturn("testuser");
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());

        // Il JWT (costante JWT definita nella classe) contiene "generico" tra i ruoli in realm_access.roles
        // JWT_NO_ROLE non ha "generico" ma ha "offline_access", "uma_authorization", "default-roles-entando"

        manager.init();
        manager.processNewUser(userDetails, JWT, false);

        // Verifichiamo che addUserAuthorization NON sia mai chiamato (perché persist è AUTH, non FULL)
        verify(authorizationManager, never()).addUserAuthorization(anyString(), any());

        // Verifichiamo che le autorizzazioni siano state aggiunte all'oggetto userDetails
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Authorization>> listCaptor = ArgumentCaptor.forClass(List.class);
        // "generico" e "role_from_profile" (più altri eventuali dal JWT standard se non filtrati)
        verify(userDetails, org.mockito.Mockito.atLeastOnce()).addAuthorizations(listCaptor.capture());

        List<Authorization> captured = listCaptor.getValue();
        assertThat(captured).anySatisfy(a -> assertThat(a.getRole().getName()).isEqualTo("role_from_profile"));
        assertThat(captured).anySatisfy(a -> assertThat(a.getRole().getName()).isEqualTo("generico"));
    }

    @Test
    void testAuthAssignmentWhenRoleGroupExistWithPersistAuth() throws Exception {
        String xmlConf = "<DynamicMapping>"
                + " <persist>AUTH</persist>"
                + " <enabled>true</enabled>"
                + "<mappings>"
                + " <mapping>"
                + "  <enabled>true</enabled>"
                + "  <attribute>AD_ROLE</attribute>"
                + "  <kind>ROLE</kind>"
                + " </mapping>"
                + "</mappings>"
                + "  <roles>"
                + "   <role>existing_role</role>"
                + "  </roles>"
                + "</DynamicMapping>";

        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(xmlConf);

        Role existingRole = new Role();
        existingRole.setName("existing_role");
        when(roleManager.getRole("existing_role")).thenReturn(existingRole);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setAttributes(Map.of("AD_ROLE", List.of("existing_role")));
        when(userDetails.getUserRepresentation()).thenReturn(userRepresentation);

        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());

        manager.init();
        manager.processNewUser(userDetails, null, false);

        // Verifica che l'autorizzazione sia stata aggiunta all'utente
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Authorization>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(userDetails, times(1)).addAuthorizations(listCaptor.capture());
        List<Authorization> captured = listCaptor.getValue();
        assertThat(captured).hasSize(1);
        assertThat(captured.get(0).getRole().getName()).isEqualTo("existing_role");

        // Verifica che non sia stata chiamata la persistenza
        verify(authorizationManager, never()).addUserAuthorization(anyString(), any());
    }

    @Test
    void testAuthAssignmentWhenRoleExistsAndAddRoleFailsWithPersistAuth() throws Exception {
        String xmlConf = "<DynamicMapping>"
                + " <persist>AUTH</persist>"
                + " <enabled>true</enabled>"
                + "<mappings>"
                + " <mapping>"
                + "  <enabled>true</enabled>"
                + "  <attribute>AD_ROLE</attribute>"
                + "  <kind>ROLE</kind>"
                + " </mapping>"
                + "</mappings>"
                + "  <roles>"
                + "   <role>conflict_role</role>"
                + "  </roles>"
                + "</DynamicMapping>";

        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(xmlConf);

        Role existingRole = new Role();
        existingRole.setName("conflict_role");

        // First it returns null (simulating that it can’t find it), then after the addRole error, it finds it
        when(roleManager.getRole("conflict_role"))
                .thenReturn(null)
                .thenReturn(existingRole);

        // Simulate a conflict on addRole
        org.mockito.Mockito.doThrow(new EntException("Conflict"))
                .when(roleManager).addRole(any(Role.class));

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setAttributes(Map.of("AD_ROLE", List.of("conflict_role")));
        when(userDetails.getUserRepresentation()).thenReturn(userRepresentation);

        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());

        manager.init();
        manager.processNewUser(userDetails, null, false);

        // Verifichiamo che l'autorizzazione sia stata comunque aggiunta all'utente
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Authorization>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(userDetails, times(1)).addAuthorizations(listCaptor.capture());
        List<Authorization> captured = listCaptor.getValue();
        assertThat(captured).hasSize(1);
        assertThat(captured.get(0).getRole().getName()).isEqualTo("conflict_role");
    }



    @Test
    void testCleanupManagedAuthorizationsWithRolesAndGroups() throws Exception {
        String xml = "<dynamicMapping>"
                + "  <enabled>true</enabled>"
                + "  <persist>full</persist>"
                + "  <roles>"
                + "    <item>roleA</item>"
                + "    <item>roleB</item>"
                + "  </roles>"
                + "  <groups>"
                + "    <item>groupA</item>"
                + "    <item>groupB</item>"
                + "  </groups>"
                + "</dynamicMapping>";

        when(configManager.getConfigItem("dynamicAuthMapping")).thenReturn(xml);
        when(userDetails.getUsername()).thenReturn("john");

        List<Authorization> existingAuths = new ArrayList<>();
        existingAuths.add(authorization("groupA", "roleA"));
        when(userDetails.getAuthorizations()).thenReturn(existingAuths);

        manager.init();
        manager.processNewUser(userDetails, null, false);

        // It should try to sync using externalAuthSync
        verify(authorizationManager, times(1)).externalAuthSync(eq("john"), anyLong(), anyList(), anyList());
    }

    @Test
    void testSyncAuthorizationsRemovesStaleAuthorization() throws Exception {
        // User has [A(groupA,roleA), B(groupB,roleB)], dynamic produces only [A]
        // B should be in toDelete, removed from user
        String xml = "<DynamicMapping>"
                + "<persist>FULL</persist>"
                + "<enabled>true</enabled>"
                + "<mappings>"
                + " <mapping><enabled>true</enabled><attribute>AD_ROLE</attribute><kind>ROLE</kind></mapping>"
                + "</mappings>"
                + "<roles><role>roleA</role><role>roleB</role></roles>"
                + "<groups><group>groupA</group><group>groupB</group></groups>"
                + "</DynamicMapping>";

        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(xml);
        when(userDetails.getUsername()).thenReturn("testuser");

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setAttributes(Map.of("AD_ROLE", List.of("roleA")));
        when(userDetails.getUserRepresentation()).thenReturn(userRepresentation);

        Role existingRoleA = new Role();
        existingRoleA.setName("roleA");
        when(roleManager.getRole("roleA")).thenReturn(existingRoleA);

        Authorization authA = roleOnlyAuthorization("roleA");
        Authorization authB = roleOnlyAuthorization("roleB");
        List<Authorization> userAuths = new ArrayList<>(List.of(authA, authB));
        when(userDetails.getAuthorizations()).thenReturn(userAuths);

        manager.init();
        manager.processNewUser(userDetails, null, false);

        // Capture externalAuthSync args
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Authorization>> toAddCaptor = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Authorization>> toDeleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(authorizationManager, times(1)).externalAuthSync(
                eq("testuser"), anyLong(), toAddCaptor.capture(), toDeleteCaptor.capture());

        // A is already assigned, so toAdd should be empty
        assertThat(toAddCaptor.getValue()).isEmpty();
        // B is stale (managed but not in dynamic), so it should be in toDelete
        assertThat(toDeleteCaptor.getValue()).hasSize(1);
        assertThat(toDeleteCaptor.getValue().get(0).getRole().getName()).isEqualTo("roleB");

        // After removeAll, the user's mutable list should no longer contain B
        assertThat(userAuths).hasSize(1);
        assertThat(userAuths.get(0).getRole().getName()).isEqualTo("roleA");
    }

    @Test
    void testSyncAuthorizationsMixAddAndDelete() throws Exception {
        // User has [A(roleA)], dynamic produces [B(roleB)]. A deleted, B added.
        String xml = "<DynamicMapping>"
                + "<persist>FULL</persist>"
                + "<enabled>true</enabled>"
                + "<mappings>"
                + " <mapping><enabled>true</enabled><attribute>AD_ROLE</attribute><kind>ROLE</kind></mapping>"
                + "</mappings>"
                + "<roles><role>roleA</role><role>roleB</role></roles>"
                + "<groups></groups>"
                + "</DynamicMapping>";

        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(xml);
        when(userDetails.getUsername()).thenReturn("testuser");

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setAttributes(Map.of("AD_ROLE", List.of("roleB")));
        when(userDetails.getUserRepresentation()).thenReturn(userRepresentation);

        Role existingRoleB = new Role();
        existingRoleB.setName("roleB");
        when(roleManager.getRole("roleB")).thenReturn(existingRoleB);

        Authorization authA = roleOnlyAuthorization("roleA");
        List<Authorization> userAuths = new ArrayList<>(List.of(authA));
        when(userDetails.getAuthorizations()).thenReturn(userAuths);

        manager.init();
        manager.processNewUser(userDetails, null, false);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Authorization>> toAddCaptor = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Authorization>> toDeleteCaptor = ArgumentCaptor.forClass(List.class);
        verify(authorizationManager, times(1)).externalAuthSync(
                eq("testuser"), anyLong(), toAddCaptor.capture(), toDeleteCaptor.capture());

        assertThat(toAddCaptor.getValue()).hasSize(1);
        assertThat(toAddCaptor.getValue().get(0).getRole().getName()).isEqualTo("roleB");

        assertThat(toDeleteCaptor.getValue()).hasSize(1);
        assertThat(toDeleteCaptor.getValue().get(0).getRole().getName()).isEqualTo("roleA");
    }

    @Test
    void testProcessNewUserDisabledSkipsAuthProcessing() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_DISABLED);
        lenient().when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());

        manager.init();
        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, never()).externalAuthSync(anyString(), anyLong(), anyList(), anyList());
        verify(userDetails, never()).addAuthorizations(anyList());
    }

    @Test
    void testProcessNewUserAlreadySyncedSkipsProcessing() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_ROLE_CLAIM);
        lenient().when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());
        when(userDetails.getUsername()).thenReturn("testuser");

        // Override the lenient stub: checkExternalAuthSync returns true
        when(authorizationManager.externalAuthSyncCheck(eq("testuser"), anyLong())).thenReturn(true);

        manager.init();
        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, never()).externalAuthSync(anyString(), anyLong(), anyList(), anyList());
        verify(userDetails, never()).addAuthorizations(anyList());
    }

    @Test
    void testRefreshConfigurationSuccess() throws Exception {
        // First init with no mappings
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_NO_MAPPING);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());
        when(userDetails.getUsername()).thenReturn("testuser");

        manager.init();
        manager.processNewUser(userDetails, JWT, false);

        // No real mappings → externalAuthSync called with empty lists
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Authorization>> toAddCaptor1 = ArgumentCaptor.forClass(List.class);
        verify(authorizationManager, times(1)).externalAuthSync(
                eq("testuser"), anyLong(), toAddCaptor1.capture(), anyList());
        assertThat(toAddCaptor1.getValue()).isEmpty();

        // Now change config to have a real mapping and refresh
        when(configManager.getConfigItem(anyString())).thenReturn(XML_ROLE_CLAIM);

        manager.refreshConfiguration();

        // Reset the checkExternalAuthSync to allow re-processing
        when(authorizationManager.externalAuthSyncCheck(anyString(), anyLong())).thenReturn(false);

        manager.processNewUser(userDetails, JWT, false);

        // After refresh, the ROLECLAIM mapping should be active and produce "generico"
        verify(authorizationManager, times(2)).externalAuthSync(
                eq("testuser"), anyLong(), anyList(), anyList());
    }

    @Test
    void testRefreshConfigurationErrorDoesNotThrow() throws Exception {
        // First init with valid config
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_NO_MAPPING);
        manager.init();

        // Now refresh with malformed XML - should NOT throw
        when(configManager.getConfigItem(anyString())).thenReturn(XML_MALFORMED_MAPPING);
        manager.refreshConfiguration(); // should not throw

        // After error, enabled=false, so processNewUser skips dynamic processing
        lenient().when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());
        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, never()).externalAuthSync(anyString(), anyLong(), anyList(), anyList());
        verify(userDetails, never()).addAuthorizations(anyList());
    }

    @Test
    void testPersistNoneDoesNotCallExternalAuthSync() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_ROLE_CLAIM_NONE);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());
        when(userDetails.getUsername()).thenReturn("testuser");

        manager.init();
        manager.processNewUser(userDetails, JWT, false);

        // NONE → externalAuthSync never called
        verify(authorizationManager, never()).externalAuthSync(anyString(), anyLong(), anyList(), anyList());

        // But addAuthorizations IS called with the dynamic auths
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Authorization>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(userDetails, times(1)).addAuthorizations(listCaptor.capture());

        List<Authorization> captured = listCaptor.getValue();
        assertThat(captured).isNotEmpty();
        assertThat(captured).extracting(a -> a.getRole().getName()).contains("generico");
    }

    @Test
    void testPersistNoneCreatesTransientObjectsNotPersisted() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_GROUP_ROLE_CONF_NO_PERSIST);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setAttributes(Map.of("AD_GROUPROLE", List.of("arole_r_agroup")));
        when(userDetails.getUserRepresentation()).thenReturn(userRepresentation);

        manager.init();
        manager.processNewUser(userDetails, JWT, false);

        // NONE persist → no DB operations for role/group creation
        verify(roleManager, never()).addRole(any());
        verify(groupManager, never()).addGroup(any());

        // But authorizations are still added to the user object
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Authorization>> listCaptor = ArgumentCaptor.forClass(List.class);
        verify(userDetails, times(1)).addAuthorizations(listCaptor.capture());

        List<Authorization> captured = listCaptor.getValue();
        assertThat(captured).hasSize(1);
        // Transient group has description starting with "sys:"
        assertThat(captured.get(0).getGroup().getDescription()).startsWith("sys:");
    }

    @Test
    void testExternalAuthSyncArgsVerifiedExactly() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_ROLE_CLAIM);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());
        when(userDetails.getUsername()).thenReturn("testuser");

        manager.init();
        manager.processNewUser(userDetails, JWT, false);

        ArgumentCaptor<String> usernameCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Long> iatCaptor = ArgumentCaptor.forClass(Long.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Authorization>> toAddCaptor = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Authorization>> toDeleteCaptor = ArgumentCaptor.forClass(List.class);

        verify(authorizationManager, times(1)).externalAuthSync(
                usernameCaptor.capture(), iatCaptor.capture(), toAddCaptor.capture(), toDeleteCaptor.capture());

        assertThat(usernameCaptor.getValue()).isEqualTo("testuser");
        assertThat(iatCaptor.getValue()).isEqualTo(1768319143L);

        // toAdd should have exactly "generico" (the only managed role in JWT)
        assertThat(toAddCaptor.getValue()).hasSize(1);
        assertThat(toAddCaptor.getValue().get(0).getRole().getName()).isEqualTo("generico");
        assertThat(toAddCaptor.getValue().get(0).getGroup()).isNull();

        // toDelete should be empty (user had no existing managed auths)
        assertThat(toDeleteCaptor.getValue()).isEmpty();
    }

    private Authorization authorization(final String groupName, final String roleName) {
        final Group group = new Group();
        group.setName(groupName);
        final Role role = new Role();
        role.setName(roleName);
        return new Authorization(group, role);
    }

    private Authorization roleOnlyAuthorization(final String roleName) {
        final Role role = new Role();
        role.setName(roleName);
        return new Authorization(null, role);
    }

    @Test
    void testCleanSyncData() throws Exception {
        when(configManager.getConfigItem("dynamicAuthMapping")).thenReturn("<DynamicMapping><enabled>true</enabled></DynamicMapping>");
        manager.init();
        manager.setCleanBatchSize(100);

        manager.cleanSyncData();

        verify(authorizationManager, times(1)).externalAuthSyncClean(any(java.time.Instant.class), eq(100));
    }

    @Test
    void testCleanSyncDataDisabled() throws Exception {
        when(configManager.getConfigItem("dynamicAuthMapping")).thenReturn("<DynamicMapping><enabled>false</enabled></DynamicMapping>");
        manager.init();

        manager.cleanSyncData();

        verify(authorizationManager, never()).externalAuthSyncClean(any(java.time.Instant.class), anyInt());
    }

    private static final String XML_ROLE_CONF =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<DynamicMapping>"
                    + "<persist>FULL</persist>"
                    + "<enabled>true</enabled>"
                    + "<mappings>"
                    + " <mapping>"
                    + "  <enabled>true</enabled>"
                    + "  <attribute>AD_ROLE</attribute>"
                    + "  <kind>ROLE</kind>"
                    + " </mapping>"
                    + " <mapping>"
                    + "  <enabled>false</enabled>"
                    + "  <attribute>AD_GROUP</attribute>"
                    + "  <kind>GROUP</kind>"
                    + " </mapping>"
                    + " <mapping>"
                    + "  <enabled>false</enabled>"
                    + "  <attribute>AD_GROUPROLE</attribute>"
                    + "  <kind>ROLEGROUP</kind>"
                    + "  <separator>_r_</separator>"
                    + " </mapping>"
                    + "</mappings>"
                    + "<exclusions>"
                    + "   <exclusions>default-roles-entando-development</exclusions>"
                    + "   <exclusions>offline_access</exclusions>"
                    + "   <exclusions>uma_authorization</exclusions>"
                    + "  </exclusions>"
                    + "  <roles>"
                    + "   <role>imported_role</role>"
                    + "   <role>imported_role2</role>"
                    + "   <role>ruolo</role>"
                    + "  </roles>"
                    + "  <groups>"
                    + "   <group>imported_group</group>"
                    + "   <group>imported_group2</group>"
                    + "  </groups>"
                    + "</DynamicMapping>";

    private static final String XML_GROUP_CONF = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<DynamicMapping>"
            + " <persist>FULL</persist>"
            + " <enabled>true</enabled>"
            + "<mappings>"
            + "             <mapping>"
            + "              <enabled>false</enabled>"
            + "              <attribute>AD_ROLE</attribute>"
            + "              <kind>ROLE</kind>"
            + "             </mapping>"
            + "             <mapping>"
            + "              <enabled>true</enabled>"
            + "              <attribute>AD_GROUP</attribute>"
            + "              <kind>GROUP</kind>"
            + "             </mapping>"
            + "             <mapping>"
            + "              <enabled>false</enabled>"
            + "              <attribute>AD_GROUPROLE</attribute>"
            + "              <kind>ROLEGROUP</kind>"
            + "              <separator>_r_</separator>"
            + "             </mapping>"
            + "            </mappings>"
            + " <exclusions>"
            + "   <exclusions>default-roles-entando-development</exclusions>"
            + "   <exclusions>offline_access</exclusions>"
            + "   <exclusions>uma_authorization</exclusions>"
            + "  </exclusions>"
            + "  <roles>"
            + "   <role>imported_role</role>"
            + "   <role>imported_role2</role>"
            + "  </roles>"
            + "  <groups>"
            + "   <group>group</group>"
            + "   <group>imported_group2</group>"
            + "  </groups>"
            + "</DynamicMapping>";

    private static final String XML_GROUP_CONF_NO_PERSIST = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<DynamicMapping>"
            + " <persist>NONE</persist>"
            + " <enabled>true</enabled>" +
            "<mappings>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_ROLE</attribute>"
            + "  <kind>ROLE</kind>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <attribute>AD_GROUP</attribute>"
            + "  <kind>GROUP</kind>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_GROUPROLE</attribute>"
            + "  <kind>ROLEGROUP</kind>"
            + "  <separator>_r_</separator>"
            + " </mapping>"
            + "</mappings>"
            +"<exclusions>"
            + "   <exclusions>default-roles-entando-development</exclusions>"
            + "   <exclusions>offline_access</exclusions>"
            + "   <exclusions>uma_authorization</exclusions>"
            + "  </exclusions>"
            + "  <roles>"
            + "   <role>imported_role</role>"
            + "   <role>imported_role2</role>"
            + "  </roles>"
            + "  <groups>"
            + "   <group>imported_group</group>"
            + "   <group>group</group>"
            + "  </groups>"
            + "</DynamicMapping>";

    private static final String XML_GROUP_ROLE_CONF = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<DynamicMapping>"
            + " <persist>FULL</persist>"
            + " <enabled>true</enabled>"
            + "<mappings>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_ROLE</attribute>"
            + "  <kind>ROLE</kind>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_GROUP</attribute>"
            + "  <kind>GROUP</kind>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <attribute>AD_GROUPROLE</attribute>"
            + "  <kind>ROLEGROUP</kind>"
            + "  <separator>_r_</separator>"
            + " </mapping>"
            + "</mappings>"
            + "<exclusions>"
            + "   <exclusions>default-roles-entando-development</exclusions>"
            + "   <exclusions>offline_access</exclusions>"
            + "   <exclusions>uma_authorization</exclusions>"
            + "  </exclusions>"
            + "  <roles>"
            + "   <role>arole</role>"
            + "   <role>imported_role2</role>"
            + "  </roles>"
            + "  <groups>"
            + "   <group>imported_group</group>"
            + "   <group>agroup</group>"
            + "  </groups>"
            + "</DynamicMapping>";

    private static final String XML_GROUP_ROLE_CONF_NO_PERSIST = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<DynamicMapping>"
            + " <persist>NONE</persist>"
            + " <enabled>true</enabled>"
            + "<mappings>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_ROLE</attribute>"
            + "  <kind>ROLE</kind>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_GROUP</attribute>"
            + "  <kind>GROUP</kind>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <attribute>AD_GROUPROLE</attribute>"
            + "  <kind>ROLEGROUP</kind>"
            + "  <separator>_r_</separator>"
            + " </mapping>"
            + "</mappings>"
            + "<exclusions>"
            + "   <exclusions>default-roles-entando-development</exclusions>"
            + "   <exclusions>offline_access</exclusions>"
            + "   <exclusions>uma_authorization</exclusions>"
            + "  </exclusions>"
            + "  <roles>"
            + "   <role>imported_role</role>"
            + "   <role>arole</role>"
            + "  </roles>"
            + "  <groups>"
            + "   <group>imported_group</group>"
            + "   <group>agroup</group>"
            + "  </groups>"
            + "</DynamicMapping>";

    private static final String XML_WITH_IGNORE_GROUP = "<DynamicMapping>"
            + "<persist>FULL</persist>"
            + " <enabled>true</enabled>"
            + "<mappings>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <path>groups</path>"
            + "  <kind>GROUPCLAIM</kind>"
            + " </mapping>"
            + "</mappings>"
            + " <exclusions><exclusions>altro-gruppo</exclusions></exclusions>"
            + "<roles>"
            + "   <role>imported_role</role>"
            + "   <role>imported_role2</role>"
            + "  </roles>"
            + "  <groups>"
            + "   <group>Gruppo-Microsoft-Importato</group>"
            + "   <group>imported_group2</group>"
            + "  </groups>"
            + "</DynamicMapping>";

    private static final String XML_WITH_IGNORE = "<DynamicMapping>"
            + " <persist>FULL</persist>"
            + " <enabled>true</enabled>"
            + "<mappings>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <path>realm_access.roles</path>"
            + "  <kind>ROLECLAIM</kind>"
            + " </mapping>"
            + "</mappings>"
            + "<exclusions>"
            + " <exclusions>offline_access</exclusions>"
            + " <exclusions>uma_authorization</exclusions>"
            + " <exclusions>default-roles-entando</exclusions>"
            + "</exclusions>"
            + "<roles>"
            + "   <role>generico</role>"
            + "   <role>imported_role2</role>"
            + "  </roles>"
            + "  <groups>"
            + "   <group>imported_group</group>"
            + "   <group>imported_group2</group>"
            + "  </groups>"
            + "</DynamicMapping>";

    private static final String XML_ROLE_CLAIM = "<DynamicMapping>"
            + " <persist>FULL</persist>"
            + " <enabled>true</enabled>"
            + "<mappings>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <path>realm_access.roles</path>"
            + "  <kind>ROLECLAIM</kind>"
            + " </mapping>"
            + "</mappings>"
            + "<exclusions>"
            + "   <exclusions>default-roles-entando-development</exclusions>"
            + "   <exclusions>offline_access</exclusions>"
            + "   <exclusions>uma_authorization</exclusions>"
            + "   <exclusions>default-roles-entando</exclusions>"
            + "  </exclusions>"
            + "  <roles>"
            + "   <role>generico</role>"
            + "   <role>imported_role2</role>"
            + "  </roles>"
            + "  <groups>"
            + "   <group>imported_group</group>"
            + "   <group>imported_group2</group>"
            + "  </groups>"
            + "</DynamicMapping>";

    private static final String XML_ROLE_CLAIM_AUTH = "<DynamicMapping>"
            + " <persist>AUTH</persist>"
            + " <enabled>true</enabled>"
            + "<mappings>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <path>realm_access.roles</path>"
            + "  <kind>ROLECLAIM</kind>"
            + " </mapping>"
            + "</mappings>"
            + "<exclusions>"
            + "   <exclusions>default-roles-entando-development</exclusions>"
            + "   <exclusions>offline_access</exclusions>"
            + "   <exclusions>uma_authorization</exclusions>"
            + "   <exclusions>default-roles-entando</exclusions>"
            + "  </exclusions>"
            + "  <roles>"
            + "   <role>generico</role>"
            + "   <role>imported_role2</role>"
            + "  </roles>"
            + "  <groups>"
            + "   <group>imported_group</group>"
            + "   <group>imported_group2</group>"
            + "  </groups>"
            + "</DynamicMapping>";

    private static final String XML_GROUP_CLAIM = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<DynamicMapping>"
            + " <persist>FULL</persist>"
            + " <enabled>true</enabled>"
            + "<mappings>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <path>groups</path>"
            + "  <kind>GROUPCLAIM</kind>"
            + " </mapping>"
            + "</mappings>"
            + "<exclusions>"
            + "   <exclusions>default-roles-entando-development</exclusions>"
            + "   <exclusions>offline_access</exclusions>"
            + "   <exclusions>uma_authorization</exclusions>"
            + "  </exclusions>"
            + "  <roles>"
            + "   <role>imported_role</role>"
            + "   <role>imported_role2</role>"
            + "  </roles>"
            + "  <groups>"
            + "   <group>altro-gruppo</group>"
            + "   <group>Gruppo-Microsoft-Importato</group>"
            + "  </groups>"
            + "</DynamicMapping>";

    private static final String XML_GROUP_CLAIM_AUTH =  "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<DynamicMapping>"
            + " <persist>AUTH</persist>"
            + " <enabled>true</enabled>"
            + "<mappings>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <path>groups</path>"
            + "  <kind>GROUPCLAIM</kind>"
            + " </mapping>"
            + "</mappings>"
            + "<exclusions>"
            + "   <exclusions>default-roles-entando-development</exclusions>"
            + "   <exclusions>offline_access</exclusions>"
            + "   <exclusions>uma_authorization</exclusions>"
            + "  </exclusions>"
            + "  <roles>"
            + "   <role>imported_role</role>"
            + "   <role>imported_role2</role>"
            + "  </roles>"
            + "  <groups>"
            + "   <group>altro-gruppo</group>"
            + "   <group>Gruppo-Microsoft-Importato</group>"
            + "  </groups>"
            + "</DynamicMapping>";

    private static final String XML_ROLEGROUP_CLAIM = "<DynamicMapping>"
            + " <persist>FULL</persist>"
            + " <enabled>true</enabled>"
            + "<mappings>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <path>realm_access.roles</path>"
            + "  <kind>ROLEGROUPCLAIM</kind>"
            + "  <separator>_SEP_</separator>"
            + " </mapping>"
            + "</mappings>"
            + "<exclusions>"
            + "   <exclusions>default-roles-entando-development</exclusions>"
            + "   <exclusions>offline_access</exclusions>"
            + "   <exclusions>uma_authorization</exclusions>"
            + "  </exclusions>"
            + "  <roles>"
            + "   <role>role1</role>"
            + "   <role>role2</role>"
            + "   <role>group1</role>"
            + "  </roles>"
            + "  <groups>"
            + "   <group>group1</group>"
            + "   <group>group2</group>"
            + "  </groups>"
            + "</DynamicMapping>";

    private static final String XML_ROLEGROUP_CLAIM_AUTH = "<DynamicMapping>"
            + " <persist>AUTH</persist>"
            + " <enabled>true</enabled>"
            + "<mappings>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <path>realm_access.roles</path>"
            + "  <kind>ROLEGROUPCLAIM</kind>"
            + "  <separator>_SEP_</separator>"
            + " </mapping>"
            + "</mappings>"
            + "<exclusions>"
            + "   <exclusions>default-roles-entando-development</exclusions>"
            + "   <exclusions>offline_access</exclusions>"
            + "   <exclusions>uma_authorization</exclusions>"
            + "  </exclusions>"
            + "  <roles>"
            + "   <role>role1</role>"
            + "   <role>role2</role>"
            + "  </roles>"
            + "  <groups>"
            + "   <group>group1</group>"
            + "   <group>group2</group>"
            + "  </groups>"
            + "</DynamicMapping>";

    private static final String XML_NO_MAPPING = "<DynamicMapping>"
            + " <persist>FULL</persist>"
            + " <enabled>true</enabled>"
            + "<mappings>"
            + "</mappings>"
            +"</DynamicMapping>";

    private static final String XML_MALFORMED_MAPPING = "<mappings>"
            + "</mappings";

    private static final String XML_WRONG_CONF = "<DynamicMapping>"
            + " <persist>FULL</persist>"
            + " <enabled>true</enabled>"
            + "<mappings>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_ROLE</attribute>"
//            + "  <kind>ROLE</kind>"  // kind null
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <attribute>AD_GROUP</attribute>"
            + "  <kind>GROUP</kind>" // unknown
            + " </mapping>"

            + " <mapping>"
            + "  <enabled>false</enabled>"
//            + "  <attribute>AD_GROUPROLE</attribute>" // attribute null
            + "  <kind>ROLEGROUP</kind>"
            + "  <separator>_r_</separator>"
            + " </mapping>"
            
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_ROLE</attribute>"
            + "  <kind>ROLECLAIM</kind>"  // no path
            + " </mapping>"

            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_GROUPROLE</attribute>"
            + "  <kind>ROLEGROUP</kind>"
//            + "  <separator>_r_</separator>"  // separator null
            + " </mapping>"
            
            + "</mappings>"
            + "<exclusions>"
            + "   <exclusions>default-roles-entando-development</exclusions>"
            + "   <exclusions>offline_access</exclusions>"
            + "   <exclusions>uma_authorization</exclusions>"
            + "  </exclusions>"
            + "  <roles>"
            + "   <role>imported_role</role>"
            + "   <role>imported_role2</role>"
            + "  </roles>"
            + "  <groups>"
            + "   <group>imported_group</group>"
            + "   <group>imported_group2</group>"
            + "  </groups>"
            + "</DynamicMapping>";
    
    private static final String XML_DISABLED = "<DynamicMapping>"
            + "<persist>FULL</persist>"
            + "<enabled>false</enabled>"
            + "<mappings>"
            + " <mapping><enabled>true</enabled><path>realm_access.roles</path><kind>ROLECLAIM</kind></mapping>"
            + "</mappings>"
            + "<roles><role>generico</role></roles>"
            + "<groups><group>agroup</group></groups>"
            + "</DynamicMapping>";

    private static final String XML_ROLE_CLAIM_NONE = "<DynamicMapping>"
            + " <persist>NONE</persist>"
            + " <enabled>true</enabled>"
            + "<mappings>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <path>realm_access.roles</path>"
            + "  <kind>ROLECLAIM</kind>"
            + " </mapping>"
            + "</mappings>"
            + "<exclusions>"
            + "   <exclusions>default-roles-entando-development</exclusions>"
            + "   <exclusions>offline_access</exclusions>"
            + "   <exclusions>uma_authorization</exclusions>"
            + "   <exclusions>default-roles-entando</exclusions>"
            + "  </exclusions>"
            + "  <roles>"
            + "   <role>generico</role>"
            + "   <role>imported_role2</role>"
            + "  </roles>"
            + "  <groups>"
            + "   <group>imported_group</group>"
            + "   <group>imported_group2</group>"
            + "  </groups>"
            + "</DynamicMapping>";

    private static final String JWT_NO_ROLE = "{"
            + "  \"header\" : {"
            + "    \"alg\" : \"RS256\","
            + "    \"typ\" : \"JWT\","
            + "    \"kid\" : \"l09Wlf_NY_dmMORYBjkr7deFVGVJ5TRLHW1p7DIT1ds\""
            + "  },"
            + "  \"payload\" : {"
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
            + "    \"realm_access\" : {"
            + "      \"roles\" : [ \"offline_access\", \"uma_authorization\", \"default-roles-entando\" ]"
            + "    },"
            + "    \"resource_access\" : {"
            + "      \"aclient\" : {"
            + "        \"roles\" : [ \"generico\" ]"
            + "      },"
            + "      \"account\" : {"
            + "        \"roles\" : [ \"manage-account\", \"manage-account-links\", \"view-profile\" ]"
            + "      }"
            + "    },"
            + "    \"scope\" : \"openid profile email\","
            + "    \"sid\" : \"0503e261-d522-41b6-8096-1debdd2c86e7\","
            + "    \"email_verified\" : false,"
            + "    \"name\" : \"User lastname\","
            + "    \"preferred_username\" : \"user@email.it\","
            + "    \"given_name\" : \"User\","
            + "    \"family_name\" : \"lastname\","
            + "    \"email\" : \"user@email.it\","
            + "    \"miei_ruoli_custom\" : [ \"offline_access\", \"uma_authorization\", \"default-roles-entando\" ]"
            + "  },"
            + "  \"signature\" : \"dLENSPEPw\""
            + "}";

    private static final String JWT = "{"
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
            + "    \"realm_access\" : {"
            + "      \"roles\" : [ \"offline_access\", \"uma_authorization\", \"default-roles-entando\", \"generico\" ]"
            + "    },"
            + "    \"resource_access\" : {"
            + "      \"sim730\" : {"
            + "        \"roles\" : [ \"generico\" ]"
            + "      },"
            + "      \"account\" : {"
            + "        \"roles\" : [ \"manage-account\", \"manage-account-links\", \"view-profile\" ]"
            + "      }"
            + "    },"
            + "    \"scope\" : \"openid profile email\","
            + "    \"sid\" : \"0503e261-d522-41b6-8096-1debdd2c86e7\","
            + "    \"email_verified\" : false,"
            + "    \"name\" : \"User lastname\","
            + "    \"groups\": ["
            + "         \"Gruppo-Microsoft-Importato\", \"altro-gruppo\" "
            + "     ],"
            + "    \"preferred_username\" : \"user@email.it\","
            + "    \"given_name\" : \"User\","
            + "    \"family_name\" : \"lastname\","
            + "    \"email\" : \"user@email.it\","
            + "    \"miei_ruoli_custom\" : [ \"offline_access\", \"uma_authorization\", \"default-roles-entando\" ]"
            + "  }";

    private static final String JWT_ROLEGROUP = "{"
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
            + "    \"realm_access\" : {"
            + "      \"roles\" : [ \"role1_SEP_group1\", \"role2_SEP_group2\" ]"
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

    private static final String JWT_ROLEGROUP_EDGE = "{"
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
            + "    \"realm_access\" : {"
            + "      \"roles\" : [ \"group1\", \"_SEP_group2\" ]"
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
