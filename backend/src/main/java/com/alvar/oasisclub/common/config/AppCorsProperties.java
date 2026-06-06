package com.alvar.oasisclub.common.config;

import java.util.List;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsConfiguration;

@Component
@Getter
public class AppCorsProperties {

  @Value("${app.cors.allowed-origins:https://tfg-mocha.vercel.app,http://localhost:4200,http://127.0.0.1:4200,http://localhost:4300,http://127.0.0.1:4300}")
  private List<String> allowedOrigins;

  public boolean isAllowedOrigin(String origin) {
    if (origin == null || origin.isBlank()) {
      return false;
    }

    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    return config.checkOrigin(origin) != null;
  }
}
