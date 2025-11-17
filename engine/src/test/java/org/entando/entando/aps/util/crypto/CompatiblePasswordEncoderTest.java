/*
 * Copyright 2019-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.aps.util.crypto;

import com.agiletec.aps.BaseTestCase;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompatiblePasswordEncoderTest extends BaseTestCase {

    private static final String SECRET = "my secret";

    private CryptoBeansConfig.BCryptEncoderWrapper bcryptEncoder;
    private CompatiblePasswordEncoder passwordEncoder;

    @BeforeEach
    public void init() throws Exception {
        // The bcryptEncoder bean is wrapped, so we get it as Object and cast
        this.bcryptEncoder = (CryptoBeansConfig.BCryptEncoderWrapper) this.getApplicationContext().getBean("bcryptEncoder");
        this.passwordEncoder = this.getApplicationContext().getBean("compatiblePasswordEncoder", CompatiblePasswordEncoder.class);
    }

    @Test
    void testBCrypt() {
        testMatches("{bcrypt}" + bcryptEncoder.getEncoder().encode(SECRET), SECRET);
    }

    @Test
    void testBCryptBuildInProdPwd() {
        // Test the passwords inserted via SQL (look for "{DIRECT USERS INSERT SQL}" in code)
        testMatches("{bcrypt}$2a$10$TMRaAmZE4w5LEeELdmpJguuSuJc2D9hUelMGmsJyK35K3PBiePqXu", "adminadmin");
        testMatches("{bcrypt}$2a$10$CkUsRinB3JkFlRE4M.FOg.XrUpYX5HySBxpEasdex7L5bh05RnX.G", "editoreditor");
        testMatches("{bcrypt}$2a$10$0Idom7PIOI4YuKzyhqDJpe3Z/0N0M0FQEvKtrSOjgF71Hkx5mKhlq", "approverapprover");
    }
    
    @Test
    void testBCryptBuildInTestPwd() {
        testMatches("{bcrypt}$2a$10$zy1zkH5mP09rGv.iSYQiPunsc7F9Rd/TpZXm03YtSfZVeHK9Nddw2", "supervisorCoach");
        testMatches("{bcrypt}$2a$10$WUtgtTwdhJdD0hTBu0aIlOgjdgv5wZ7W1BD9Nh.woEzmEfq3m1CT.", "mainEditor");
        testMatches("{bcrypt}$2a$10$NIhSwtsre0H9tVDVpcs86eN/vR816tJxEPJwbtU4XeJOoFfvOYX6m", "pageManagerCoach");
        testMatches("{bcrypt}$2a$10$pAmySl8JN1jYKRO9A88sEeesmrWiTOPndbgvifWjKW0BMD7zFk0JK", "supervisorCustomers");
        testMatches("{bcrypt}$2a$10$eAFQsWoQG9k9.D6mo0aQJu/aAXGJE/dwuOBj8sbXPL7CH3YiWRVyG", "pageManagerCustomers");
        testMatches("{bcrypt}$2a$10$6mbu1yVQ/jdgPnuqFMvbYOQklHY6VmIBUZTeYaY3OhxiGx0Yjbx3K", "editorCustomers");
        testMatches("{bcrypt}$2a$10$8KYc6sUA7fiC2Pia20J4ouMk3Meb.zW3qk0QBD8EZ0vQiI0jqysMa", "editorCoach");
        testMatches("{bcrypt}$2a$10$E9R2sHNZ/YXlDn188lpdyeoBl2iSF4E5LE8FNvxbdZbqnqlNP2mL2", "admin");
    }

    private void testMatches(String encodedPwd, String plainPwd) {
        assertThat(encodedPwd).isNotEqualTo(plainPwd);
        assertTrue(passwordEncoder.matches(plainPwd, encodedPwd));
    }
}
