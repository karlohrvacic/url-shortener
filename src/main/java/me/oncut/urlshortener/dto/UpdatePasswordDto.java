package me.oncut.urlshortener.dto;

import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePasswordDto {

  @NotNull
  private String oldPassword;

  @NotNull
  private String newPassword;

}