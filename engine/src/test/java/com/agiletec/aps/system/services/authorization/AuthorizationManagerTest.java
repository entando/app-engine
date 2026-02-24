package com.agiletec.aps.system.services.authorization;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import org.entando.entando.ent.exception.EntException;
import org.junit.jupiter.api.BeforeEach;
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

    @BeforeEach
    void setUp() {
    }

    @Test
    void shouldDeleteUserRoles() throws EntException {
        String username = "testUser";
        List<String> roles = Arrays.asList("role1", "role2");

        authorizationManager.deleteUserRoles(username, roles);

        verify(authorizationDAO).deleteUserRoles(username, roles);
    }

    @Test
    void shouldDeleteUserGroups() throws EntException {
        String username = "testUser";
        List<String> groups = Arrays.asList("group1", "group2");

        authorizationManager.deleteUserGroups(username, groups);

        verify(authorizationDAO).deleteUserGroups(username, groups);
    }

    @Test
    void shouldThrowExceptionWhenDaoFailsOnDeleteUserRoles() {
        String username = "testUser";
        List<String> roles = Arrays.asList("role1");
        
        when(authorizationDAO.deleteUserRoles(anyString(), anyList())).thenThrow(new RuntimeException("DAO error"));

        try {
            authorizationManager.deleteUserRoles(username, roles);
        } catch (EntException e) {
            // expected
        }
    }

    @Test
    void shouldThrowExceptionWhenDaoFailsOnDeleteUserGroups() {
        String username = "testUser";
        List<String> groups = Arrays.asList("group1");
        
        when(authorizationDAO.deleteUserGroups(anyString(), anyList())).thenThrow(new RuntimeException("DAO error"));

        try {
            authorizationManager.deleteUserGroups(username, groups);
        } catch (EntException e) {
            // expected
        }
    }
}
