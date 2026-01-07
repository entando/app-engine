package org.entando.entando.keycloak.services.mapping;


public class DynamicMappingElement {

    public Boolean enabled;
    public String attribute;
    public DynamicMappingKind kind;
    public String injectTo;
    public Boolean persist;
    public String separator;

    public String toString() {
        return "DynamicMappingElement(enabled=" + this.enabled + ", attribute=" + this.attribute + ", kind="
                + this.kind + ", injectTo=" + this.injectTo
                + ", persist=" + this.persist + ", separator=\" + this.separator + \")";
    }
}
