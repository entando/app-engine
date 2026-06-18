package org.entando.entando.keycloak.services.mapping;


public class DynamicMappingElement {

    public boolean enabled;
    public String attribute;
    public DynamicMappingKind kind;
    public String separator; // FOR ROLEGROUP and ROLEGROUPCLAIM ONLY
    public SingleTokenFallback singleTokenFallback; // FOR ROLEGROUP and ROLEGROUPCLAIM ONLY
    public String path; // FOR *CLAIM ONLY

}
