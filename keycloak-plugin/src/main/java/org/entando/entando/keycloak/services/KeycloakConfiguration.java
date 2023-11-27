package org.entando.entando.keycloak.services;

import com.agiletec.aps.util.ApsTenantApplicationUtils;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.aps.system.services.tenants.ITenantManager;
import org.entando.entando.aps.system.services.tenants.TenantConfig;
import org.springframework.beans.factory.annotation.Autowired;

public class KeycloakConfiguration {

    private static final String MISSED_REQUIRED_TENANT_CONFIG_MSG = "Keycloak config from tenant missed tenant '%s' configuration";
    private ITenantManager tenantManager;
    private boolean enabled;
    private String authUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
    private String publicClientId;
    private String secureUris;
    private String defaultAuthorizations;

    @Autowired
    public void setTenantManager(ITenantManager tenantManager) {
        this.tenantManager = tenantManager;
    }

    public boolean isEnabled() {
        return getCurrentConfig()
                .map(TenantConfig::isKcEnabled)
                .orElse(enabled);
    }
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private String getFromTenantOrThrow(String value, String field){
        return Optional.ofNullable(value)
                .filter(StringUtils::isNotBlank)
                .orElseThrow(() -> new IllegalArgumentException(String.format(MISSED_REQUIRED_TENANT_CONFIG_MSG ,field)));
    }

    public String getAuthUrl() {
        return getCurrentConfig()
                .map(tc -> getFromTenantOrThrow(tc.getKcAuthUrl(),"kcAuthUrl"))
                .orElse(authUrl);
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }

    public String getRealm() {
        return getCurrentConfig()
                .map(tc -> getFromTenantOrThrow(tc.getKcRealm(),"kcRealm"))
                .orElse(realm);
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public String getClientId() {
        return getCurrentConfig()
                .map(tc -> getFromTenantOrThrow(tc.getKcClientId(),"kcClientId"))
                .orElse(clientId);
    }
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return getCurrentConfig()
                .map(tc -> getFromTenantOrThrow(tc.getKcClientSecret(),"kcClientSecret"))
                .orElse(clientSecret);
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getPublicClientId() {
        return getCurrentConfig()
                .map(tc -> getFromTenantOrThrow(tc.getKcPublicClientId(),"kcPublicClientId"))
                .orElse(publicClientId);
    }

    public void setPublicClientId(String publicClientId) {
        this.publicClientId = publicClientId;
    }

    public String getSecureUris() {
        try {
            return getCurrentConfig()
                    .map(tc -> getFromTenantOrThrow(tc.getKcSecureUris(),"kcSecureUris"))
                    .orElse(secureUris);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
    public void setSecureUris(String secureUris) {
        this.secureUris = secureUris;
    }

    public String getDefaultAuthorizations() {
        try {
            return getCurrentConfig()
                    .map(tc -> getFromTenantOrThrow(tc.getKcDefaultAuthorizations(),"kcDefaultAuthorizations"))
                    .orElse(defaultAuthorizations);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public void setDefaultAuthorizations(String defaultAuthorizations) {
        this.defaultAuthorizations = defaultAuthorizations;
    }

    private Optional<TenantConfig> getCurrentConfig() {
        return ApsTenantApplicationUtils.getTenant()
                .filter(StringUtils::isNotBlank)
                .flatMap(tenantManager::getConfigOfReadyTenant);
    }
}
