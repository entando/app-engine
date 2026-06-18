package org.entando.entando.keycloak.services;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.entando.entando.keycloak.services.mapping.DynamicMappingElement;
import org.entando.entando.keycloak.services.mapping.PersistKind;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeycloakImportConfig {

    private transient List<DynamicMappingElement> profileMappings = new ArrayList<>();
    private transient List<DynamicMappingElement> jwtMappings =  new ArrayList<>();
    private transient List<String> ignore;
    private transient List<String> roles;
    private transient List<String> groups;
    private transient List<String> excludeUsers;
    private transient Boolean enabled;
    private transient PersistKind persist;

}
