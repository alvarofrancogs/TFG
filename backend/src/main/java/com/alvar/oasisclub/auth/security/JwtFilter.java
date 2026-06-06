package com.alvar.oasisclub.auth.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.Date;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import com.alvar.oasisclub.clients.repository.ClientRepository;
import com.alvar.oasisclub.clients.entity.ClientEntity;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@AllArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final ClientRepository clientRepository;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    String token = authHeader.substring(7);

    if (jwtService.isTokenValid(token)) {
      UUID clientId = jwtService.extractClientId(token);
      String role = jwtService.extractRole(token);
      String email = jwtService.extractEmail(token);
      Date issuedAt = jwtService.extractIssuedAt(token);

      ClientEntity client = clientRepository.findById(clientId).orElse(null);

      if (client != null 
          && client.getRole().equals(role)
          && (client.getPasswordChangedAt() == null || !issuedAt.before(Date.from(client.getPasswordChangedAt())))) {

        AuthenticatedUser principal = new AuthenticatedUser(clientId, email, role);

        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );

        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }

    filterChain.doFilter(request, response);
  }
}

