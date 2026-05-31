package cc.hrva.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorCodeDto {

    @NotBlank(message = "Code is required")
    @Schema(description = "TOTP code from the authenticator app, or a recovery code", example = "123456")
    private String code;

}
