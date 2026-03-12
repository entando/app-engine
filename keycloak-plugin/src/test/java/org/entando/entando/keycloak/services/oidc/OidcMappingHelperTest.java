package org.entando.entando.keycloak.services.oidc;

import java.util.Collections;
import java.util.List;
import org.entando.entando.keycloak.services.mapping.DynamicMappingElement;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OidcMappingHelperTest {

    @Test
    void shouldReturnEmptyListWhenJwtFormatIsInvalid() {
        String invalidToken = "part1.part2"; // Only 2 parts instead of 3
        DynamicMappingElement claimMapper = new DynamicMappingElement();
        claimMapper.path = "roles";
        
        List<String> result = OidcMappingHelper.extractAuthorizationsFromJwt(invalidToken, true, claimMapper, "testUser");
        
        Assertions.assertEquals(Collections.emptyList(), result);
    }

    @Test
    void shouldHandleIllegalArgumentExceptionDuringBase64Decode() {
        // A token with 3 parts but the second part is not valid Base64
        String invalidBase64Token = "header.invalid_base64_!.signature";
        DynamicMappingElement claimMapper = new DynamicMappingElement();
        claimMapper.path = "roles";

        List<String> result = OidcMappingHelper.extractAuthorizationsFromJwt(invalidBase64Token, true, claimMapper, "testUser");

        Assertions.assertEquals(Collections.emptyList(), result);
    }

    @Test
    void shouldHandleJsonProcessingException() {
        // Providing an invalid JSON string with decode=false to trigger JsonProcessingException in mapper.readTree(json)
        String invalidJson = "{ invalid json }";
        DynamicMappingElement claimMapper = new DynamicMappingElement();
        claimMapper.path = "roles";

        List<String> result = OidcMappingHelper.extractAuthorizationsFromJwt(invalidJson, false, claimMapper, "testUser");

        Assertions.assertEquals(Collections.emptyList(), result);
    }

    @Test
    void shouldHandleGenericException() {
        // Passing null for claimMapper should trigger a NullPointerException, which is caught by the generic Exception catch block
        String token = "anyToken";
        
        List<String> result = OidcMappingHelper.extractAuthorizationsFromJwt(token, false, null, "testUser");

        Assertions.assertEquals(Collections.emptyList(), result);
    }
    
    @Test
    void shouldExtractAuthorizationsSuccessfully() {
        // Test case for successful extraction to ensure basic functionality is still working
        // Payload: {"roles": ["admin", "user"]} -> Base64: eyJyb2xlcyI6IFsiYWRtaW4iLCAidXNlciJdfQ==
        String payload = "eyJyb2xlcyI6IFsiYWRtaW4iLCAidXNlciJdfQ==";
        String token = "header." + payload + ".signature";
        DynamicMappingElement claimMapper = new DynamicMappingElement();
        claimMapper.path = "roles";

        List<String> result = OidcMappingHelper.extractAuthorizationsFromJwt(token, true, claimMapper, "testUser");

        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.contains("admin"));
        Assertions.assertTrue(result.contains("user"));
    }
}
