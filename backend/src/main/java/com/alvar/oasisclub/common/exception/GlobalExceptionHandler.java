package com.alvar.oasisclub.common.exception;

import com.alvar.oasisclub.auth.exception.EmailAlreadyRegisteredException;
import com.alvar.oasisclub.auth.exception.InvalidCredentialsException;
import com.alvar.oasisclub.auth.exception.PasswordResetTokenInvalidException;
import com.alvar.oasisclub.clients.exception.ClientEmailAlreadyExistsException;
import com.alvar.oasisclub.clients.exception.ClientNotFoundException;
import com.alvar.oasisclub.courts.exception.CourtNotFoundException;
import com.alvar.oasisclub.events.exception.EventNotFoundException;
import com.alvar.oasisclub.reservations.exception.ReservationNotFoundException;
import com.stripe.exception.StripeException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(
      MethodArgumentNotValidException ex,
      HttpServletRequest request
  ) {
    List<String> details = ex.getBindingResult().getFieldErrors().stream()
        .map(this::formatFieldError)
        .collect(Collectors.toList());

    String summary = details.size() == 1
        ? details.get(0)
        : "Hay " + details.size() + " errores en el formulario";

    ApiErrorResponse body = ApiErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value())
        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
        .message(summary)
        .path(request.getRequestURI())
        .details(details)
        .build();

    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConstraint(
      ConstraintViolationException ex,
      HttpServletRequest request
  ) {
    List<String> details = ex.getConstraintViolations().stream()
        .map(v -> {
          String field = v.getPropertyPath().toString();
          if (field.contains(".")) field = field.substring(field.lastIndexOf('.') + 1);
          return friendlyFieldName(field) + ": " + translateConstraintMessage(v.getMessage());
        })
        .collect(Collectors.toList());

    String summary = details.size() == 1
        ? details.get(0)
        : "Hay " + details.size() + " errores de validación";

    ApiErrorResponse body = ApiErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value())
        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
        .message(summary)
        .path(request.getRequestURI())
        .details(details)
        .build();

    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler({
      ClientNotFoundException.class,
      CourtNotFoundException.class,
      ReservationNotFoundException.class,
      EventNotFoundException.class
  })
  public ResponseEntity<ApiErrorResponse> handleNotFound(
      RuntimeException ex,
      HttpServletRequest request
  ) {
    ApiErrorResponse body = ApiErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.NOT_FOUND.value())
        .error(HttpStatus.NOT_FOUND.getReasonPhrase())
        .message(ex.getMessage())
        .path(request.getRequestURI())
        .details(List.of())
        .build();

    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
  }

  @ExceptionHandler({
      ClientEmailAlreadyExistsException.class,
      EmailAlreadyRegisteredException.class,
      PasswordResetTokenInvalidException.class
  })
  public ResponseEntity<ApiErrorResponse> handleCustomBadRequest(
      RuntimeException ex,
      HttpServletRequest request
  ) {
    ApiErrorResponse body = ApiErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value())
        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
        .message(ex.getMessage())
        .path(request.getRequestURI())
        .details(List.of())
        .build();

    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(InvalidCredentialsException.class)
  public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(
      InvalidCredentialsException ex,
      HttpServletRequest request
  ) {
    ApiErrorResponse body = ApiErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.UNAUTHORIZED.value())
        .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
        .message(ex.getMessage())
        .path(request.getRequestURI())
        .details(List.of())
        .build();

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
      IllegalArgumentException ex,
      HttpServletRequest request
  ) {
    ApiErrorResponse body = ApiErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.BAD_REQUEST.value())
        .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
        .message(ex.getMessage())
        .path(request.getRequestURI())
        .details(List.of())
        .build();

    return ResponseEntity.badRequest().body(body);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ApiErrorResponse> handleIllegalState(
      IllegalStateException ex,
      HttpServletRequest request
  ) {
    ApiErrorResponse body = ApiErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.BAD_GATEWAY.value())
        .error(HttpStatus.BAD_GATEWAY.getReasonPhrase())
        .message(ex.getMessage())
        .path(request.getRequestURI())
        .details(List.of())
        .build();

    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<ApiErrorResponse> handleAccessDenied(
      AccessDeniedException ex,
      HttpServletRequest request
  ) {
    ApiErrorResponse body = ApiErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.FORBIDDEN.value())
        .error(HttpStatus.FORBIDDEN.getReasonPhrase())
        .message("Acceso denegado")
        .path(request.getRequestURI())
        .details(List.of())
        .build();

    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
  }

  @ExceptionHandler(org.springframework.security.authentication.BadCredentialsException.class)
  public ResponseEntity<ApiErrorResponse> handleBadCredentials(
      org.springframework.security.authentication.BadCredentialsException ex,
      HttpServletRequest request
  ) {
    ApiErrorResponse body = ApiErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.UNAUTHORIZED.value())
        .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
        .message("Credenciales inválidas")
        .path(request.getRequestURI())
        .details(List.of())
        .build();

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
  }

  
  @ExceptionHandler({StripeOperationFailedException.class, StripeException.class})
  public ResponseEntity<ApiErrorResponse> handleStripeOperationFailed(
      Exception ex,
      HttpServletRequest request
  ) {
    ApiErrorResponse body = ApiErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.BAD_GATEWAY.value())
        .error(HttpStatus.BAD_GATEWAY.getReasonPhrase())
        .message(ex.getMessage())
        .path(request.getRequestURI())
        .details(List.of())
        .build();

    return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
  }

  
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleGeneric(
      Exception ex,
      HttpServletRequest request
  ) {
    
    log.error("Unexpected error processing {} {}", request.getMethod(), request.getRequestURI(), ex);

    
    ApiErrorResponse body = ApiErrorResponse.builder()
        .timestamp(LocalDateTime.now())
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
        .message("Se ha producido un error inesperado. Por favor, inténtalo de nuevo más tarde.")
        .path(request.getRequestURI())
        .details(List.of())
        .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
  }

  private String formatFieldError(FieldError error) {
    String friendly = friendlyFieldName(error.getField());
    String code = error.getCode() != null ? error.getCode().toLowerCase() : "";
    Object[] args = error.getArguments();

    return switch (code) {
      case "notblank", "notempty" -> friendly + " es obligatorio";
      case "notnull"              -> friendly + " es obligatorio";
      case "min" -> {
        String min = args != null && args.length > 1
            ? String.valueOf(((Number) args[1]).longValue()) : "?";
        yield friendly + " debe ser mayor o igual a " + min;
      }
      case "max" -> {
        String max = args != null && args.length > 1
            ? String.valueOf(((Number) args[1]).longValue()) : "?";
        yield friendly + " debe ser menor o igual a " + max;
      }
      case "size" -> {
        String mn = args != null && args.length > 2 ? String.valueOf(args[2]) : "?";
        String mx = args != null && args.length > 1 ? String.valueOf(args[1]) : "?";
        yield friendly + " debe tener entre " + mn + " y " + mx + " caracteres";
      }
      case "email"           -> friendly + " debe ser un correo electrónico válido";
      case "pattern"         -> friendly + ": " + translateConstraintMessage(error.getDefaultMessage());
      case "past"            -> friendly + " debe ser una fecha pasada";
      case "future",
           "futureorpresent" -> friendly + " debe ser una fecha futura o presente";
      case "positive",
           "positiveorzero"  -> friendly + " debe ser un número positivo";
      default -> friendly + ": " + translateConstraintMessage(error.getDefaultMessage());
    };
  }

  private String friendlyFieldName(String field) {
    return switch (field) {
      case "title"       -> "El título";
      case "name"        -> "El nombre";
      case "email"       -> "El correo electrónico";
      case "password"    -> "La contraseña";
      case "newPassword" -> "La nueva contraseña";
      case "phone"       -> "El teléfono";
      case "description" -> "La descripción";
      case "eventDate"   -> "La fecha del evento";
      case "startTime"   -> "La hora de inicio";
      case "endTime"     -> "La hora de fin";
      case "maxCapacity" -> "La capacidad máxima";
      case "category"    -> "La categoría";
      case "sport"       -> "El deporte";
      case "courtId"     -> "La pista";
      case "date"        -> "La fecha";
      case "time"        -> "La hora";
      case "userName"    -> "El nombre de usuario";
      case "token"       -> "El token";
      case "sessionId"   -> "La sesión";
      case "birthDate"   -> "La fecha de nacimiento";
      default            -> "El campo '" + field + "'";
    };
  }

  private String translateConstraintMessage(String msg) {
    if (msg == null) return "valor inválido";
    return switch (msg.trim()) {
      case "must not be blank"                    -> "es obligatorio";
      case "must not be null"                     -> "es obligatorio";
      case "must be a well-formed email address"  -> "debe ser un correo electrónico válido";
      case "must be greater than or equal to 1"  -> "debe ser mayor o igual a 1";
      case "must be positive"                     -> "debe ser un número positivo";
      case "must be positive or zero"             -> "debe ser un número positivo o cero";
      default -> msg;
    };
  }
}
