package cc.hrva.urlshortener.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAccountDto {

    @Schema(description = "Current password, required for local (email/password) accounts", example = "myPassword123")
    private String password;

}
