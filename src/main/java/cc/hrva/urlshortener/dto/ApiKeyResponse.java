package cc.hrva.urlshortener.dto;

import cc.hrva.urlshortener.model.ApiKey;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ApiKeyResponse(
        @Schema(description = "Unique identifier", example = "1")
        Long id,
        @Schema(description = "The API key value", example = "sk_abc123def456")
        String key,
        @Schema(description = "Maximum number of API calls allowed", example = "1000")
        Long apiCallsLimit,
        @Schema(description = "Number of API calls used", example = "42")
        Long apiCallsUsed,
        @Schema(description = "Creation date and time", example = "2026-05-16T10:00:00")
        LocalDateTime createDate,
        @Schema(description = "Expiration date and time", example = "2026-12-31T23:59:59")
        LocalDateTime expirationDate,
        @Schema(description = "Whether the API key is active", example = "true")
        boolean active,
        @Schema(description = "Reason for the current state: ACTIVE, LIMIT_REACHED, EXPIRED, REVOKED", example = "ACTIVE")
        String status,
        @Schema(description = "Email of the key owner", example = "user@example.com")
        String ownerEmail) {

    public static ApiKeyResponse from(final ApiKey apiKey) {
        return new ApiKeyResponse(
                apiKey.getId(),
                apiKey.getKey(),
                apiKey.getApiCallsLimit(),
                apiKey.getApiCallsUsed(),
                apiKey.getCreateDate(),
                apiKey.getExpirationDate(),
                apiKey.isActive(),
                resolveStatus(apiKey),
                apiKey.getOwner() != null ? apiKey.getOwner().getEmail() : null);
    }

    private static String resolveStatus(final ApiKey apiKey) {
        if (apiKey.isActive()) {
            return "ACTIVE";
        }
        if (apiKey.getApiCallsLimit() != null && apiKey.getApiCallsUsed() != null
                && apiKey.getApiCallsUsed() >= apiKey.getApiCallsLimit()) {
            return "LIMIT_REACHED";
        }
        if (apiKey.getExpirationDate() != null && apiKey.getExpirationDate().isBefore(LocalDateTime.now())) {
            return "EXPIRED";
        }
        return "REVOKED";
    }

}
