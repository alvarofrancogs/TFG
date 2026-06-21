package com.alvar.oasisclub.gym.controller;

import com.alvar.oasisclub.auth.security.AccessControlService;
import com.alvar.oasisclub.gym.dto.RoutineDayDto;
import com.alvar.oasisclub.gym.dto.UpdateRoutineRequest;
import com.alvar.oasisclub.gym.service.GymService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/gym/routines")
@AllArgsConstructor
@Tag(name = "Gimnasio", description = "Consulta y actualización de las rutinas de entrenamiento de los clientes")
public class GymController {

  private final GymService gymService;
  private final AccessControlService accessControl;

  @GetMapping("/{clientId}")
  @Operation(
      summary = "Obtener rutina del cliente",
      description = "Devuelve la rutina de entrenamiento semanal del cliente indicado. Solo accesible por el propio cliente o un administrador."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Rutina obtenida correctamente"),
      @ApiResponse(responseCode = "401", description = "No autenticado"),
      @ApiResponse(responseCode = "403", description = "Sin permisos para acceder a esta rutina"),
      @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
  })
  public List<RoutineDayDto> getRoutine(
      @Parameter(description = "ID UUID del cliente") @PathVariable UUID clientId,
      Authentication authentication
  ) {
    accessControl.requireAdminOrOwner(authentication, clientId);
    return gymService.getRoutine(clientId);
  }

  @PutMapping("/{clientId}")
  @Operation(
      summary = "Actualizar rutina del cliente",
      description = "Reemplaza la rutina de entrenamiento del cliente con los datos enviados en el cuerpo. Solo accesible por el propio cliente o un administrador."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Rutina actualizada correctamente"),
      @ApiResponse(responseCode = "400", description = "Datos de la rutina inválidos"),
      @ApiResponse(responseCode = "401", description = "No autenticado"),
      @ApiResponse(responseCode = "403", description = "Sin permisos para modificar esta rutina"),
      @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
  })
  public ResponseEntity<List<RoutineDayDto>> updateRoutine(
      @Parameter(description = "ID UUID del cliente") @PathVariable UUID clientId,
      @Valid @RequestBody UpdateRoutineRequest request,
      Authentication authentication
  ) {
    accessControl.requireAdminOrOwner(authentication, clientId);
    return ResponseEntity.ok(gymService.updateRoutine(clientId, request));
  }
}
