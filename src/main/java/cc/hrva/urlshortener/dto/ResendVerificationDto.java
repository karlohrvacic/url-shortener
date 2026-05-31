package cc.hrva.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResendVerificationDto {

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Schema(description = "Email address to resend the verification link to", example = "user@example.com")
    private String email;

}
