package com.alvar.oasisclub.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

  @Email(message = "El correo electrónico no tiene un formato válido")
  @NotBlank
  @Size(max = 180, message = "El correo no puede superar 180 caracteres")
  private String email;

  @NotBlank
  @Size(min = 8, max = 128, message = "La contraseña debe tener minimo 8 caracteres")
  private String password;
}
