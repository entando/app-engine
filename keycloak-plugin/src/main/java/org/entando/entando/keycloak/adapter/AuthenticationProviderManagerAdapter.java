package org.entando.entando.keycloak.adapter;

import com.agiletec.aps.system.services.user.AuthenticationProviderManager;
import com.agiletec.aps.system.services.user.IAuthenticationProviderManager;
import com.agiletec.aps.system.services.user.IUserManager;
import com.agiletec.aps.system.services.user.UserDetails;
import java.util.Optional;
import org.entando.entando.ent.exception.EntException;
import org.entando.entando.keycloak.services.KeycloakAuthenticationProviderManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

public class AuthenticationProviderManagerAdapter extends AuthenticationProviderManager implements IAuthenticationProviderManager {

    private boolean keycloakEnabled;

    private final KeycloakAuthenticationProviderManager keycloak;

    public AuthenticationProviderManagerAdapter(final IUserManager userManager) {
        this.setUserManager(userManager);
        keycloak = new KeycloakAuthenticationProviderManager(userManager);
    }

    @Override
    public UserDetails getUser(final String username) throws EntException {
        UserDetails user = Optional.ofNullable(keycloakEnabled
                ? keycloak.getUser(username)
                : super.getUser(username)).orElse(null);
        super.addUserAuthorizations(user);
        return user;
    }

    @Override
    public UserDetails getUser(final String username, final String password) throws EntException {
        UserDetails user = Optional.ofNullable(keycloakEnabled
                ? keycloak.getUser(username, password)
                : super.getUser(username, password)).orElse(null);
        super.addUserAuthorizations(user);
        return user;
    }

    @Override
    public Authentication authenticate(final Authentication authentication) throws AuthenticationException {
        return keycloakEnabled
                ? keycloak.authenticate(authentication)
                : super.authenticate(authentication);
    }

    @Override
    public org.springframework.security.core.userdetails.UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        return keycloakEnabled
                ? keycloak.loadUserByUsername(username)
                : super.loadUserByUsername(username);
    }

    public void setKeycloakEnabled(final boolean keycloakEnabled) {
         this.keycloakEnabled = keycloakEnabled;
    }
    
}
