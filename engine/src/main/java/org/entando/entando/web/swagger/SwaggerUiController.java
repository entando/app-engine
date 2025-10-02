package org.entando.entando.web.swagger;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Controller to serve Swagger UI page
 */
@Controller
public class SwaggerUiController {

    @GetMapping(value = "/swagger-ui.html", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String swaggerUi(HttpServletRequest request) {
        String contextPath = request.getContextPath();

        String API_DOCS_URL = contextPath + "/api/webjars/swagger-ui/5.18.2";

        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Entando API Documentation</title>\n" +
                "    <link rel=\"stylesheet\" type=\"text/css\" href=\"" + API_DOCS_URL + "/swagger-ui.css\" />\n" +
                "    <link rel=\"icon\" type=\"image/png\" href=\"" + API_DOCS_URL + "/favicon-32x32.png\" sizes=\"32x32\" />\n" +
                "    <link rel=\"icon\" type=\"image/png\" href=\"" + API_DOCS_URL + "/favicon-16x16.png\" sizes=\"16x16\" />\n" +
                "    <style>\n" +
                "        html { box-sizing: border-box; overflow: -moz-scrollbars-vertical; overflow-y: scroll; }\n" +
                "        *, *:before, *:after { box-sizing: inherit; }\n" +
                "        body { margin: 0; padding: 0; }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div id=\"swagger-ui\"></div>\n" +
                "    <script src=\"" + API_DOCS_URL + "/swagger-ui-bundle.js\" charset=\"UTF-8\"></script>\n" +
                "    <script src=\"" + API_DOCS_URL + "/swagger-ui-standalone-preset.js\" charset=\"UTF-8\"></script>\n" +
                "    <script>\n" +
                "        window.onload = function() {\n" +
                "            const params = new URLSearchParams(window.location.search);\n" +
                "            const specUrl = params.get('url') || '" + contextPath + "/api/v3/api-docs';\n" +
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
                "                    SwaggerUIBundle.plugins.DownloadUrl\n" +
                "                ],\n" +
                "                layout: \"StandaloneLayout\"\n" +
                "            });\n" +
                "        };\n" +
                "    </script>\n" +
                "</body>\n" +
                "</html>";
    }
}