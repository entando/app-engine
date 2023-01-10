package org.entando.entando.web.info;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.Properties;

@JsonInclude(Include.NON_EMPTY)
public class BuildInfoDto extends AbstractInfoDto {

    public BuildInfoDto(Properties entries) {
        super(processEntries(entries));
    }

    public String getGroup() {
        return get("group");
    }

    public String getArtifact() {
        return get("artifact");
    }

    public String getName() {
        return get("name");
    }

    public String getVersion() {
        return get("version");
    }

    public String getTime() {
        return get("time");
    }

    private static Properties processEntries(Properties properties) {
        coercePropertyToEpoch(properties, "time");
        return properties;
    }


}
