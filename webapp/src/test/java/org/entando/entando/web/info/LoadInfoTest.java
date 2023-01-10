package org.entando.entando.web.info;



import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;

import java.nio.charset.StandardCharsets;
import org.entando.entando.web.info.InfoConfigurations.Configurations;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class LoadInfoTest {

    @Test
    void testLoadInfo() throws Exception{
        Configurations bProp = new Configurations(new ClassPathResource("info/build-info.properties"), StandardCharsets.UTF_8);
        Configurations gProp = new Configurations(new ClassPathResource("info/git.properties"), StandardCharsets.UTF_8);
        InfoLoader ld = new InfoLoader(new InfoConfigurations(bProp,gProp));
        BuildInfoDto build = ld.buildInfo();
        GitInfoDto git =ld.gitInfo();

        assertThat(build.getName(),equalToIgnoringCase("Entando Core: Engine"));
        assertThat(build.getArtifact(),equalToIgnoringCase("entando-engine"));
        assertThat(build.getGroup(), equalToIgnoringCase("org.entando.entando"));
        assertThat(build.getVersion(), equalToIgnoringCase("7.2.0-SNAPSHOT"));
        assertThat(build.getTime(), equalToIgnoringCase("2023-01-04T11:36:14.459Z"));

        assertThat(git.getBranch(), equalToIgnoringCase("ENG-4456_Every-Entando-BE-microservices-MUST-return-their-version-number"));
        assertThat(git.getCommit().getId(), equalToIgnoringCase("4955710"));


        Configurations gProp1 = new Configurations(new ClassPathResource("info/git.test1.properties"), StandardCharsets.UTF_8);
        ld = new InfoLoader(new InfoConfigurations(bProp,gProp1));
        build = ld.buildInfo();
        git =ld.gitInfo();
        assertThat(git.getCommit().getId(), equalToIgnoringCase("4966710"));
        assertThat(git.getCommit().getTime(), equalToIgnoringCase("2023-01-02T12:36:16+0100"));

    }

}
