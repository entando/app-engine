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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.agiletec.aps.system.services.user.UserDetails;
import org.entando.entando.plugins.jpsolr.SolrTestUtils;
import org.entando.entando.plugins.jpsolr.aps.system.solr.ISolrSearchEngineManager;
import org.entando.entando.plugins.jpsolr.web.AbstractControllerIntegrationTest;
import org.entando.entando.web.utils.OAuth2TestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

/**
 * @author E.Santoboni
 */
public class SolrConfigControllerTest extends AbstractControllerIntegrationTest {
    
    @Autowired
    private ISolrSearchEngineManager solrSearchEngineManager;

    @BeforeAll
    public static void setup() throws Exception {
        SolrTestUtils.startContainer();
        AbstractControllerIntegrationTest.setup();
    }

    @AfterAll
    public static void teardown() throws Exception {
        SolrTestUtils.stopContainer();
    }

    @Test
    public void testGetConfig() throws Exception {
        UserDetails user = new OAuth2TestUtils.UserBuilder("jack_bauer", "0x24")
                .grantedToRoleAdmin().build();
        String accessToken = mockOAuthInterceptor(user);

        ResultActions result_1 = mockMvc
                .perform(get("/plugins/solr/config").header("Authorization", "Bearer " + accessToken));
        result_1.andExpect(status().isOk());
        String bodyResult_1 = result_1.andReturn().getResponse().getContentAsString();
        Assertions.assertNotNull(bodyResult_1);
        
        this.solrSearchEngineManager.refreshCmsFields();
        
        ResultActions result_2 = mockMvc
                .perform(get("/plugins/solr/config").header("Authorization", "Bearer " + accessToken));
        result_2.andExpect(status().isOk());
        String bodyResult_2 = result_2.andReturn().getResponse().getContentAsString();
        Assertions.assertNotNull(bodyResult_2);
    }

}
