package cc.hrva.urlshortener.controller;

import io.swagger.v3.oas.annotations.Operation;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("api/changelog")
public class ChangelogController {

    @Operation(summary = "Get changelog", description = "Return the changelog JSON with all releases, features, and fixes.")
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getChangelog() {
        try {
            final var resource = new ClassPathResource("changelog.json");
            final var json = resource.getContentAsString(StandardCharsets.UTF_8);
            return ResponseEntity.ok(json);
        } catch (final IOException e) {
            log.error("Failed to read changelog.json", e);
            return ResponseEntity.internalServerError().body("{\"error\": \"Changelog not found\"}");
        }
    }

}
