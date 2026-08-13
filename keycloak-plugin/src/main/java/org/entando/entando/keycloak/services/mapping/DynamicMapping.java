package org.entando.entando.keycloak.services.mapping;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;

@JacksonXmlRootElement(localName = "dynamicMapping")
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DynamicMapping {

    @JacksonXmlElementWrapper(localName = "mappings")
    @JacksonXmlProperty(localName = "mapping")
    public List<DynamicMappingElement> mapping;

    @JacksonXmlElementWrapper(localName = "roles")
    @JacksonXmlProperty(localName = "role")
    public List<String> roles;

    @JacksonXmlElementWrapper(localName = "groups")
    @JacksonXmlProperty(localName = "group")
    public List<String> groups;

    @JacksonXmlElementWrapper(localName = "excludeUsers")
    @JacksonXmlProperty(localName = "excludeUser")
    public List<String> excludeUsers;

    public Boolean enabled;
    public PersistKind persist;

    @Deprecated(forRemoval = true)
    @JacksonXmlElementWrapper(localName = "exclusions")
    @JacksonXmlProperty(localName = "exclusion")
    public List<String> exclusions;

}
