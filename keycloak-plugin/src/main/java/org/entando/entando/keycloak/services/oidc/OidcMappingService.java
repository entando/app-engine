package org.entando.entando.keycloak.services.oidc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.entando.entando.ent.util.EntLogging.EntLogFactory;
import org.entando.entando.ent.util.EntLogging.EntLogger;
import org.entando.entando.keycloak.services.mapping.DynamicMappingElement;
import org.entando.entando.keycloak.services.oidc.model.KeycloakUser;
import org.springframework.stereotype.Service;

@Service
public class OidcMappingService {

    private static final EntLogger log = EntLogFactory.getSanitizedLogger(OidcMappingService.class);

    private final ObjectMapper mapper = new ObjectMapper();

    public List<String> extractAuthorizationsFromJwt(final String token, final boolean decode, final DynamicMappingElement claimMapper, final String username) {
        try {
            String json = decodeTokenIfNeeded(token, decode);
            JsonNode authNode = extractAuthNodeFromJson(json, claimMapper);

            if (isNodeMissing(authNode)) {
                log.debug("Path '{}' not found in JWT claims for user {}", claimMapper.path, username);
                return Collections.emptyList();
            }

            return extractAuthorizationsFromNode(authNode, claimMapper);

        } catch (IllegalArgumentException e) {
            log.error("Error decoding JWT payload for user {}", username, e);
        } catch (JsonProcessingException e) {
            log.error("Error parsing JWT JSON for user {}", username, e);
        } catch (Exception e) {
            log.error("Unexpected error importing JWT claims from path '{}' for user {}",
                    claimMapper.path, username, e);
        }
        return Collections.emptyList();
    }

    public Long extractIat(final String token, final boolean decode, final String username) {
        final DynamicMappingElement dme = new DynamicMappingElement();

        // fake claim
        dme.path = "iat";
        try {
            String json = decodeTokenIfNeeded(token, decode);
            JsonNode authNode = extractAuthNodeFromJson(json, dme);

            if (isNodeMissing(authNode)) {
                log.debug("Path '{}' in JWT claims not found for user {}", dme.path, username);
                return null;
            }

            final List<String> res = extractAuthorizationsFromNode(authNode, dme);
            // finally
            if (res != null && !res.isEmpty()) {
                return Long.valueOf(res.get(0));
            }
        } catch (IllegalArgumentException e) {
            log.error("Error decoding JWT payload for user {}", username, e);
        } catch (JsonProcessingException e) {
            log.error("Error parsing JWT JSON for user {}", username, e);
        } catch (Exception e) {
            log.error("Unexpected error importing JWT claims from path '{}' for user {}",
                    dme.path, username, e);
        }
        return null;
    }

    private String decodeTokenIfNeeded(String token, boolean decode) {
        if (!decode) {
            return token;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid JWT token format: expected 3 parts, found " + parts.length);
        }
        return new String(Base64.getUrlDecoder().decode(parts[1]));
    }

    private JsonNode extractAuthNodeFromJson(String json, DynamicMappingElement claimMapper) throws JsonProcessingException {
        final JsonNode root = mapper.readTree(json);
        final String jwtPath = "/" + claimMapper.path.replace(".", "/");
        return root.at(jwtPath);
    }

    private boolean isNodeMissing(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull();
    }

    private List<String> extractAuthorizationsFromNode(JsonNode authNode, DynamicMappingElement claimMapper) {
        if (authNode.isArray()) {
            return extractFromArrayNode(authNode);
        }
        if (authNode.isTextual()) {
            return List.of(authNode.asText());
        }
        if (authNode.isNumber()) {
            return List.of(authNode.asText());
        }
        log.warn("Unsupported node type for path '{}' in JWT: {}", claimMapper.path, authNode.getNodeType());
        return Collections.emptyList();
    }

    private List<String> extractFromArrayNode(JsonNode arrayNode) {
        List<String> authorizations = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            if (node.isTextual()) {
                authorizations.add(node.asText());
            }
        }
        return authorizations;
    }

    /**
     * Process dynamic configuration element for a user. If the attribute is missing, it skips processing.
     * @param user the Keycloak user
     * @param elem the dynamic mapping element
     * @return the list of processed attribute tokens or null if the attribute is missing
     */
    public List<String> extractAuthorizationsFromProfile(KeycloakUser user, DynamicMappingElement elem) {
        if (user.getUserRepresentation() == null
                || user.getUserRepresentation().getAttributes() == null
                || !user.getUserRepresentation().getAttributes().containsKey(elem.attribute)) {
            log.info("skipping dynamic processing for user {}", user.getUsername());
            return Collections.emptyList();
        }
        final Object kcProfileAttr = user.getUserRepresentation()
                .getAttributes()
                .get(elem.attribute);
        return handleKeycloakAttribute(kcProfileAttr);
    }

    /**
     * Process user attribute of the Keycloak profile. If it's a list, it will be flattened and split by whitespace.
     * If it's a string, it will be split by whitespace.
     * @param attribute the attribute data
     * @return the list of the processed attribute tokens
     */
    protected List<String> handleKeycloakAttribute(Object attribute) {
        if (attribute instanceof List) {
            List<Object> list = (List) attribute;
            return list.stream()
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .flatMap(s -> Arrays.stream(s.split("\\s+")))
                    .filter(token -> !token.isBlank())
                    .collect(Collectors.toUnmodifiableList());
        } else if (attribute instanceof String) {
            return Arrays.stream(((String) attribute).split("\\s+"))
                    .filter(token -> !token.isBlank())
                    .collect(Collectors.toUnmodifiableList());
        }
        return Collections.emptyList();
    }
}
