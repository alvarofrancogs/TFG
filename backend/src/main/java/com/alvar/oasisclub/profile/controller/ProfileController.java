package com.alvar.oasisclub.profile.controller;

import com.alvar.oasisclub.auth.security.AccessControlService;
import com.alvar.oasisclub.profile.dto.ProfileResponse;
import com.alvar.oasisclub.profile.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
@AllArgsConstructor
@Tag(name = "Perfil", description = "Consulta del perfil y datos personales de un cliente")
public class ProfileController {

  private final ProfileService profileService;
  private final AccessControlService accessControl;

  @GetMapping("/{clientId}")
  @Operation(
      summary = "Obtener perfil del cliente",
      description = "Devuelve la información del perfil del cliente: nombre, email, foto y datos personales. Solo accesible por el propio cliente o un administrador."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Perfil obtenido correctamente"),
      @ApiResponse(responseCode = "401", description = "No autenticado"),
      @ApiResponse(responseCode = "403", description = "Sin permisos para acceder a este perfil"),
      @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
  })
  public ProfileResponse getProfile(
      @Parameter(description = "ID UUID del cliente") @PathVariable UUID clientId,
      Authentication authentication
  ) {
    accessControl.requireAdminOrOwner(authentication, clientId);
    return profileService.getProfile(clientId);
  }
}
