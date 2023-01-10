package org.entando.entando.web.info;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class InfoConfigurations {

    private final Configurations build;

    private final Configurations git;

    public InfoConfigurations(){
        build = new Configurations(new ClassPathResource("META-INF/build-info.properties"), StandardCharsets.UTF_8);
        git = new Configurations(new ClassPathResource("git.properties"), StandardCharsets.UTF_8);
    }

    public InfoConfigurations(Configurations buildConfig, Configurations gitConfig){
        build = buildConfig;
        git = gitConfig;
    }


    public Configurations getBuild() {
        return this.build;
    }

    public Configurations getGit() {
        return this.git;
    }

    public static class Configurations {

        private final Resource location;

        private final Charset encoding;

        public Configurations(Resource location, Charset encoding){
            this.location = location;
            this.encoding = encoding;
        }
        public Resource getLocation() {
            return this.location;
        }

        public Charset getEncoding() {
            return this.encoding;
        }

    }

}
