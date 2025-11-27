package com.academic.user;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SwaggerController {

    @GetMapping(value = "/swagger-ui/index.html", produces = MediaType.TEXT_HTML_VALUE)
    public String swaggerUi() {
        return "<!doctype html>\n"
                + "<html>\n"
                + "<head>\n"
                + "  <meta charset=\"utf-8\">\n"
                + "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
                + "  <title>Swagger UI</title>\n"
                + "  <link rel=\"stylesheet\" href=\"https://unpkg.com/swagger-ui-dist@4/swagger-ui.css\"/>\n"
                + "</head>\n"
                + "<body>\n"
                + "  <div id=\"swagger-ui\"></div>\n"
                + "  <script src=\"https://unpkg.com/swagger-ui-dist@4/swagger-ui-bundle.js\"></script>\n"
                + "  <script>\n"
                + "    const ui = SwaggerUIBundle({\n"
                + "      url: '/v3/api-docs',\n"
                + "      dom_id: '#swagger-ui',\n"
                + "      presets: [SwaggerUIBundle.presets.apis],\n"
                + "    });\n"
                + "  </script>\n"
                + "</body>\n"
                + "</html>\n";
    }
}
