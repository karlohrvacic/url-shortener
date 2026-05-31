package cc.hrva.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record TwoFactorEnableResponse(
        @Schema(description = "One-time recovery codes, shown only once. Store them safely.")
        List<String> recoveryCodes) {
}
