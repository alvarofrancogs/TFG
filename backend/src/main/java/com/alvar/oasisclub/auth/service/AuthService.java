package com.alvar.oasisclub.auth.service;

import com.alvar.oasisclub.auth.dto.AuthSessionResponse;
import com.alvar.oasisclub.auth.dto.LoginRequest;
import com.alvar.oasisclub.auth.dto.RegisterRequest;
import com.alvar.oasisclub.auth.entity.PasswordResetTokenEntity;
import com.alvar.oasisclub.auth.exception.EmailAlreadyRegisteredException;
import com.alvar.oasisclub.auth.exception.InvalidCredentialsException;
import com.alvar.oasisclub.auth.exception.PasswordResetTokenInvalidException;
import com.alvar.oasisclub.auth.mapper.AuthMapper;
import com.alvar.oasisclub.auth.repository.PasswordResetTokenRepository;
import com.alvar.oasisclub.auth.security.JwtService;
import com.alvar.oasisclub.clients.entity.ClientEntity;
import com.alvar.oasisclub.clients.service.ClientService;
import com.alvar.oasisclub.common.config.AppFrontendUrlProperties;
import com.alvar.oasisclub.common.email.EmailService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@AllArgsConstructor
public class AuthService {
}