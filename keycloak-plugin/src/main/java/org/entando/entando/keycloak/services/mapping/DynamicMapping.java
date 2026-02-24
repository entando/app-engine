package org.entando.entando.keycloak.services.mapping;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;

@JacksonXmlRootElement(localName = "dynamicmapping")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DynamicMapping {

    @JacksonXmlElementWrapper(useWrapping = false)
    public List<DynamicMappingElement> mapping;

    @JacksonXmlElementWrapper(useWrapping = false)
    public List<String> ignore;

    @JacksonXmlElementWrapper(localName = "roles")
    public List<String> roles;

    @JacksonXmlElementWrapper(localName = "groups")
    public List<String> groups;

}
