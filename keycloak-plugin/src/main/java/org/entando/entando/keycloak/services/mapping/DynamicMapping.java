package org.entando.entando.keycloak.services.mapping;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;

@JacksonXmlRootElement(localName = "dynamicmapping")
public class DynamicMapping {

    @JacksonXmlElementWrapper(useWrapping = false)
    public List<DynamicMappingElement> mapping;

}
