package com.agiletec.aps.system.services.authorization;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.group.IGroupManager;
import com.agiletec.aps.system.services.role.IRoleManager;
import com.agiletec.aps.system.services.role.Role;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TestAuthorizationDAO extends BaseTestCase {

    private AuthorizationDAO authorizationDAO;
    private List<Authorization> originalAuthorizations;
    private final String USERNAME = "admin";

    @BeforeEach
    void init() throws Exception {
        DataSource dataSource = (DataSource) this.getApplicationContext().getBean("servDataSource");
        this.authorizationDAO = new AuthorizationDAO();
        this.authorizationDAO.setDataSource(dataSource);

        IGroupManager groupManager = (IGroupManager) this.getApplicationContext().getBean(SystemConstants.GROUP_MANAGER);
        IRoleManager roleManager = (IRoleManager) this.getApplicationContext().getBean(SystemConstants.ROLE_MANAGER);

        Map<String, Role> rolesMap = roleManager.getRoles().stream().collect(Collectors.toMap(Role::getName, r -> r));
        this.originalAuthorizations = this.authorizationDAO.getUserAuthorizations(USERNAME, groupManager.getGroupsMap(), rolesMap);
    }

    @AfterEach
    void restore() {
        this.authorizationDAO.deleteUserAuthorizations(USERNAME);
        if (null != this.originalAuthorizations && !this.originalAuthorizations.isEmpty()) {
            this.authorizationDAO.addUserAuthorizations(USERNAME, this.originalAuthorizations);
        }
    }

    @Test
    void testDeleteUserAuthorizationByGroupAndRole() throws Throwable {
        authorizationDAO.deleteUserAuthorizations(USERNAME);

        // Setup with multiple authorizations
        Group freeGroup = new Group();
        freeGroup.setName("free");
        Role editorRole = new Role();
        editorRole.setName("editor");
        Authorization auth1 = new Authorization(freeGroup, editorRole);

        Group coachGroup = new Group();
        coachGroup.setName("coach");
        Role pageManagerRole = new Role();
        pageManagerRole.setName("pageManager");
        Authorization auth2 = new Authorization(coachGroup, pageManagerRole);

        Group customersGroup = new Group();
        customersGroup.setName("customers");
        Role supervisorRole = new Role();
        supervisorRole.setName("supervisor");
        Authorization auth3 = new Authorization(customersGroup, supervisorRole);

        authorizationDAO.addUserAuthorizations(USERNAME, Arrays.asList(auth1, auth2, auth3));

        Map<String, Group> groups = Map.of(
                "free", freeGroup,
                "coach", coachGroup,
                "customers", customersGroup
        );
        Map<String, Role> roles = Map.of(
                "editor", editorRole,
                "pageManager", pageManagerRole,
                "supervisor", supervisorRole
        );

        List<Authorization> authorizations = authorizationDAO.getUserAuthorizations(USERNAME, groups, roles);
        assertTrue(containsAuth(authorizations, "free", "editor"));
        assertTrue(containsAuth(authorizations, "coach", "pageManager"));
        assertTrue(containsAuth(authorizations, "customers", "supervisor"));

        //Test deleteUserAuthorizationByGroupAndRole with specific groups and roles
        authorizationDAO.deleteUserAuthorizationByGroupAndRole(
                USERNAME,
                Arrays.asList("free", "coach"),
                Arrays.asList("editor")
        );

        authorizations = authorizationDAO.getUserAuthorizations(USERNAME, groups, roles);
        assertFalse(containsAuth(authorizations, "free", "editor"));
        assertFalse(containsAuth(authorizations, "coach", "pageManager"));
        assertTrue(containsAuth(authorizations, "customers", "supervisor"));
    }

    @Test
    void testDeleteUserAuthorizationByGroupAndRoleWithOnlyGroups() throws Throwable {
        authorizationDAO.deleteUserAuthorizations(USERNAME);

        // Setup
        Group freeGroup = new Group();
        freeGroup.setName("free");
        Role editorRole = new Role();
        editorRole.setName("editor");
        Authorization auth1 = new Authorization(freeGroup, editorRole);

        Group coachGroup = new Group();
        coachGroup.setName("coach");
        Role pageManagerRole = new Role();
        pageManagerRole.setName("pageManager");
        Authorization auth2 = new Authorization(coachGroup, pageManagerRole);

        authorizationDAO.addUserAuthorizations(USERNAME, Arrays.asList(auth1, auth2));

        Map<String, Group> groups = Map.of("free", freeGroup, "coach", coachGroup);
        Map<String, Role> roles = Map.of("editor", editorRole, "pageManager", pageManagerRole);

        // Delete it by groups only
        authorizationDAO.deleteUserAuthorizationByGroupAndRole(
                USERNAME,
                Arrays.asList("free"),
                null
        );

        List<Authorization> authorizations = authorizationDAO.getUserAuthorizations(USERNAME, groups, roles);
        assertFalse(containsAuth(authorizations, "free", "editor"));
        assertTrue(containsAuth(authorizations, "coach", "pageManager"));
    }

    @Test
    void testDeleteUserAuthorizationByGroupAndRoleWithOnlyRoles() {
        authorizationDAO.deleteUserAuthorizations(USERNAME);

        // Setup
        Group freeGroup = new Group();
        freeGroup.setName("free");
        Role editorRole = new Role();
        editorRole.setName("editor");
        Authorization auth1 = new Authorization(freeGroup, editorRole);

        Group coachGroup = new Group();
        coachGroup.setName("coach");
        Role pageManagerRole = new Role();
        pageManagerRole.setName("pageManager");
        Authorization auth2 = new Authorization(coachGroup, pageManagerRole);

        authorizationDAO.addUserAuthorizations(USERNAME, Arrays.asList(auth1, auth2));

        Map<String, Group> groups = Map.of("free", freeGroup, "coach", coachGroup);
        Map<String, Role> roles = Map.of("editor", editorRole, "pageManager", pageManagerRole);

        // Delete it by roles only
        authorizationDAO.deleteUserAuthorizationByGroupAndRole(
                USERNAME,
                null,
                Arrays.asList("editor")
        );

        List<Authorization> authorizations = authorizationDAO.getUserAuthorizations(USERNAME, groups, roles);
        assertFalse(containsAuth(authorizations, "free", "editor"));
        assertTrue(containsAuth(authorizations, "coach", "pageManager"));
    }

    private boolean containsAuth(List<Authorization> authorizations, String group, String role) {
        return authorizations.stream()
                .anyMatch(a -> a.getGroup() != null && a.getGroup().getName().equals(group)
                            && a.getRole() != null && a.getRole().getName().equals(role));
    }

     /**/
}
