package org.entando.entando.aps.system.services.api;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator.Feature;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import java.util.List;
import org.entando.entando.aps.system.services.api.model.ListResponse;
import org.entando.entando.aps.system.services.api.model.ListResponseSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ObjectMapperConfiguration {

    private List<JsonTypesProvider> jsonTypesProviders;

    @Autowired
    public void setJsonTypesProviders(List<JsonTypesProvider> jsonTypesProviders) {
        this.jsonTypesProviders = jsonTypesProviders;
    }

    @Bean("DefaultObjectMapper")
    @Primary
    public ObjectMapper defaultObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        configureDefaultSettings(objectMapper);
        return objectMapper;
    }

    @Bean("LegacyApiXmlMapper")
    public XmlMapper xmlMapper() {
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.configure(Feature.WRITE_XML_DECLARATION, true);
        xmlMapper.configure(FromXmlParser.Feature.EMPTY_ELEMENT_AS_NULL, true);

        SimpleModule simpleModule = new SimpleModule("CustomXmlSerializerModule", Version.unknownVersion());
        simpleModule.addSerializer(ListResponse.class, new ListResponseSerializer());
        xmlMapper.registerModule(simpleModule);

        configureDefaultSettings(xmlMapper);
        return xmlMapper;
    }

    private void configureDefaultSettings(ObjectMapper mapper) {
        mapper.registerModule(new JaxbAnnotationModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        registerSubtypes(mapper);
    }

    private void registerSubtypes(ObjectMapper mapper) {
        for (JsonTypesProvider jsonTypesProvider : jsonTypesProviders) {
            mapper.registerSubtypes(jsonTypesProvider.getJsonSubtypes());
        }
    }
}
