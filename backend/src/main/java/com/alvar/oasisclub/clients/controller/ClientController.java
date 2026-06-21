package com.alvar.oasisclub.clients.controller;

import com.alvar.oasisclub.auth.security.AccessControlService;
import com.alvar.oasisclub.clients.dto.ClientResponse;
import com.alvar.oasisclub.clients.dto.CreateClientRequest;
import com.alvar.oasisclub.clients.service.ClientService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clients")
@AllArgsConstructor
@Tag(name = "Clientes", description = "Gestión de socios del club (solo administradores)")
public class ClientController {

  private final ClientService clientService;
  private final AccessControlService accessControl;

  @GetMapping
  @Operation(
      summary = "Listar clientes",
      description = "Devuelve la lista de todos los clientes registrados. Permite filtrar por nombre o email mediante el parámetro 'query' y limitar el número de resultados. Solo accesible por administradores."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Lista de clientes obtenida correctamente"),
      @ApiResponse(responseCode = "401", description = "No autenticado"),
      @ApiResponse(responseCode = "403", description = "Solo los administradores pueden listar clientes")
  })
  public List<ClientResponse> getClients(
      @Parameter(description = "Texto para filtrar por nombre o email") @RequestParam(required = false) String query,
      @Parameter(description = "Número máximo de resultados a devolver") @RequestParam(required = false) Integer limit,
      Authentication authentication
  ) {
    accessControl.requireAdmin(authentication);
    return clientService.getClients(query, limit);
  }

  @PostMapping
  @Operation(
      summary = "Crear cliente",
      description = "Registra un nuevo cliente en el sistema. Solo accesible por administradores."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Cliente creado correctamente"),
      @ApiResponse(responseCode = "400", description = "Datos del cliente inválidos o email ya en uso"),
      @ApiResponse(responseCode = "401", description = "No autenticado"),
      @ApiResponse(responseCode = "403", description = "Solo los administradores pueden crear clientes")
  })
  public ResponseEntity<ClientResponse> createClient(
      @Valid @RequestBody CreateClientRequest request,
      Authentication authentication
  ) {
    accessControl.requireAdmin(authentication);
    return ResponseEntity.ok(clientService.createClient(request));
  }

  @DeleteMapping("/{id}")
  @Operation(
      summary = "Eliminar cliente",
      description = "Elimina permanentemente un cliente del sistema. Esta acción también elimina todas sus reservas y datos asociados. Solo accesible por administradores."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Cliente eliminado correctamente"),
      @ApiResponse(responseCode = "401", description = "No autenticado"),
      @ApiResponse(responseCode = "403", description = "Solo los administradores pueden eliminar clientes"),
      @ApiResponse(responseCode = "404", description = "Cliente no encontrado")
  })
  public ResponseEntity<Void> deleteClient(
      @Parameter(description = "ID UUID del cliente") @PathVariable UUID id,
      Authentication authentication
  ) {
    accessControl.requireAdmin(authentication);
    clientService.deleteClient(id);
    return ResponseEntity.noContent().build();
  }
}
