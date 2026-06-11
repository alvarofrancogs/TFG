package com.alvar.oasisclub.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

  @NotBlank
  private String token;

  @NotBlank
  @Size(min = 8,   message = "La contraseña debe tener al menos 8 caracteres")
  @Size(max = 128, message = "La contraseña no puede superar 128 caracteres")
  private String newPassword;
}
