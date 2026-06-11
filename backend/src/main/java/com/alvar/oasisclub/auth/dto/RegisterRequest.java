package com.alvar.oasisclub.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

  @NotBlank
  @Size(max = 120, message = "El nombre no puede superar 120 caracteres")
  private String name;

  @Email(message = "El correo electrónico no tiene un formato válido")
  @NotBlank
  @Size(max = 180, message = "El correo no puede superar 180 caracteres")
  private String email;

  @NotBlank
  @Size(min = 8, max = 128, message = "La contraseña debe tener entre 8 y 128 caracteres")
  private String password;

  @NotBlank
  @Pattern(
      regexp = "^[+]?[0-9\\s\\-().]{7,20}$",
      message = "El teléfono no tiene un formato válido"
  )
  private String phone;

  @NotNull
  @Past(message = "La fecha de nacimiento debe ser una fecha pasada")
  private LocalDate birthDate;
}
