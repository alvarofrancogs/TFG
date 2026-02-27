package com.alvar.oasisclub.common.exception;

import com.alvar.oasisclub.auth.exception.EmailAlreadyRegisteredException;
import com.alvar.oasisclub.auth.exception.InvalidCredentialsException;
import com.alvar.oasisclub.auth.exception.PasswordResetTokenInvalidException;
import com.alvar.oasisclub.clients.exception.ClientEmailAlreadyExistsException;
import com.alvar.oasisclub.clients.exception.ClientNotFoundException;
import com.alvar.oasisclub.reservations.exception.ReservationNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
}