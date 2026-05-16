package cc.hrva.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record LinkPreviewResponse(
        @Schema(description = "The original URL", example = "https://example.com/article")
        String url,
        @Schema(description = "Page title", example = "Example Article Title")
        String title,
        @Schema(description = "Page description", example = "An example article about URL shorteners")
        String description,
        @Schema(description = "Open Graph image URL", example = "https://example.com/og-image.jpg")
        String imageUrl) {

}
