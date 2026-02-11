package org.entando.entando.keycloak.services.mapping;


public class DynamicMappingElement {

    public boolean enabled;
    public String attribute;
    public DynamicMappingKind kind;
    public String injectTo;
    public boolean persist;
    public String separator; // FOR GROUPROLE ONLY
    public String client;   // FOR CLIENTROLE ONLY

    public String toString() {
        return "DynamicMappingElement(enabled=" + this.enabled + ", attribute=" + this.attribute + ", kind="
                + this.kind + ", injectTo=" + this.injectTo
                + ", persist=" + this.persist + ", separator=\" + this.separator + \")";
    }
}
