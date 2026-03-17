package org.entando.entando.keycloak.services.mapping;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;

@JacksonXmlRootElement(localName = "dynamicMapping")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DynamicMapping {

    @JacksonXmlElementWrapper(localName = "mappings")
    public List<DynamicMappingElement> mapping;

    @JacksonXmlElementWrapper(localName = "exclusions")
    public List<String> exclusions;

    @JacksonXmlElementWrapper(localName = "roles")
    public List<String> roles;

    @JacksonXmlElementWrapper(localName = "groups")
    public List<String> groups;

    public Boolean enabled;
    public PersistKind persist;

}
