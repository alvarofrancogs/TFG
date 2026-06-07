package com.alvar.oasisclub.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
  private static final ObjectMapper MAPPER = new ObjectMapper();

  
  private static final String LOGIN_PATH          = "/api/v1/auth/login";
  private static final String FORGOT_PATH         = "/api/v1/auth/forgot-password";
  private static final String CONTACT_PATH        = "/api/v1/contact";
  private static final String NEWSLETTER_PREFIX   = "/api/v1/newsletter";
  private static final String WEBHOOK_PATH        = "/api/v1/payments/webhook";

  
  private final Cache<String, Bucket> globalBuckets;
  private final Cache<String, Bucket> loginBuckets;
  private final Cache<String, Bucket> forgotBuckets;
  private final Cache<String, Bucket> contactBuckets;
  private final Cache<String, Bucket> newsletterBuckets;

  public RateLimitFilter() {
    this.globalBuckets     = buildCache();
    this.loginBuckets      = buildCache();
    this.forgotBuckets     = buildCache();
    this.contactBuckets    = buildCache();
    this.newsletterBuckets = buildCache();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    String path   = request.getServletPath();
    String method = request.getMethod();
    String ip     = resolveClientIp(request);

    
    if (WEBHOOK_PATH.equals(path)) {
      filterChain.doFilter(request, response);
      return;
    }

    
    if (!globalBuckets.get(ip, k -> globalBucket()).tryConsume(1)) {
      return429(response, request, ip, "global", 60);
      return;
    }

    
    if ("POST".equalsIgnoreCase(method) && LOGIN_PATH.equals(path)) {
      if (!loginBuckets.get(ip, k -> sensitiveEndpointBucket(10, Duration.ofMinutes(15))).tryConsume(1)) {
        return429(response, request, ip, "login", 900);
        return;
      }
    }

    
    if ("POST".equalsIgnoreCase(method) && FORGOT_PATH.equals(path)) {
      if (!forgotBuckets.get(ip, k -> sensitiveEndpointBucket(5, Duration.ofMinutes(60))).tryConsume(1)) {
        return429(response, request, ip, "forgot-password", 3600);
        return;
      }
    }

    
    if ("POST".equalsIgnoreCase(method) && CONTACT_PATH.equals(path)) {
      if (!contactBuckets.get(ip, k -> sensitiveEndpointBucket(5, Duration.ofMinutes(60))).tryConsume(1)) {
        return429(response, request, ip, "contact", 3600);
        return;
      }
    }

    
    if ("POST".equalsIgnoreCase(method) && path.startsWith(NEWSLETTER_PREFIX)) {
      if (!newsletterBuckets.get(ip, k -> sensitiveEndpointBucket(5, Duration.ofMinutes(60))).tryConsume(1)) {
        return429(response, request, ip, "newsletter", 3600);
        return;
      }
    }

    filterChain.doFilter(request, response);
  }

  

  
  private Bucket globalBucket() {
    return Bucket.builder()
        .addLimit(Bandwidth.builder()
            .capacity(100)
            .refillGreedy(100, Duration.ofSeconds(60))
            .build())
        .build();
  }

  
  private Bucket sensitiveEndpointBucket(long limit, Duration duration) {
    return Bucket.builder()
        .addLimit(Bandwidth.builder()
            .capacity(limit)
            .refillGreedy(limit, duration)
            .build())
        .build();
  }

  

  private void return429(
      HttpServletResponse response,
      HttpServletRequest request,
      String ip,
      String limitType,
      int retryAfterSeconds
  ) throws IOException {
    
    log.warn("[RATE_LIMIT] ip={} path={} type={} → 429 Too Many Requests",
        ip, request.getServletPath(), limitType);

    
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

    Map<String, Object> body = Map.of(
        "timestamp", LocalDateTime.now().toString(),
        "status", 429,
        "error", "Demasiadas solicitudes",
        "message", "Has superado el límite de solicitudes. Por favor, espera un momento.",
        "path", request.getServletPath()
    );
    MAPPER.writeValue(response.getOutputStream(), body);
  }

  

  
  private String resolveClientIp(HttpServletRequest request) {
    String forwarded = request.getHeader("X-Forwarded-For");
    if (forwarded != null && !forwarded.isBlank()) {
      
      return forwarded.split(",")[0].trim();
    }
    String realIp = request.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isBlank()) {
      return realIp.trim();
    }
    return request.getRemoteAddr();
  }

  

  
  private <K, V> Cache<K, V> buildCache() {
    return Caffeine.newBuilder()
        .expireAfterAccess(65, TimeUnit.MINUTES)
        .maximumSize(50_000)
        .build();
  }
}
