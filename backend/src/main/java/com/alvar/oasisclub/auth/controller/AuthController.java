package com.alvar.oasisclub.auth.controller;

import com.alvar.oasisclub.auth.dto.AuthSessionResponse;
import com.alvar.oasisclub.auth.dto.ForgotPasswordRequest;
import com.alvar.oasisclub.auth.dto.LoginRequest;
import com.alvar.oasisclub.auth.dto.RegisterRequest;
import com.alvar.oasisclub.auth.dto.ResetPasswordRequest;
import com.alvar.oasisclub.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@AllArgsConstructor
@Tag(name = "Autenticación", description = "Registro, login y gestión de contraseñas")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  @Operation(summary = "Iniciar sesión", description = "Autentica al usuario con email y contraseña y devuelve un token JWT.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Login correcto, se devuelve el token JWT"),
      @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
      @ApiResponse(responseCode = "401", description = "Credenciales incorrectas")
  })
  public ResponseEntity<AuthSessionResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(authService.login(request));
  }

  @PostMapping("/forgot-password")
  @Operation(summary = "Solicitar restablecimiento de contraseña", description = "Envía un email con un enlace para restablecer la contraseña al correo proporcionado.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Email enviado correctamente"),
      @ApiResponse(responseCode = "400", description = "Email inválido o no encontrado")
  })
  public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
    authService.forgotPassword(request.getEmail());
    return ResponseEntity.ok(Map.of(
        "message", "Te hemos enviado un enlace para restablecer tu contraseña"
    ));
  }

  @PostMapping("/reset-password")
  @Operation(summary = "Restablecer contraseña", description = "Cambia la contraseña del usuario usando el token enviado por email.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Contraseña restablecida correctamente"),
      @ApiResponse(responseCode = "400", description = "Token inválido o expirado")
  })
  public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
    authService.resetPassword(request.getToken(), request.getNewPassword());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/register")
  @Operation(summary = "Registrar nuevo usuario", description = "Crea una nueva cuenta de usuario en el sistema y devuelve un token JWT de sesión.")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Usuario registrado correctamente"),
      @ApiResponse(responseCode = "400", description = "Datos de registro inválidos o email ya registrado")
  })
  public ResponseEntity<AuthSessionResponse> register(@Valid @RequestBody RegisterRequest request) {
    return ResponseEntity.ok(authService.register(request));
  }
}
