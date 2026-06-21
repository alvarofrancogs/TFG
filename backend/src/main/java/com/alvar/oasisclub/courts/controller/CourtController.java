package com.alvar.oasisclub.courts.controller;

import com.alvar.oasisclub.auth.security.AccessControlService;
import com.alvar.oasisclub.courts.dto.CourtResponse;
import com.alvar.oasisclub.courts.dto.CreateCourtRequest;
import com.alvar.oasisclub.courts.service.CourtService;
import com.alvar.oasisclub.reservations.entity.SportType;
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
@RequestMapping("/api/v1/courts")
@AllArgsConstructor
@Tag(name = "Pistas", description = "Consulta, creación y eliminación de pistas deportivas del club")
public class CourtController {

    private final CourtService courtService;
    private final AccessControlService accessControl;

    @GetMapping
    @Operation(
        summary = "Listar pistas",
        description = "Devuelve la lista de pistas disponibles. Los administradores ven todas las pistas (activas e inactivas); los clientes solo ven las pistas activas. Se puede filtrar por tipo de deporte."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de pistas obtenida correctamente")
    })
    public List<CourtResponse> getCourts(
        @Parameter(description = "Filtrar por tipo de deporte (PADEL, TENNIS, etc.)") @RequestParam(required = false) SportType sport,
        Authentication authentication
    ) {
        if (accessControl.isAdmin(authentication)) {
            return courtService.getAllCourts(sport);
        }
        return courtService.getActiveCourts(sport);
    }

    @PostMapping
    @Operation(
        summary = "Crear pista",
        description = "Registra una nueva pista deportiva en el sistema. Solo accesible por administradores."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Pista creada correctamente"),
        @ApiResponse(responseCode = "400", description = "Datos de la pista inválidos"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Solo los administradores pueden crear pistas")
    })
    public ResponseEntity<CourtResponse> createCourt(
        @Valid @RequestBody CreateCourtRequest request,
        Authentication authentication
    ) {
        accessControl.requireAdmin(authentication);
        return ResponseEntity.ok(courtService.createCourt(request));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Eliminar pista",
        description = "Elimina una pista del sistema. Solo accesible por administradores."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Pista eliminada correctamente"),
        @ApiResponse(responseCode = "401", description = "No autenticado"),
        @ApiResponse(responseCode = "403", description = "Solo los administradores pueden eliminar pistas"),
        @ApiResponse(responseCode = "404", description = "Pista no encontrada")
    })
    public ResponseEntity<Void> deleteCourt(
        @Parameter(description = "ID UUID de la pista") @PathVariable UUID id,
        Authentication authentication
    ) {
        accessControl.requireAdmin(authentication);
        courtService.deleteCourt(id);
        return ResponseEntity.noContent().build();
    }
}
