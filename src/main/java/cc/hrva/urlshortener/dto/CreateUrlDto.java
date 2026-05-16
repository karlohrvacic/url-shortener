package cc.hrva.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class CreateUrlDto {

  @NotBlank(message = "Long URL is required")
  @Schema(description = "The original long URL to shorten", example = "https://example.com/very-long-url")
  private String longUrl;
  @Schema(description = "Custom short URL alias (optional)", example = "my-custom-alias")
  private String shortUrl;
  @Schema(description = "Maximum number of visits allowed", example = "100")
  private Long visitLimit;
  @Schema(description = "Expiration date and time of the URL", example = "2026-12-31T23:59:59")
  private LocalDateTime expirationDate;

}
