package cc.hrva.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class UserUpdateDto {

    @NotNull(message = "ID is required")
    @Schema(description = "Unique identifier of the user to update", example = "1")
    private Long id;
    @Schema(description = "New email address", example = "updated@example.com")
    private String email;
    @Schema(description = "Number of API key slots", example = "10")
    private Long apiKeySlots;
    @Schema(description = "Whether the user account is active", example = "true")
    private Boolean active;
}
