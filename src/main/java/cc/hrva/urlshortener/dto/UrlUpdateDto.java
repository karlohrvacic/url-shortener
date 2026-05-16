package cc.hrva.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class UrlUpdateDto {

    @NotNull(message = "ID is required")
    @Schema(description = "Unique identifier of the URL to update", example = "1")
    private Long id;
    @Schema(description = "Maximum number of visits allowed", example = "200")
    private Long visitLimit;
    @Schema(description = "Expiration date and time", example = "2026-12-31T23:59:59")
    private LocalDateTime expirationDate;

}
