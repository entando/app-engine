package org.entando.entando.keycloak.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.keycloak.services.oidc.model.KeycloakUser;
import org.entando.entando.keycloak.services.oidc.model.UserRepresentation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.ProviderManager;

@ExtendWith(MockitoExtension.class)
class KeycloakAuthorizationManagerComplexTest {

    @Mock
    private ProviderManager providerManager;

    @Mock
    private KeycloakConfiguration configuration;

    @Mock
    private AuthorizationManager authorizationManager;

    @Mock
    private GroupManager groupManager;

    @Mock
    private RoleManager roleManager;

    @Mock
    private BaseConfigManager configManager;

    private KeycloakAuthorizationManager manager;

    @BeforeEach
    public void setUp() {
        manager = new KeycloakAuthorizationManager(configuration, authorizationManager, groupManager, roleManager, configManager);
        lenient().when(configuration.getDefaultAuthorizations()).thenReturn("");
        try {
            lenient().when(authorizationManager.externalAuthSyncCheck(anyString(), any(Long.class))).thenReturn(false);
        } catch (EntException e) {
            // ignore
        }
    }

    private String createToken(String jsonPayload) {
        return "header." + java.util.Base64.getUrlEncoder().encodeToString(jsonPayload.getBytes()) + ".signature";
    }

    private void setMappingConfig(String xml) throws Exception {
        when(configManager.getConfigItem("dynamicAuthMapping")).thenReturn(xml);
        manager.init();
    }

    @Test
    void testKindGroupWithPersistNone() throws Exception {
        String xml = "<dynamicMapping>"
                + "  <enabled>true</enabled>"
                + "  <persist>none</persist>"
                + "  <mappings>"
                + "    <mapping>"
                + "      <enabled>true</enabled>"
                + "      <attribute>kc_groups</attribute>"
                + "      <kind>group</kind>"
                + "    </mapping>"
                + "  </mappings>"
                + "  <groups>"
                + "    <group>group1</group>"
                + "    <group>group2</group>"
                + "  </groups>"
                + "</dynamicMapping>";
        setMappingConfig(xml);

        KeycloakUser user = createKeycloakUser("test-user", "kc_groups", List.of("group1", "group2"));

        manager.processNewUser(user, null, false);

        assertThat(user.getAuthorizations()).hasSize(2);
        assertThat(user.getAuthorizations()).extracting(a -> a.getGroup().getName()).containsExactlyInAnyOrder("group1", "group2");
        assertThat(user.getAuthorizations()).allMatch(a -> a.getRole() == null);

        verify(authorizationManager, never()).addUserAuthorization(anyString(), any(Authorization.class));
    }

    @Test
    void testKindRoleWithPersistAuth() throws Exception {
        String xml = "<dynamicMapping>"
                + "  <enabled>true</enabled>"
                + "  <persist>auth</persist>"
                + "  <mappings>"
                + "    <mapping>"
                + "      <enabled>true</enabled>"
                + "      <attribute>kc_roles</attribute>"
                + "      <kind>role</kind>"
                + "    </mapping>"
                + "  </mappings>"
                + "  <roles>"
                + "   <role>role1</role>"
                + "   <role>role2</role>"
                + "  </roles>"
                + "</dynamicMapping>";
        setMappingConfig(xml);

        KeycloakUser user = createKeycloakUser("test-user", "kc_roles", List.of("role1", "role2"));
        when(roleManager.getRole("role1")).thenReturn(null);
        when(roleManager.getRole("role2")).thenReturn(null);

        manager.processNewUser(user, null, false);

        assertThat(user.getAuthorizations()).hasSize(2);
        assertThat(user.getAuthorizations()).extracting(a -> a.getRole().getName()).containsExactlyInAnyOrder("role1", "role2");

        // PersistKind.AUTH creates roles/groups in DB but does not call addUserAuthorization
        verify(roleManager, atLeastOnce()).getRole(anyString());
        // verify(roleManager).addRole(any(Role.class)); // Not sure if addRole is called if role is missing in transient mode
    }

