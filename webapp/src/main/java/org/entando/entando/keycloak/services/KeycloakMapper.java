package org.entando.entando.keycloak.services;

import org.entando.entando.keycloak.services.oidc.model.UserRepresentation;

import static java.util.Optional.ofNullable;

import com.agiletec.aps.system.services.user.User;
import org.entando.entando.keycloak.services.oidc.model.KeycloakUser;

/// Classes modified for "Casa Tecnologie Emergenti" project
public class KeycloakMapper {

    static User convertUserDetails(final UserRepresentation userRepresentation) {
        final boolean credentialsExpired = ofNullable(userRepresentation.getRequiredActions())
                .filter(actions -> actions.contains("UPDATE_PASSWORD")).isPresent();
        final KeycloakUser user = credentialsExpired ? newUserCredentialsExpired() : new KeycloakUser();
        user.setDisabled(!userRepresentation.isEnabled());
        user.setUsername(userRepresentation.getUsername());
        user.setUserRepresentation(userRepresentation);
        return user;
    }

    private static KeycloakUser newUserCredentialsExpired() {
        return new KeycloakUser() {
            {
                setMaxMonthsSinceLastAccess(-1);
                setMaxMonthsSinceLastPasswordChange(-1);
            }

            @Override
            public boolean isCredentialsNotExpired() {
                return false;
            }
        };
    }

}
