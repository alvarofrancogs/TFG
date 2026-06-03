package com.alvar.oasisclub.newsletter;

import com.alvar.oasisclub.auth.security.AccessControlService;
import com.alvar.oasisclub.auth.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

  private final NewsletterService newsletterService;
  private final AccessControlService accessControl;

  public record SubscribeRequest(@NotBlank @Email String email) {}

  @PostMapping("/subscribe")
  public ResponseEntity<Void> subscribe(
      @Valid @RequestBody SubscribeRequest request,
      Authentication authentication
  ) {
    // Si hay usuario autenticado, usar su email del token (más seguro)
    String email = resolveEmail(authentication, request.email());
    newsletterService.subscribe(email);
    return ResponseEntity.ok().build();
  }

  @PostMapping("/unsubscribe")
  public ResponseEntity<Void> unsubscribe(
      @Valid @RequestBody SubscribeRequest request,
      Authentication authentication
  ) {
    String email = resolveEmail(authentication, request.email());
    newsletterService.unsubscribe(email);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/status")
  public ResponseEntity<Boolean> status(Authentication authentication) {
    AuthenticatedUser user = accessControl.requireUser(authentication);
    boolean subscribed = newsletterService.isSubscribed(user.email());
    return ResponseEntity.ok(subscribed);
  }

  private String resolveEmail(Authentication authentication, String fallback) {
    if (authentication != null && authentication.isAuthenticated()) {
      try {
        return accessControl.requireUser(authentication).email();
      } catch (Exception ignored) {}
    }
    return fallback;
  }
}
