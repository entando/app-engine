package org.entando.entando.keycloak.services;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.entando.entando.keycloak.services.mapping.DynamicMappingElement;
import org.entando.entando.keycloak.services.mapping.PersistKind;

@Data
public class KeycloakImportConfig {

    public KeycloakImportConfig() { }

    public transient List<DynamicMappingElement> profileMappings = new ArrayList<>();
    public transient List<DynamicMappingElement> jwtMappings =  new ArrayList<>();
    public transient List<String> ignore;
    public transient List<String> roles;
    public transient List<String> groups;
    public transient Boolean enabled;
    public transient PersistKind persist;

}
