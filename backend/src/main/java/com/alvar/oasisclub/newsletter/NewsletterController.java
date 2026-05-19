package com.alvar.oasisclub.newsletter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

  private final NewsletterService newsletterService;

  public record SubscribeRequest(@NotBlank @Email String email) {}

  @PostMapping("/subscribe")
  public ResponseEntity<Void> subscribe(@Valid @RequestBody SubscribeRequest request) {
    newsletterService.subscribe(request.email());
    return ResponseEntity.ok().build();
  }
}
