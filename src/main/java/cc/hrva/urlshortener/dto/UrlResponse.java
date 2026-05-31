package cc.hrva.urlshortener.dto;

import cc.hrva.urlshortener.model.Url;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Set;

public record UrlResponse(
        @Schema(description = "Unique identifier", example = "1")
        Long id,
        @Schema(description = "The original long URL", example = "https://example.com/very-long-url")
        String longUrl,
        @Schema(description = "Short URL alias", example = "abc123")
        String shortUrl,
        @Schema(description = "Creation date and time", example = "2026-05-16T10:00:00")
        LocalDateTime createDate,
        @Schema(description = "Expiration date and time", example = "2026-12-31T23:59:59")
        LocalDateTime expirationDate,
        @Schema(description = "Number of visits", example = "42")
        Long visits,
        @Schema(description = "Maximum number of visits allowed", example = "100")
        Long visitLimit,
        @Schema(description = "Whether the URL is active", example = "true")
        boolean active,
        @Schema(description = "Reason for the current state: ACTIVE, LIMIT_REACHED, EXPIRED, BLOCKED, DEACTIVATED", example = "ACTIVE")
        String status,
        @Schema(description = "Tags assigned to the URL", example = "[\"work\"]")
        Set<String> tags,
        @Schema(description = "Email of the URL owner", example = "user@example.com")
        String ownerEmail) {

    public static UrlResponse from(final Url url) {
        return new UrlResponse(
                url.getId(),
                url.getLongUrl(),
                url.getShortUrl(),
                url.getCreateDate(),
                url.getExpirationDate(),
                url.getVisits(),
                url.getVisitLimit(),
                url.isActive(),
                resolveStatus(url),
                url.getTags() != null ? url.getTags() : java.util.Set.of(),
                url.getOwner() != null ? url.getOwner().getEmail() : null);
    }

    private static String resolveStatus(final Url url) {
        if (url.isActive()) {
            return "ACTIVE";
        }
        if (url.getThreatType() != null) {
            return "BLOCKED";
        }
        if (url.getVisitLimit() != null && url.getVisits() != null
                && url.getVisits() >= url.getVisitLimit()) {
            return "LIMIT_REACHED";
        }
        if (url.getExpirationDate() != null && url.getExpirationDate().isBefore(LocalDateTime.now())) {
            return "EXPIRED";
        }
        return "DEACTIVATED";
    }

}