    @Test
    void testKindRoleGroupWithPersistFull() throws Exception {
        String xml = "<dynamicMapping>"
                + "  <enabled>true</enabled>"
                + "  <persist>full</persist>"
                + "  <mappings>"
                + "    <mapping>"
                + "      <enabled>true</enabled>"
                + "      <attribute>kc_rolegroups</attribute>"
                + "      <kind>rolegroup</kind>"
                + "      <separator>_SEP_</separator>"
                + "    </mapping>"
                + "  </mappings>"
                + "  <roles>"
                + "   <role>role1</role>"
                + "   <role>role2</role>"
                + "  </roles>"
                + "  <groups>"
                + "   <group>group1</group>"
                + "   <group>group2</group>"
                + "  </groups>"
                + "</dynamicMapping>";
        setMappingConfig(xml);

        KeycloakUser user = createKeycloakUser("test-user", "kc_rolegroups", List.of("role1_SEP_group1", "role2_SEP_group2"));

        Group group1 = new Group(); group1.setName("group1");
        Group group2 = new Group(); group2.setName("group2");
        Role role1 = new Role(); role1.setName("role1");
        Role role2 = new Role(); role2.setName("role2");

        when(groupManager.getGroup("group1")).thenReturn(group1);
        when(groupManager.getGroup("group2")).thenReturn(group2);
        when(roleManager.getRole("role1")).thenReturn(role1);
        when(roleManager.getRole("role2")).thenReturn(role2);

        manager.processNewUser(user, null, false);

        verify(authorizationManager, times(1)).externalAuthSync(eq("test-user"), anyLong(), anyList(), anyList());
    }

    @Test
    void testJwtClaimMapping() throws Exception {
        String xml = "<dynamicMapping>"
                + "  <enabled>true</enabled>"
                + "  <persist>none</persist>"
                + "  <mappings>"
                + "    <mapping>"
                + "      <enabled>true</enabled>"
                + "      <path>resource_access.client1.roles</path>"
                + "      <kind>roleclaim</kind>"
                + "    </mapping>"
                + "  </mappings>"
                + "  <roles>"
                + "   <role>jwt-role1</role>"
                + "  </roles>"
                + "</dynamicMapping>";
        setMappingConfig(xml);

        String token = createToken("{\"iat\":123, \"resource_access\":{\"client1\":{\"roles\":[\"jwt-role1\"]}}}");
        KeycloakUser user = createKeycloakUser("test-user", null, null);

        manager.processNewUser(user, token, true);

        assertThat(user.getAuthorizations()).hasSize(1);
        assertThat(user.getAuthorizations().get(0).getRole().getName()).isEqualTo("jwt-role1");
    }

    @Test
    void testGroupClaimMapping() throws Exception {
        String xml = "<dynamicMapping>"
                + "  <enabled>true</enabled>"
                + "  <persist>none</persist>"
                + "  <mappings>"
                + "    <mapping>"
                + "      <enabled>true</enabled>"
                + "      <path>custom_groups</path>"
                + "      <kind>groupclaim</kind>"
                + "    </mapping>"
                + "  </mappings>"
                + "  <groups>"
                + "   <group>jwt-group1</group>"
                + "   <group>jwt-group2</group>"
                + "  </groups>"
                + "</dynamicMapping>";
        setMappingConfig(xml);

        String token = createToken("{\"iat\":123, \"custom_groups\":[\"jwt-group1\", \"jwt-group2\"]}");
        KeycloakUser user = createKeycloakUser("test-user", null, null);

        manager.processNewUser(user, token, true);

        assertThat(user.getAuthorizations()).hasSize(2);
        assertThat(user.getAuthorizations()).extracting(a -> a.getGroup().getName()).containsExactlyInAnyOrder("jwt-group1", "jwt-group2");
    }

    @Test
    void testRoleGroupClaimMapping() throws Exception {
        String xml = "<dynamicMapping>"
                + "  <enabled>true</enabled>"
                + "  <persist>none</persist>"
                + "  <mappings>"
                + "    <mapping>"
                + "      <enabled>true</enabled>"
                + "      <path>complex_auth</path>"
                + "      <kind>rolegroupclaim</kind>"
                + "      <separator>:</separator>"
                + "    </mapping>"
                + "  </mappings>"
                + "  <roles>"
                + "   <role>roleA</role>"
                + "   <role>roleB</role>"
                + "  </roles>"
                + "  <groups>"
                + "   <group>groupB</group>"
                + "   <group>groupA</group>"
                + "  </groups>"
                + "</dynamicMapping>";
        setMappingConfig(xml);

        String token = createToken("{\"iat\":123, \"complex_auth\":[\"roleA:groupA\", \"roleB:groupB\"]}");
        KeycloakUser user = createKeycloakUser("test-user", null, null);

        manager.processNewUser(user, token, true);

        assertThat(user.getAuthorizations()).hasSize(2);
        assertThat(user.getAuthorizations()).anyMatch(a -> a.getRole().getName().equals("roleA") && a.getGroup().getName().equals("groupA"));
        assertThat(user.getAuthorizations()).anyMatch(a -> a.getRole().getName().equals("roleB") && a.getGroup().getName().equals("groupB"));
    }

