package cc.hrva.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TwoFactorSetupResponse(
        @Schema(description = "Base32 TOTP secret to enter manually into an authenticator app")
        String secret,
        @Schema(description = "otpauth:// URI to render as a QR code")
        String otpauthUri) {
}
