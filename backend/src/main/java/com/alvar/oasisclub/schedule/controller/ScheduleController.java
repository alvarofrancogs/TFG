package com.alvar.oasisclub.schedule.controller;

import com.alvar.oasisclub.auth.security.AccessControlService;
import com.alvar.oasisclub.schedule.service.ScheduleSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/schedule")
@AllArgsConstructor
@Tag(name = "Horarios", description = "Gestión de franjas horarias disponibles para reservas")
public class ScheduleController {

  private final ScheduleSlotService scheduleSlotService;
  private final AccessControlService accessControl;

  @GetMapping
  @Operation(
      summary = "Listar franjas horarias",
      description = "Devuelve todas las franjas horarias disponibles configuradas en el sistema (por ejemplo: '09:00', '10:30', '12:00')."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Lista de franjas horarias obtenida correctamente")
  })
  public List<String> getSlots() {
    return scheduleSlotService.getAllSlots();
  }

  @PostMapping
  @Operation(
      summary = "Añadir franja horaria",
      description = "Crea una nueva franja horaria disponible para las reservas. Solo accesible por administradores. Enviar el campo 'time' con el formato 'HH:mm'."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "Franja horaria creada correctamente"),
      @ApiResponse(responseCode = "400", description = "El campo 'time' es obligatorio o tiene formato inválido"),
      @ApiResponse(responseCode = "401", description = "No autenticado"),
      @ApiResponse(responseCode = "403", description = "Solo los administradores pueden añadir franjas horarias")
  })
  public ResponseEntity<Map<String, String>> addSlot(
      @RequestBody Map<String, String> body,
      Authentication authentication
  ) {
    accessControl.requireAdmin(authentication);
    String time = body.get("time");
    if (time == null || time.isBlank()) {
      throw new IllegalArgumentException("El campo 'time' es obligatorio");
    }
    String created = scheduleSlotService.addSlot(time);
    return ResponseEntity.ok(Map.of("time", created));
  }

  @DeleteMapping("/{time}")
  @Operation(
      summary = "Eliminar franja horaria",
      description = "Elimina una franja horaria existente del sistema. Solo accesible por administradores. El valor 'time' debe estar en formato 'HH:mm'."
  )
  @ApiResponses({
      @ApiResponse(responseCode = "204", description = "Franja horaria eliminada correctamente"),
      @ApiResponse(responseCode = "401", description = "No autenticado"),
      @ApiResponse(responseCode = "403", description = "Solo los administradores pueden eliminar franjas horarias"),
      @ApiResponse(responseCode = "404", description = "Franja horaria no encontrada")
  })
  public ResponseEntity<Void> removeSlot(
      @Parameter(description = "Hora en formato HH:mm, por ejemplo: '09:00'") @PathVariable String time,
      Authentication authentication
  ) {
    accessControl.requireAdmin(authentication);
    scheduleSlotService.removeSlot(time);
    return ResponseEntity.noContent().build();
  }
}
