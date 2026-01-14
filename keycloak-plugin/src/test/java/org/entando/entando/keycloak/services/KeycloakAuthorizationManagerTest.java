package org.entando.entando.keycloak.services;

import com.agiletec.aps.system.services.authorization.Authorization;
import com.agiletec.aps.system.services.authorization.AuthorizationManager;
import com.agiletec.aps.system.services.baseconfig.BaseConfigManager;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.group.GroupManager;
import com.agiletec.aps.system.services.role.Role;
import com.agiletec.aps.system.services.role.RoleManager;
import com.agiletec.aps.system.services.user.UserDetails;
import java.util.List;
import java.util.Map;
import org.entando.entando.keycloak.services.oidc.model.KeycloakUser;
import org.entando.entando.keycloak.services.oidc.model.UserRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.entando.entando.ent.exception.EntException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KeycloakAuthorizationManagerTest {

    @Mock private KeycloakUser userDetails;
    @Mock private KeycloakConfiguration configuration;
    @Mock private AuthorizationManager authorizationManager;
    @Mock private GroupManager groupManager;
    @Mock private RoleManager roleManager;
    @Mock private BaseConfigManager configManager;
    @Mock private KeycloakUserManager userManager;

    private KeycloakAuthorizationManager manager;

    @BeforeEach
    public void setUp() throws Exception {
        manager = new KeycloakAuthorizationManager(configuration, authorizationManager, groupManager, roleManager, configManager);
    }

    @Test
    void testGroupCreation() throws EntException {
        when(configuration.getDefaultAuthorizations()).thenReturn("readers");
        when(groupManager.getGroup(anyString())).thenReturn(null);
        when(userDetails.getAuthorizations()).thenReturn(new ArrayList<>());

        manager.processNewUser(userDetails);

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

        manager.processNewUser(userDetails);

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

        manager.processNewUser(userDetails);

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
        when(configManager.getConfigItem(anyString())).thenReturn(XML_CLIENT_ROLE);

        UserRepresentation userRepresentation = new UserRepresentation();

        manager.init();

        final ArgumentCaptor<Authorization> authCaptor = ArgumentCaptor.forClass(Authorization.class);

        manager.processNewUser(userDetails, JWT, false);

        verify(authorizationManager, times(1)).addUserAuthorization(eq("testuser"), authCaptor.capture());

        assertThat(authCaptor.getValue().getRole().getName()).isEqualTo("generico");
        assertThat(authCaptor.getValue().getGroup()).isNull();
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
    void testDynamicConfigurationNoGroupOnlyRoleOnLogin() throws Exception {
        when(configuration.getDefaultAuthorizations()).thenReturn(null);
        when(configManager.getConfigItem(anyString())).thenReturn(XML_GROUP_ROLE_CONF);

        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setAttributes(Map.of("AD_GROUPROLE", List.of("_r_arole")));

        when(userDetails.getUserRepresentation()).thenReturn(userRepresentation);

        manager.init();

        final ArgumentCaptor<Authorization> authCaptor = ArgumentCaptor.forClass(Authorization.class);

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
            + "  <persist>true</persist>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_GROUP</attribute>"
            + "  <kind>GROUP</kind>"
            + "  <persist>true</persist>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_GROUPROLE</attribute>"
            + "  <kind>GROUPROLE</kind>"
            + "  <separator>_r_</separator>"
            + "  <persist>true</persist>        "
            + " </mapping>"
            + "</mappings>";

    private static final String XML_GROUP_CONF = "<mappings>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_ROLE</attribute>"
            + "  <kind>ROLE</kind>"
            + "  <persist>true</persist>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <attribute>AD_GROUP</attribute>"
            + "  <kind>GROUP</kind>"
            + "  <persist>true</persist>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_GROUPROLE</attribute>"
            + "  <kind>GROUPROLE</kind>"
            + "  <separator>_r_</separator>"
            + "  <persist>true</persist>        "
            + " </mapping>"
            + "</mappings>";

    private static final String XML_GROUP_ROLE_CONF = "<mappings>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_ROLE</attribute>"
            + "  <kind>ROLE</kind>"
            + "  <persist>true</persist>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>false</enabled>"
            + "  <attribute>AD_GROUP</attribute>"
            + "  <kind>GROUP</kind>"
            + "  <persist>true</persist>"
            + " </mapping>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <attribute>AD_GROUPROLE</attribute>"
            + "  <kind>GROUPROLE</kind>"
            + "  <separator>_r_</separator>"
            + "  <persist>true</persist>"
            + " </mapping>"
            + "</mappings>";

    private static final String XML_CLIENT_ROLE = "<mappings>"
            + " <mapping>"
            + "  <enabled>true</enabled>"
            + "  <client>sim730</client>"
            + "  <kind>CLIENTROLE</kind>"
            + "  <persist>true</persist>"
            + " </mapping>"
            + "</mappings>";
    
    private static final String _JWT = "{\n"
            + "  \"header\" : {\n"
            + "    \"alg\" : \"RS256\",\n"
            + "    \"typ\" : \"JWT\",\n"
            + "    \"kid\" : \"l09Wlf_NY_dmMORYBjkr7deFVGVJ5TRLHW1p7DIT1ds\"\n"
            + "  },\n"
            + "  \"payload\" : {\n"
            + "    \"exp\" : 1768319443,\n"
            + "    \"iat\" : 1768319143,\n"
            + "    \"auth_time\" : 1768319142,\n"
            + "    \"jti\" : \"e64ed1da-aa8c-488f-be10-09e0c2f580c3\",\n"
            + "    \"iss\" : \"https://localhost:8080/auth/realms/entando\",\n"
            + "    \"aud\" : [ \"sim730\", \"account\" ],\n"
            + "    \"sub\" : \"5e7213c6-ad81-4094-bb24-fead709b05af\",\n"
            + "    \"typ\" : \"Bearer\",\n"
            + "    \"azp\" : \"entando-web\",\n"
            + "    \"nonce\" : \"6a9f89c2-c904-4e9e-80cb-e8c1ccddd1e0\",\n"
            + "    \"session_state\" : \"0503e261-d522-41b6-8096-1debdd2c86e7\",\n"
            + "    \"acr\" : \"1\",\n"
            + "    \"allowed-origins\" : [ \"https://localhost:8080\", \"*\" ],\n"
            + "    \"realm_access\" : {\n"
            + "      \"roles\" : [ \"offline_access\", \"uma_authorization\", \"default-roles-entando\" ]\n"
            + "    },\n"
            + "    \"resource_access\" : {\n"
            + "      \"sim730\" : {\n"
            + "        \"roles\" : [ \"generico\" ]\n"
            + "      },\n"
            + "      \"account\" : {\n"
            + "        \"roles\" : [ \"manage-account\", \"manage-account-links\", \"view-profile\" ]\n"
            + "      }\n"
            + "    },\n"
            + "    \"scope\" : \"openid profile email\",\n"
            + "    \"sid\" : \"0503e261-d522-41b6-8096-1debdd2c86e7\",\n"
            + "    \"email_verified\" : false,\n"
            + "    \"name\" : \"User lastname\",\n"
            + "    \"preferred_username\" : \"user@email.it\",\n"
            + "    \"given_name\" : \"User\",\n"
            + "    \"family_name\" : \"lastname\",\n"
            + "    \"email\" : \"user@email.it\",\n"
            + "    \"miei_ruoli_custom\" : [ \"offline_access\", \"uma_authorization\", \"default-roles-entando\" ]\n"
            + "  },\n"
            + "  \"signature\" : \"dLENSPEPw\"\n"
            + "}";

    private static final String JWT = "{\n"
            + "    \"exp\" : 1768319443,\n"
            + "    \"iat\" : 1768319143,\n"
            + "    \"auth_time\" : 1768319142,\n"
            + "    \"jti\" : \"e64ed1da-aa8c-488f-be10-09e0c2f580c3\",\n"
            + "    \"iss\" : \"https://localhost:8080/auth/realms/entando\",\n"
            + "    \"aud\" : [ \"sim730\", \"account\" ],\n"
            + "    \"sub\" : \"5e7213c6-ad81-4094-bb24-fead709b05af\",\n"
            + "    \"typ\" : \"Bearer\",\n"
            + "    \"azp\" : \"entando-web\",\n"
            + "    \"nonce\" : \"6a9f89c2-c904-4e9e-80cb-e8c1ccddd1e0\",\n"
            + "    \"session_state\" : \"0503e261-d522-41b6-8096-1debdd2c86e7\",\n"
            + "    \"acr\" : \"1\",\n"
            + "    \"allowed-origins\" : [ \"https://localhost:8080\", \"*\" ],\n"
            + "    \"realm_access\" : {\n"
            + "      \"roles\" : [ \"offline_access\", \"uma_authorization\", \"default-roles-entando\" ]\n"
            + "    },\n"
            + "    \"resource_access\" : {\n"
            + "      \"sim730\" : {\n"
            + "        \"roles\" : [ \"generico\" ]\n"
            + "      },\n"
            + "      \"account\" : {\n"
            + "        \"roles\" : [ \"manage-account\", \"manage-account-links\", \"view-profile\" ]\n"
            + "      }\n"
            + "    },\n"
            + "    \"scope\" : \"openid profile email\",\n"
            + "    \"sid\" : \"0503e261-d522-41b6-8096-1debdd2c86e7\",\n"
            + "    \"email_verified\" : false,\n"
            + "    \"name\" : \"User lastname\",\n"
            + "    \"preferred_username\" : \"user@email.it\",\n"
            + "    \"given_name\" : \"User\",\n"
            + "    \"family_name\" : \"lastname\",\n"
            + "    \"email\" : \"user@email.it\",\n"
            + "    \"miei_ruoli_custom\" : [ \"offline_access\", \"uma_authorization\", \"default-roles-entando\" ]\n"
            + "  }";
}
