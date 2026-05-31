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
public class VerifyEmailDto {

    @NotBlank(message = "Token is required")
    @Schema(description = "Email verification token", example = "550e8400-e29b-41d4-a716-446655440000")
    private String token;

}
