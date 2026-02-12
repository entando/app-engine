package org.entando.entando.keycloak.services.mapping;


public class DynamicMappingElement {

    public boolean enabled;
    public String attribute;
    public DynamicMappingKind kind;
    public PersistKind persist;
    public String separator; // FOR GROUPROLE ONLY
    public String path; // FOR *CLAIM ONLY

}
