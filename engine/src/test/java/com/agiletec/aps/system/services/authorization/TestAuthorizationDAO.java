package com.agiletec.aps.system.services.authorization;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.services.group.Group;
import com.agiletec.aps.system.services.role.Role;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

class TestAuthorizationDAO extends BaseTestCase {

    @Test
    void testDeleteUserAuthorizationByGroupAndRole() throws Throwable {
        DataSource dataSource = (DataSource) this.getApplicationContext().getBean("servDataSource");
        AuthorizationDAO authorizationDAO = new AuthorizationDAO();
        authorizationDAO.setDataSource(dataSource);

        String username = "admin";

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

        authorizationDAO.addUserAuthorizations(username, Arrays.asList(auth1, auth2, auth3));

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

        List<Authorization> authorizations = authorizationDAO.getUserAuthorizations(username, groups, roles);
        assertTrue(containsAuth(authorizations, "free", "editor"));
        assertTrue(containsAuth(authorizations, "coach", "pageManager"));
        assertTrue(containsAuth(authorizations, "customers", "supervisor"));

        //Test deleteUserAuthorizationByGroupAndRole with specific groups and roles
        authorizationDAO.deleteUserAuthorizationByGroupAndRole(
                username,
                Arrays.asList("free", "coach"),
                Arrays.asList("editor")
        );

        authorizations = authorizationDAO.getUserAuthorizations(username, groups, roles);
        assertFalse(containsAuth(authorizations, "free", "editor"));
        assertFalse(containsAuth(authorizations, "coach", "pageManager"));
        assertTrue(containsAuth(authorizations, "customers", "supervisor"));
    }

    @Test
    void testDeleteUserAuthorizationByGroupAndRoleWithOnlyGroups() throws Throwable {
        DataSource dataSource = (DataSource) this.getApplicationContext().getBean("servDataSource");
        AuthorizationDAO authorizationDAO = new AuthorizationDAO();
        authorizationDAO.setDataSource(dataSource);

        String username = "admin";

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

        authorizationDAO.addUserAuthorizations(username, Arrays.asList(auth1, auth2));

        Map<String, Group> groups = Map.of("free", freeGroup, "coach", coachGroup);
        Map<String, Role> roles = Map.of("editor", editorRole, "pageManager", pageManagerRole);

        // Delete it by groups only
        authorizationDAO.deleteUserAuthorizationByGroupAndRole(
                username,
                Arrays.asList("free"),
                null
        );

        List<Authorization> authorizations = authorizationDAO.getUserAuthorizations(username, groups, roles);
        assertFalse(containsAuth(authorizations, "free", "editor"));
        assertTrue(containsAuth(authorizations, "coach", "pageManager"));
    }

    @Test
    void testDeleteUserAuthorizationByGroupAndRoleWithOnlyRoles() {
        DataSource dataSource = (DataSource) this.getApplicationContext().getBean("servDataSource");
        AuthorizationDAO authorizationDAO = new AuthorizationDAO();
        authorizationDAO.setDataSource(dataSource);

        String username = "admin";

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

        authorizationDAO.addUserAuthorizations(username, Arrays.asList(auth1, auth2));

        Map<String, Group> groups = Map.of("free", freeGroup, "coach", coachGroup);
        Map<String, Role> roles = Map.of("editor", editorRole, "pageManager", pageManagerRole);

        // Delete it by roles only
        authorizationDAO.deleteUserAuthorizationByGroupAndRole(
                username,
                null,
                Arrays.asList("editor")
        );

        List<Authorization> authorizations = authorizationDAO.getUserAuthorizations(username, groups, roles);
        assertFalse(containsAuth(authorizations, "free", "editor"));
        assertTrue(containsAuth(authorizations, "coach", "pageManager"));
    }

    private boolean containsAuth(List<Authorization> authorizations, String group, String role) {
        return authorizations.stream()
                .anyMatch(a -> a.getGroup() != null && a.getGroup().getName().equals(group)
                            && a.getRole() != null && a.getRole().getName().equals(role));
    }
}
