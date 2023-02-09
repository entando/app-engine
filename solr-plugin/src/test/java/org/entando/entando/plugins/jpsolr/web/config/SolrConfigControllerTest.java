/*
 * Copyright 2021-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpsolr.web.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agiletec.aps.system.services.user.UserDetails;
import com.jayway.jsonpath.JsonPath;
import org.entando.entando.plugins.jpsolr.SolrTestExtension;
import org.entando.entando.plugins.jpsolr.aps.system.solr.ISolrSearchEngineManager;
import org.entando.entando.plugins.jpsolr.web.AbstractControllerIntegrationTest;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

/**
 * @author E.Santoboni
 */
@ExtendWith(SolrTestExtension.class)
@Tag(SolrTestExtension.RECREATE_CORE)
class SolrConfigControllerTest extends AbstractControllerIntegrationTest {

    @Autowired
    private ISolrSearchEngineManager solrSearchEngineManager;

    @BeforeAll
    public static void setup() throws Exception {
        AbstractControllerIntegrationTest.setup();
    }

    @Test
    void testRefreshType() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        ResultActions result_1 = mockMvc
                .perform(get("/plugins/solr/config").header("Authorization", "Bearer " + accessToken));
        result_1.andExpect(status().isOk());
        String bodyResult_1 = result_1.andReturn().getResponse().getContentAsString();
        Assertions.assertNotNull(bodyResult_1);

        int payloadSize = JsonPath.read(bodyResult_1, "$.size()");
        Assertions.assertEquals(4, payloadSize);
        for (int i = 0; i < payloadSize; i++) {
            int size = JsonPath.read(bodyResult_1, "$[" + i + "].attributeSettings[0].currentConfig.size()");
            Assertions.assertEquals(0, size);
        }

        ResultActions result_2 = mockMvc
                .perform(post("/plugins/solr/config/TST").header("Authorization", "Bearer " + accessToken));
        result_2.andExpect(status().isOk());
        String bodyResult_2 = result_2.andReturn().getResponse().getContentAsString();
        Assertions.assertNotNull(bodyResult_2);

        ResultActions result_3 = mockMvc
                .perform(get("/plugins/solr/config").header("Authorization", "Bearer " + accessToken));
        result_3.andExpect(status().isOk());
        String bodyResult_3 = result_3.andReturn().getResponse().getContentAsString();
        Assertions.assertNotNull(bodyResult_3);
        for (int i = 0; i < payloadSize; i++) {
            String extractedType = JsonPath.read(bodyResult_3, "$[" + i + "].typeCode");
            int size = JsonPath.read(bodyResult_3, "$[" + i + "].attributeSettings[0].currentConfig.size()");
            if (extractedType.equals("TST")) {
                Assertions.assertTrue(size > 0);
            } else {
                Assertions.assertEquals(0, size);
            }
        }

        this.solrSearchEngineManager.refreshCmsFields();

        ResultActions result_4 = mockMvc
                .perform(get("/plugins/solr/config").header("Authorization", "Bearer " + accessToken));
        result_4.andExpect(status().isOk());
        String bodyResult_4 = result_4.andReturn().getResponse().getContentAsString();
        Assertions.assertNotNull(bodyResult_4);
        for (int i = 0; i < payloadSize; i++) {
            int size = JsonPath.read(bodyResult_4, "$[" + i + "].attributeSettings[0].currentConfig.size()");
            Assertions.assertTrue(size > 0);
        }


    }

}
