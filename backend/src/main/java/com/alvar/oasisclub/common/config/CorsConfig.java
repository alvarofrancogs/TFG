package com.alvar.oasisclub.common.config;

import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@AllArgsConstructor
public class CorsConfig {

  private final AppCorsProperties corsProperties;

  @PostConstruct
  public void validateCorsConfig() {
    List<String> origins = corsProperties.getAllowedOrigins();
    if (origins == null || origins.isEmpty()) {
      throw new IllegalStateException("CORS allowed-origins no puede estar vacío");
    }
    for (String origin : origins) {
      if (origin.contains("*")) {
        throw new IllegalStateException(
            "CORS origen no válido (wildcards prohibidos por seguridad): " + origin);
      }
    }
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(corsProperties.getAllowedOrigins());
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
  }
}

