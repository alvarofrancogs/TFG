package com.alvar.oasisclub.auth.service;

import com.alvar.oasisclub.auth.dto.AuthSessionResponse;
import com.alvar.oasisclub.auth.dto.LoginRequest;
import com.alvar.oasisclub.auth.dto.RegisterRequest;
import com.alvar.oasisclub.auth.entity.PasswordResetTokenEntity;
import com.alvar.oasisclub.auth.exception.EmailAlreadyRegisteredException;
import com.alvar.oasisclub.auth.exception.EmailNotFoundException;
import com.alvar.oasisclub.auth.exception.InvalidCredentialsException;
import com.alvar.oasisclub.auth.exception.PasswordResetTokenInvalidException;
import com.alvar.oasisclub.auth.mapper.AuthMapper;
import com.alvar.oasisclub.auth.repository.PasswordResetTokenRepository;
import com.alvar.oasisclub.auth.security.JwtService;
import com.alvar.oasisclub.clients.entity.ClientEntity;
import com.alvar.oasisclub.clients.service.ClientService;
import com.alvar.oasisclub.common.config.AppFrontendUrlProperties;
import com.alvar.oasisclub.common.email.EmailService;
import com.alvar.oasisclub.gym.service.GymService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AuthService {

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);

  
  private static final int RESET_TOKEN_VALIDITY_MINUTES = 30;

  private final ClientService clientService;
  private final AuthMapper authMapper;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;
  private final PasswordResetTokenRepository resetTokenRepository;
  private final EmailService emailService;
  private final AppFrontendUrlProperties appFrontendUrlProperties;
  private final GymService gymService;

  
  
  

  @Transactional(readOnly = true)
  public AuthSessionResponse login(LoginRequest request) {
    String email = request.getEmail().trim().toLowerCase();

    ClientEntity client = clientService.findByEmail(email);
    if (client == null) {
      throw new InvalidCredentialsException("Credenciales inválidas");
    }

    if (!passwordEncoder.matches(request.getPassword(), client.getPasswordHash())) {
      throw new InvalidCredentialsException("Credenciales inválidas");
    }

    String token = jwtService.generateToken(client.getId(), client.getEmail(), client.getRole());
    return authMapper.toResponse(client, token);
  }

  @Transactional
  public AuthSessionResponse register(RegisterRequest request) {
    String email = request.getEmail().trim().toLowerCase();
    String phone = ClientService.normalizePhone(request.getPhone());

    if (clientService.findByEmail(email) != null) {
      throw new EmailAlreadyRegisteredException("Ya existe una cuenta con ese email");
    }

    if (clientService.findByPhone(phone) != null) {
      throw new EmailAlreadyRegisteredException("Ya existe una cuenta con ese teléfono");
    }

    if (Period.between(request.getBirthDate(), LocalDate.now()).getYears() < 14) {
      throw new IllegalArgumentException("Debes tener al menos 14 años para registrarte");
    }

    ClientEntity client = ClientEntity.builder()
        .name(request.getName().trim())
        .email(email)
        .joinDate(LocalDate.now())
        .passwordHash(passwordEncoder.encode(request.getPassword()))
        .role("MEMBER")
        .phone(phone)   
        .birthDate(request.getBirthDate())
        .build();

    clientService.save(client);
    gymService.seedDefaultRoutine(client.getId());
    emailService.sendWelcomeEmail(client.getEmail(), client.getName());

    String token = jwtService.generateToken(client.getId(), client.getEmail(), client.getRole());
    return authMapper.toResponse(client, token);
  }

  
  
  

  
  @Transactional
  public void forgotPassword(String email) {
    String normalizedEmail = email.trim().toLowerCase();
    ClientEntity client = clientService.findByEmail(normalizedEmail);

    if (client == null) {
      throw new EmailNotFoundException("No existe ninguna cuenta con ese correo electrónico");
    }

    
    
    if ("ADMIN".equalsIgnoreCase(client.getRole())) {
      log.warn("[FORGOT_PASSWORD] Attempt to reset admin password from public endpoint — denied for client_id={}",
          client.getId());
      throw new EmailNotFoundException("No existe ninguna cuenta con ese correo electrónico");
    }

    
    
    LocalDateTime now = LocalDateTime.now();
    List<PasswordResetTokenEntity> previousTokens =
        resetTokenRepository.findByClientIdAndUsedFalseAndExpirationAfter(client.getId(), now);
    for (PasswordResetTokenEntity old : previousTokens) {
      old.setUsed(true);
    }
    
    resetTokenRepository.saveAll(previousTokens);

    
    String rawToken = UUID.randomUUID().toString();
    PasswordResetTokenEntity resetToken = PasswordResetTokenEntity.builder()
        .clientId(client.getId())
        .token(rawToken)
        .expiration(now.plusMinutes(RESET_TOKEN_VALIDITY_MINUTES))
        .used(false)
        .build();
    resetTokenRepository.save(resetToken);

    
    log.info("[FORGOT_PASSWORD] Reset token issued for client_id={}", client.getId());

    String resetLink = appFrontendUrlProperties.getFrontendUrl() + "/restablecer-clave?token=" + rawToken;
    
    emailService.sendPasswordResetEmail(client.getEmail(), resetLink);
  }

  
  
  

  
  @Transactional
  public void resetPassword(String token, String newPassword) {
    PasswordResetTokenEntity resetToken = resetTokenRepository.findByToken(token)
        .orElseThrow(() -> new PasswordResetTokenInvalidException("El enlace no es válido"));

    
    if (resetToken.isUsed()) {
      throw new PasswordResetTokenInvalidException("El enlace no es válido");
    }

    
    if (resetToken.getExpiration().isBefore(LocalDateTime.now())) {
      throw new PasswordResetTokenInvalidException("El enlace ha expirado. Solicita uno nuevo.");
    }

    ClientEntity client = clientService.getEntityById(resetToken.getClientId());
    client.setPasswordHash(passwordEncoder.encode(newPassword));
    client.setPasswordChangedAt(java.time.Instant.now());
    clientService.save(client);

    
    resetToken.setUsed(true);
    resetTokenRepository.save(resetToken);

    log.info("[RESET_PASSWORD] Password updated for client_id={}", client.getId());
  }
}