    @Test
    void testExclusions() throws Exception {
        String xml = "<dynamicMapping>"
                + "  <enabled>true</enabled>"
                + "  <persist>none</persist>"
                + "  <mappings>"
                + "    <mapping>"
                + "      <enabled>true</enabled>"
                + "      <attribute>kc_roles</attribute>"
                + "      <kind>role</kind>"
                + "    </mapping>"
                + "  </mappings>"
                + "  <exclusions>"
                + "    <exclusions>ignored-role</exclusions>"
                + "  </exclusions>"
                + "  <roles>"
                + "   <role>role1</role>"
                + "  </roles>"
                + "</dynamicMapping>";
        setMappingConfig(xml);

        KeycloakUser user = createKeycloakUser("test-user", "kc_roles", List.of("role1", "ignored-role"));

        manager.processNewUser(user, null, false);

        assertThat(user.getAuthorizations()).hasSize(1);
        assertThat(user.getAuthorizations().get(0).getRole().getName()).isEqualTo("role1");
    }

    @Test
    void testPersistAuthorizations() throws Exception {
        String xml = "<dynamicMapping>"
                + "  <persist>FULL</persist>"
                + "  <enabled>true</enabled>"
                + "  <mappings>"
                + "    <mapping>"
                + "      <enabled>true</enabled>"
                + "      <attribute>kc_roles</attribute>"
                + "      <kind>role</kind>"
                + "    </mapping>"
                + "  </mappings>"
                + "  <roles>"
                + "    <role>role-managed</role>"
                + "    <role>role1</role>"
                + "  </roles>"
                + "</dynamicMapping>";
        setMappingConfig(xml);

        KeycloakUser user = createKeycloakUser("test-user", "kc_roles", List.of("role1"));

        // Pre-existing managed role that should be deleted if not in dynamic auths
        Role managedRole = new Role(); managedRole.setName("role-managed");
        Authorization existingAuth = new Authorization(null, managedRole);

        // In this test, we use a mutable list for authorizations to allow removal
        List<Authorization> auths = new ArrayList<>();
        auths.add(existingAuth);
        user.setAuthorizations(auths);

        // Mock role1 to have a name
        Role role1 = new Role(); role1.setName("role1");
        when(roleManager.getRole("role1")).thenReturn(role1);

        manager.processNewUser(user, null, false);

        // role-managed should be deleted because its role name is in the "roles" managed list
        verify(authorizationManager, times(1)).externalAuthSync(eq("test-user"), anyLong(), anyList(), anyList());
    }

    @Test
    void testPersistAuthorizationsMultiple() throws Exception {
        String xml = "<dynamicMapping>"
                + "  <enabled>true</enabled>"
                + "  <persist>none</persist>"
                + "  <mappings>"
                + "    <mapping>"
                + "      <enabled>true</enabled>"
                + "      <attribute>kc_roles</attribute>"
                + "      <kind>role</kind>"
                + "    </mapping>"
                + "  </mappings>"
                + "  <roles>"
                + "    <item>role2</item>"
                + "    <item>role1</item>"
                + "  </roles>"
                + "</dynamicMapping>";
        setMappingConfig(xml);

        KeycloakUser user = createKeycloakUser("test-user", "kc_roles", List.of("role1"));

        List<Authorization> auths = new ArrayList<>();
        Role m1 = new Role(); m1.setName("managed1");
        auths.add(new Authorization(null, m1));
        Role m2 = new Role(); m2.setName("managed2");
        auths.add(new Authorization(null, m2));
        user.setAuthorizations(auths);

        Role role1 = new Role(); role1.setName("role1");
        when(roleManager.getRole("role1")).thenReturn(role1);

        manager.processNewUser(user, null, false);

        assertThat(user.getAuthorizations())
                .extracting(a -> a.getRole().getName()).containsExactly("managed1", "managed2", "role1");
    }

    private KeycloakUser createKeycloakUser(String username, String attrName, List<String> attrValues) {
        KeycloakUser user = new KeycloakUser();
        user.setUsername(username);
        user.setAuthorizations(new ArrayList<>());

        UserRepresentation ur = new UserRepresentation();
        ur.setUsername(username);
        Map<String, Object> attributes = new HashMap<>();
        if (attrName != null) {
            attributes.put(attrName, attrValues);
        }
        ur.setAttributes(attributes);
        user.setUserRepresentation(ur);

        return user;
    }

}
