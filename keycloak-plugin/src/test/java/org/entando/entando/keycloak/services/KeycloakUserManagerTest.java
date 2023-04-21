package org.entando.entando.keycloak.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.agiletec.aps.system.services.authorization.IAuthorizationManager;
import com.agiletec.aps.system.services.user.User;
import com.agiletec.aps.system.services.user.UserDetails;
import java.util.List;
import org.entando.entando.keycloak.services.oidc.OpenIDConnectService;
import org.entando.entando.keycloak.services.oidc.exception.OidcException;
import org.entando.entando.keycloak.services.oidc.model.AuthResponse;
import org.entando.entando.keycloak.services.oidc.model.UserRepresentation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KeycloakUserManagerTest {

    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    @Mock
    private KeycloakService keycloakService;
    @Mock
    private IAuthorizationManager authorizationManager;
    @Mock
    private OpenIDConnectService oidcService;

    @InjectMocks
    private KeycloakUserManager userManager;

    @BeforeEach
    void setUp() {
        userManager.init();
    }

    @Test
    void getParameterNamesTest() {
        assertThat(userManager.getParameterNames().isEmpty()).isTrue();
    }

    @Test
    void shouldRemoveUserIfUserExists() {
        mockUserInListUsers(USERNAME);
        userManager.removeUser(USERNAME);
        Mockito.verify(keycloakService, Mockito.times(1)).removeUser(Mockito.any());
    }

    @Test
    void removeUserShouldIgnoreActionIfUserDoesNotExists() {
        userManager.removeUser("username");
        Mockito.verify(keycloakService, Mockito.never()).removeUser(Mockito.any());
    }

    @Test
    void shouldUpdateUserWithoutPassword() {
        mockUserInListUsers(USERNAME);
        User user = new User();
        user.setUsername(USERNAME);
        userManager.updateUser(user);
        Mockito.verify(keycloakService, Mockito.times(1)).updateUser(Mockito.any());
        Mockito.verify(keycloakService, Mockito.never()).resetPassword(Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void shouldUpdateUserWithPassword() {
        mockUserInListUsers(USERNAME);
        User user = new User();
        user.setUsername(USERNAME);
        user.setPassword(PASSWORD);
        userManager.updateUser(user);
        Mockito.verify(keycloakService, Mockito.times(1)).updateUser(Mockito.any());
        Mockito.verify(keycloakService, Mockito.times(1)).resetPassword(
                Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void shouldGetGuestUser() {
        Assertions.assertEquals("guest", userManager.getGuestUser().getUsername());
    }

    @Test
    void shouldSearchUsersIgnoringServiceAccounts() {
        List<UserRepresentation> keycloakUsers = List.of(getUserRepresentation(USERNAME),
                getUserRepresentation("service-account-1"));
        Mockito.when(keycloakService.listUsers(Mockito.any())).thenReturn(keycloakUsers);
        List<UserDetails> users = userManager.searchUsers("");
        Assertions.assertEquals(1, users.size());
        Assertions.assertEquals(USERNAME, users.get(0).getUsername());
    }

    @Test
    void shouldListAllUsersIncludingServiceAccounts() {
        List<UserRepresentation> keycloakUsers = List.of(getUserRepresentation(USERNAME),
                getUserRepresentation("service-account-1"));
        Mockito.when(keycloakService.listUsers()).thenReturn(keycloakUsers);
        List<UserDetails> users = userManager.getUsers();
        Assertions.assertEquals(2, users.size());
        Assertions.assertEquals(USERNAME, users.get(0).getUsername());
        Assertions.assertEquals("service-account-1", users.get(1).getUsername());
    }

    @Test
    void shouldSearchUsernamesIgnoringServiceAccounts() {
        List<UserRepresentation> keycloakUsers = List.of(getUserRepresentation(USERNAME),
                getUserRepresentation("service-account-1"));
        Mockito.when(keycloakService.listUsers(Mockito.any())).thenReturn(keycloakUsers);
        List<String> usernames = userManager.searchUsernames("");
        Assertions.assertEquals(1, usernames.size());
        Assertions.assertEquals(USERNAME, usernames.get(0));
    }

    @Test
    void shouldListAllUsernamesIncludingServiceAccounts() {
        List<UserRepresentation> keycloakUsers = List.of(getUserRepresentation(USERNAME),
                getUserRepresentation("service-account-1"));
        Mockito.when(keycloakService.listUsers()).thenReturn(keycloakUsers);
        List<String> usernames = userManager.getUsernames();
        Assertions.assertEquals(2, usernames.size());
        Assertions.assertEquals(USERNAME, usernames.get(0));
        Assertions.assertEquals("service-account-1", usernames.get(1));
    }

    @Test
    void shouldGetUserFromLogin() throws Exception {
        mockUserInListUsers(USERNAME);
        AuthResponse authResponse = new AuthResponse();
        Mockito.when(oidcService.login(USERNAME, PASSWORD)).thenReturn(authResponse);
        Assertions.assertNotNull(userManager.getUser(USERNAME, PASSWORD));
    }

    @Test
    void shouldGetNullFromLoginIfUserDoesNotExist() throws Exception {
        AuthResponse authResponse = new AuthResponse();
        Mockito.when(oidcService.login(USERNAME, PASSWORD)).thenReturn(authResponse);
        Assertions.assertNull(userManager.getUser(USERNAME, PASSWORD));
    }

    @Test
    void shouldGetNullFromLoginInCaseOfOidcException() throws Exception {
        Mockito.doThrow(OidcException.class).when(oidcService).login(USERNAME, PASSWORD);
        Assertions.assertNull(userManager.getUser(USERNAME, PASSWORD));
    }

    @Test
    void shouldCreateUser() {
        User user = new User();
        user.setUsername(USERNAME);
        user.setPassword(PASSWORD);
        userManager.addUser(user);
        Mockito.verify(keycloakService, Mockito.times(1)).createUser(Mockito.any());
        Mockito.verify(keycloakService, Mockito.times(1)).resetPassword(
                Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void shouldChangePassword() {
        mockUserInListUsers(USERNAME);
        userManager.changePassword(USERNAME, PASSWORD);
        Mockito.verify(keycloakService, Mockito.times(1)).resetPassword(
                Mockito.any(), Mockito.any(), Mockito.any());
    }

    @Test
    void shouldRemoveUser() {
        mockUserInListUsers(USERNAME);
        User user = new User();
        user.setUsername(USERNAME);
        userManager.removeUser(user);
        Mockito.verify(keycloakService, Mockito.times(1)).removeUser(Mockito.any());
    }

    private void mockUserInListUsers(String username) {
        UserRepresentation user = getUserRepresentation(username);
        Mockito.when(keycloakService.listUsers(username)).thenReturn(List.of(user));
    }

    private UserRepresentation getUserRepresentation(String username) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        return user;
    }
}
