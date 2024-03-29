/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.web.health;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.entando.entando.aps.system.services.health.IHealthService;
import org.entando.entando.web.AbstractControllerIntegrationTest;
import org.entando.entando.web.MockMvcHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class HealthControllerIntegrationTest extends AbstractControllerIntegrationTest {

    private final String healthEndpoint = "/health";

    @Autowired
    private IHealthService healthService;

    private MockMvcHelper mockMvcHelper;

    @BeforeEach
    public void setupTests() {
        mockMvcHelper = new MockMvcHelper(mockMvc);
    }

    @Test
    void isHealthy() throws Exception {

        mockMvcHelper.getMockMvc(healthEndpoint).andExpect(status().isOk());
    }
}
