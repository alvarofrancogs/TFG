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
  @Size(min = 8, max = 128, message = "La contraseña debe tener entre 8 y 128 caracteres")
  private String newPassword;
}
