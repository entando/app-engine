package org.entando.entando.web.swagger;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller to serve Swagger UI page with OAuth2 authentication
 */
@Controller
public class SwaggerUiController {

    @GetMapping(value = "/swagger-ui.html", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String swaggerUi(HttpServletRequest request) {
        String contextPath = request.getContextPath();

        // Build absolute URL for OAuth2 redirect
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String baseUrl = scheme + "://" + serverName +
                        (serverPort != 80 && serverPort != 443 ? ":" + serverPort : "");

        String SWAGGER_UI_URL = baseUrl + contextPath + "/api/swagger-ui.html";
        String SWAGGER_APP_URL = contextPath + "/api/webjars/swagger-ui/5.18.2";
        String API_DOCS_URL = contextPath + "/api/v3/api-docs";
        String oauth2RedirectUrl = baseUrl + contextPath + "/api/webjars/swagger-ui/5.18.2/oauth2-redirect.html";

        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Entando API Documentation</title>\n" +
                "    <link rel=\"stylesheet\" type=\"text/css\" href=\"" + SWAGGER_APP_URL + "/swagger-ui.css\" />\n" +
                "    <link rel=\"icon\" type=\"image/png\" href=\"" + SWAGGER_APP_URL + "/favicon-32x32.png\" sizes=\"32x32\" />\n" +
                "    <link rel=\"icon\" type=\"image/png\" href=\"" + SWAGGER_APP_URL + "/favicon-16x16.png\" sizes=\"16x16\" />\n" +
                "    <style>\n" +
                "        html { box-sizing: border-box; overflow: -moz-scrollbars-vertical; overflow-y: scroll; }\n" +
                "        *, *:before, *:after { box-sizing: inherit; }\n" +
                "        body { margin: 0; padding: 0; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id=\"swagger-ui\"></div>\n" +
                "    <script src=\"" + SWAGGER_APP_URL + "/swagger-ui-bundle.js\" charset=\"UTF-8\"></script>\n" +
                "    <script src=\"" + SWAGGER_APP_URL + "/swagger-ui-standalone-preset.js\" charset=\"UTF-8\"></script>\n" +
                "    <script>\n" +
                "        // OAuth Logout and Token Refresh Plugin\n" +
                "        function OAuthLogoutPlugin() {\n" +
                "            var lastAuthUrl = null;\n" +
                "            var refreshTimer = null;\n" +
                "            \n" +
                "            function refreshAccessToken(system, refreshToken, tokenUrl) {\n" +
                "                console.log('[Token Refresh] Attempting to refresh token...');\n" +
                "                \n" +
                "                // Extract client credentials from auth state\n" +
                "                var auth = system.authSelectors.authorized();\n" +
                "                var entandoAuth = auth.get('entando');\n" +
                "                var clientId = entandoAuth.get('clientId') || 'swagger';\n" +
                "                var clientSecret = entandoAuth.get('clientSecret') || '';\n" +
                "                \n" +
                "                console.log('[Token Refresh] Using client_id:', clientId);\n" +
                "                \n" +
                "                var params = {\n" +
                "                    'grant_type': 'refresh_token',\n" +
                "                    'refresh_token': refreshToken,\n" +
                "                    'client_id': clientId\n" +
                "                };\n" +
                "                \n" +
                "                // Only add client_secret if it exists (public clients don't have secrets)\n" +
                "                if (clientSecret && clientSecret.length > 0) {\n" +
                "                    params['client_secret'] = clientSecret;\n" +
                "                }\n" +
                "                \n" +
                "                return fetch(tokenUrl, {\n" +
                "                    method: 'POST',\n" +
                "                    headers: {\n" +
                "                        'Content-Type': 'application/x-www-form-urlencoded'\n" +
                "                    },\n" +
                "                    body: new URLSearchParams(params)\n" +
                "                })\n" +
                "                .then(function(response) {\n" +
                "                    if (!response.ok) {\n" +
                "                        console.error('[Token Refresh] Failed:', response.status, response.statusText);\n" +
                "                        throw new Error('Token refresh failed');\n" +
                "                    }\n" +
                "                    return response.json();\n" +
                "                })\n" +
                "                .then(function(data) {\n" +
                "                    console.log('[Token Refresh] Success! New token expires in', data.expires_in, 'seconds');\n" +
                "                    \n" +
                "                    // Get current auth to preserve all fields\n" +
                "                    var currentAuth = system.authSelectors.authorized().get('entando');\n" +
                "                    var schema = currentAuth ? currentAuth.get('schema') : null;\n" +
                "                    var currentClientId = currentAuth ? currentAuth.get('clientId') : null;\n" +
                "                    var currentClientSecret = currentAuth ? currentAuth.get('clientSecret') : null;\n" +
                "                    \n" +
                "                    // Update token in Swagger UI while preserving client credentials\n" +
                "                    var newAuth = {\n" +
                "                        entando: {\n" +
                "                            name: 'entando',\n" +
                "                            schema: schema,\n" +
                "                            value: data.access_token,\n" +
                "                            clientId: currentClientId,\n" +
                "                            clientSecret: currentClientSecret,\n" +
                "                            token: {\n" +
                "                                access_token: data.access_token,\n" +
                "                                refresh_token: data.refresh_token || refreshToken,\n" +
                "                                expires_in: data.expires_in,\n" +
                "                                token_type: data.token_type || 'Bearer',\n" +
                "                                id_token: data.id_token\n" +
                "                            }\n" +
                "                        }\n" +
                "                    };\n" +
                "                    \n" +
                "                    system.authActions.authorize(newAuth);\n" +
                "                    \n" +
                "                    // Schedule next refresh\n" +
                "                    if (data.expires_in && data.refresh_token) {\n" +
                "                        scheduleTokenRefresh(data.expires_in, data.refresh_token, system, tokenUrl);\n" +
                "                    }\n" +
                "                    \n" +
                "                    return data;\n" +
                "                })\n" +
                "                .catch(function(error) {\n" +
                "                    console.error('[Token Refresh] Error:', error);\n" +
                "                    console.warn('[Token Refresh] Please re-authenticate manually');\n" +
                "                });\n" +
                "            }\n" +
                "            \n" +
                "            function scheduleTokenRefresh(expiresIn, refreshToken, system, tokenUrl) {\n" +
                "                if (refreshTimer) {\n" +
                "                    clearTimeout(refreshTimer);\n" +
                "                }\n" +
                "                \n" +
                "                // Refresh 60 seconds before expiry (or halfway through if token lives less than 120s)\n" +
                "                var refreshDelay = Math.max(30, expiresIn - 60);\n" +
                "                var refreshTime = refreshDelay * 1000;\n" +
                "                \n" +
                "                console.log('[Token Refresh] Scheduled in', refreshDelay, 'seconds (token expires in', expiresIn, 'seconds)');\n" +
                "                \n" +
                "                refreshTimer = setTimeout(function() {\n" +
                "                    refreshAccessToken(system, refreshToken, tokenUrl);\n" +
                "                }, refreshTime);\n" +
                "            }\n" +
                "            \n" +
                "            return {\n" +
                "                statePlugins: {\n" +
                "                    auth: {\n" +
                "                        wrapActions: {\n" +
                "                            authorizeOauth2: (originalAction, system) => (payload) => {\n" +
                "                                originalAction(payload);\n" +
                "                                if (payload.auth) {\n" +
                "                                    lastAuthUrl = payload.auth.schema.get('authorizationUrl');\n" +
                "                                    \n" +
                "                                    // Wait for token to be stored then setup refresh\n" +
                "                                    setTimeout(function() {\n" +
                "                                        var auth = system.authSelectors.authorized();\n" +
                "                                        if (auth && auth.get) {\n" +
                "                                            var entandoAuth = auth.get('entando');\n" +
                "                                            if (entandoAuth && entandoAuth.get) {\n" +
                "                                                var tokenData = entandoAuth.get('token');\n" +
                "                                                if (tokenData && tokenData.get) {\n" +
                "                                                    var expiresIn = tokenData.get('expires_in');\n" +
                "                                                    var refreshToken = tokenData.get('refresh_token');\n" +
                "                                                    \n" +
                "                                                    console.log('[Token Refresh] Token received. Expires in:', expiresIn, 'seconds');\n" +
                "                                                    console.log('[Token Refresh] Refresh token present:', !!refreshToken);\n" +
                "                                                    \n" +
                "                                                    if (expiresIn && refreshToken && lastAuthUrl) {\n" +
                "                                                        // Extract token endpoint from authorization URL\n" +
                "                                                        var tokenUrl = lastAuthUrl.substring(0, lastAuthUrl.lastIndexOf('/')) + '/token';\n" +
                "                                                        console.log('[Token Refresh] Will use token endpoint:', tokenUrl);\n" +
                "                                                        scheduleTokenRefresh(expiresIn, refreshToken, system, tokenUrl);\n" +
                "                                                    } else {\n" +
                "                                                        console.warn('[Token Refresh] Cannot setup auto-refresh. Missing:', \n" +
                "                                                            !expiresIn ? 'expires_in' : '',\n" +
                "                                                            !refreshToken ? 'refresh_token' : '',\n" +
                "                                                            !lastAuthUrl ? 'authUrl' : '');\n" +
                "                                                    }\n" +
                "                                                }\n" +
                "                                            }\n" +
                "                                        }\n" +
                "                                    }, 500);\n" +
                "                                }\n" +
                "                            },\n" +
                "                            logout: (originalAction, system) => (payload) => {\n" +
                "                                // Clear refresh timer\n" +
                "                                if (refreshTimer) {\n" +
                "                                    console.log('[Token Refresh] Clearing refresh timer');\n" +
                "                                    clearTimeout(refreshTimer);\n" +
                "                                    refreshTimer = null;\n" +
                "                                }\n" +
                "                                \n" +
                "                                // Extract id_token from auth state before clearing\n" +
                "                                var token = null;\n" +
                "                                var auth = system.authSelectors.authorized();\n" +
                "                                if (auth && auth.get) {\n" +
                "                                    var entandoAuth = auth.get('entando');\n" +
                "                                    if (entandoAuth && entandoAuth.get) {\n" +
                "                                        var tokenData = entandoAuth.get('token');\n" +
                "                                        if (tokenData && tokenData.get) {\n" +
                "                                            token = tokenData.get('id_token') || tokenData.get('access_token');\n" +
                "                                        }\n" +
                "                                    }\n" +
                "                                }\n" +
                "                                \n" +
                "                                // Clear Swagger UI auth state\n" +
                "                                originalAction(payload);\n" +
                "                                \n" +
                "                                // Redirect to Keycloak logout endpoint\n" +
                "                                if (lastAuthUrl) {\n" +
                "                                    var logoutUrl = lastAuthUrl.substring(0, lastAuthUrl.lastIndexOf('/')) + '/logout';\n" +
                "                                    logoutUrl += '?post_logout_redirect_uri=' + encodeURIComponent('" + SWAGGER_UI_URL + "');\n" +
                "                                    if (token) {\n" +
                "                                        logoutUrl += '&id_token_hint=' + encodeURIComponent(token);\n" +
                "                                    }\n" +
                "                                    window.location.href = logoutUrl;\n" +
                "                                }\n" +
                "                            }\n" +
                "                        }\n" +
                "                    }\n" +
                "                }\n" +
                "            };\n" +
                "        }\n" +
                "        \n" +
                "        window.onload = function() {\n" +
                "            const params = new URLSearchParams(window.location.search);\n" +
                "            const specUrl = params.get('url') || '" + API_DOCS_URL + "';\n" +
                "            \n" +
                "            window.ui = SwaggerUIBundle({\n" +
                "                url: specUrl,\n" +
                "                dom_id: '#swagger-ui',\n" +
                "                deepLinking: true,\n" +
                "                presets: [\n" +
                "                    SwaggerUIBundle.presets.apis,\n" +
                "                    SwaggerUIStandalonePreset\n" +
                "                ],\n" +
                "                plugins: [\n" +
                "                    SwaggerUIBundle.plugins.DownloadUrl,\n" +
                "                    OAuthLogoutPlugin\n" +
                "                ],\n" +
                "                layout: \"StandaloneLayout\",\n" +
                "                oauth2RedirectUrl: '" + oauth2RedirectUrl + "',\n" +
                "                tagsSorter: 'alpha',\n" +
                "                operationsSorter: 'alpha',\n" +
                "                docExpansion: 'none',\n" +
                "                requestInterceptor: function(request) {\n" +
                "                    // Get current access token from auth state using 'this' context\n" +
                "                    try {\n" +
                "                        if (this.authSelectors && this.authSelectors.authorized) {\n" +
                "                            var auth = this.authSelectors.authorized();\n" +
                "                            if (auth && auth.get) {\n" +
                "                                var entandoAuth = auth.get('entando');\n" +
                "                                if (entandoAuth && entandoAuth.get) {\n" +
                "                                    var tokenData = entandoAuth.get('token');\n" +
                "                                    if (tokenData && tokenData.get) {\n" +
                "                                        var accessToken = tokenData.get('access_token');\n" +
                "                                        if (accessToken) {\n" +
                "                                            request.headers['Authorization'] = 'Bearer ' + accessToken;\n" +
                "                                        }\n" +
                "                                    }\n" +
                "                                }\n" +
                "                            }\n" +
                "                        }\n" +
                "                    } catch(e) {\n" +
                "                        console.warn('[Request Interceptor] Could not apply token:', e);\n" +
                "                    }\n" +
                "                    return request;\n" +
                "                }\n" +
                "            });\n" +
                "            \n" +
                "            window.ui.initOAuth({\n" +
                "                clientId: 'swagger',\n" +
                "                clientSecret: 'swaggerswagger',\n" +
                "                appName: 'Entando API',\n" +
                "                scopeSeparator: ' ',\n" +
                "                scopes: 'openid',\n" +
                "                additionalQueryStringParams: {},\n" +
                "                useBasicAuthenticationWithAccessCodeGrant: true,\n" +
                "                usePkceWithAuthorizationCodeGrant: false\n" +
                "            });\n" +
                "        };\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }
}