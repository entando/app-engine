package com.agiletec.aps.system.services.authorization;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.entando.entando.ent.exception.EntException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthorizationManagerTest {

    @Mock
    private IAuthorizationDAO authorizationDAO;

    @InjectMocks
    private AuthorizationManager authorizationManager;

    @Test
    void shouldDeleteUserAuthorizationByGroupAndRole() throws EntException {
        String username = "testUser";
        List<String> groups = Arrays.asList("group1", "group2");
        List<String> roles = Arrays.asList("role1", "role2");

        authorizationManager.deleteUserAuthorizationByGroupAndRole(username, groups, roles);

        verify(authorizationDAO).deleteUserAuthorizationByGroupAndRole(username, groups, roles);
    }

    @Test
    void shouldThrowExceptionWhenDaoFailsOnDeleteUserAuthorizationByGroupAndRole() {
        String username = "testUser";
        List<String> groups = Arrays.asList("group1");
        List<String> roles = Arrays.asList("role1");

        when(authorizationDAO.deleteUserAuthorizationByGroupAndRole(anyString(), anyList(), anyList()))
                .thenThrow(new RuntimeException("DAO error"));

        assertThrows(EntException.class, () ->
                authorizationManager.deleteUserAuthorizationByGroupAndRole(username, groups, roles)
        );
    }
}
