package org.entando.entando.web.info;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

public class InfoLoader {

    private final InfoConfigurations properties;

    public InfoLoader(){
        properties = new InfoConfigurations();
    }

    public InfoLoader(InfoConfigurations p){
        properties = p;
    }
    public GitInfoDto gitInfo() throws Exception {
        return new GitInfoDto(
                loadFrom(this.properties.getGit().getLocation(), "git", this.properties.getGit().getEncoding()));
    }

    public BuildInfoDto buildInfo() throws Exception {
        return new BuildInfoDto(
                loadFrom(this.properties.getBuild().getLocation(), "build", this.properties.getBuild().getEncoding()));
    }


    public static Map<String, Object> getInfo() throws Exception {
        Map<String, Object> map = new HashMap<>();
        InfoLoader ld = new InfoLoader();
        map.put("build",ld.buildInfo());
        map.put("git",ld.gitInfo());
        return map;
    }


    protected Properties loadFrom(Resource location, String prefix, Charset encoding) throws IOException {
        prefix = prefix.endsWith(".") ? prefix : prefix + ".";
        Properties source = loadSource(location, encoding);
        Properties target = new Properties();
        for (String key : source.stringPropertyNames()) {
            if (key.startsWith(prefix)) {
                target.put(key.substring(prefix.length()), source.get(key));
            }
        }
        return target;
    }

    private Properties loadSource(Resource location, Charset encoding) throws IOException {
        if (encoding != null) {
            return PropertiesLoaderUtils.loadProperties(new EncodedResource(location, encoding));
        }
        return PropertiesLoaderUtils.loadProperties(location);
    }

}
