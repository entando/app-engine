/*
 * Copyright 2024-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.entando.entando.plugins.jpcds.aps.system.storage;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class CdsConfigurationTest {

    @Test
    void shouldDtoWork(){
        CdsConfiguration config = new CdsConfiguration();
        config.setBaseURL("");
        config.setBaseDiskRoot("");
        config.setProtectedBaseURL("");
        config.setProtectedBaseDiskRoot("");
        config.setEnabled(true);
        config.setCdsPublicUrl("https://cds.entando.org");
        config.setCdsPrivateUrl("https://cds.entando.org");
        config.setCdsPath("/api/v1");
        config.setKcAuthUrl("http://localhost:8081/auth");
        config.setKcRealm("entando");
        config.setKcClientId("entando-app");
        config.setKcClientSecret("secret");

        Assertions.assertThat(config.getCdsPath()).isEqualTo("/api/v1");
        Assertions.assertThat(config.isEnabled()).isTrue();

    }


    @Test
    void shouldWorkFineWithToString() {

        CdsConfiguration config1 = new CdsConfiguration();
        config1.setBaseURL("");
        config1.setBaseDiskRoot("");
        config1.setProtectedBaseURL("");
        config1.setProtectedBaseDiskRoot("");
        config1.setEnabled(true);
        config1.setCdsPublicUrl("https://cds.entando.org");
        config1.setCdsPrivateUrl("https://cds.entando.org");
        config1.setCdsPath("/api/v1");
        config1.setKcAuthUrl("http://localhost:8081/auth");
        config1.setKcRealm("entando");
        config1.setKcClientId("entando-app");
        config1.setKcClientSecret("secret");

        CdsConfiguration config2 = new CdsConfiguration();
        config2.setBaseURL("");
        config2.setBaseDiskRoot("");
        config2.setProtectedBaseURL("");
        config2.setProtectedBaseDiskRoot("");
        config2.setEnabled(true);
        config2.setCdsPublicUrl("https://cds.entando.org");
        config2.setCdsPrivateUrl("https://cds.entando.org");
        config2.setCdsPath("/api/v1");
        config2.setKcAuthUrl("http://localhost:8081/auth");
        config2.setKcRealm("entando");
        config2.setKcClientId("entando-app");
        config2.setKcClientSecret("secret");

        Assertions.assertThat(config2.toString()).contains("https://cds.entando.org","https://cds.entando.org","entando");
        Assertions.assertThat(config1).hasToString(config2.toString());
    }

}
