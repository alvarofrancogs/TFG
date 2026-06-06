package com.alvar.oasisclub.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactRequest(
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    String nombre,

    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(max = 100)
    String apellidos,

    @NotBlank(message = "El correo es obligatorio")
    @Email(message = "El formato del correo no es válido")
    String email,

    @NotBlank(message = "El asunto es obligatorio")
    @Size(max = 150, message = "El asunto es demasiado largo")
    String asunto,

    @NotBlank(message = "El mensaje es obligatorio")
    @Size(max = 2000)
    String mensaje
) {}
