package cc.hrva.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UnlockResponse(
        @Schema(description = "The resolved destination URL", example = "https://example.com/very-long-url")
        String longUrl) {
}
