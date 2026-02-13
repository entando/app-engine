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

    public List<String> extractAuthorizationsFromJwt(String token, boolean decode, DynamicMappingElement claimMapper, String username) {
        try {
            String json;
            if (decode) {
                String[] parts = token.split("\\.");
                if (parts.length != 3) {
                    log.error("Invalid JWT token format: expected 3 parts, found {}", parts.length);
                    return Collections.emptyList();
                }
                json = new String(Base64.getUrlDecoder().decode(parts[1]));
            } else {
                json = token;
            }

            final JsonNode root = mapper.readTree(json);
            final String jwtPath = "/" + claimMapper.path.replace(".", "/");
            JsonNode authNode = root.at(jwtPath);

            if (authNode == null || authNode.isMissingNode() || authNode.isNull()) {
                log.debug("Path '{}' not found in JWT claims for user {}", claimMapper.path, username);
                return Collections.emptyList();
            }

            List<String> authorizations = new ArrayList<>();
            if (authNode.isArray()) {
                for (JsonNode node : authNode) {
                    if (node.isTextual()) {
                        authorizations.add(node.asText());
                    }
                }
            } else if (authNode.isTextual()) {
                authorizations.add(authNode.asText());
            } else {
                log.warn("Unsupported node type for path '{}' in JWT: {}", claimMapper.path, authNode.getNodeType());
                return Collections.emptyList();
            }
            return authorizations;

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
