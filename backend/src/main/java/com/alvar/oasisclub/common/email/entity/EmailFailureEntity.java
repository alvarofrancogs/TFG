package com.alvar.oasisclub.common.email.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "email_failures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailFailureEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "to_email", nullable = false)
  private String toEmail;

  @Column(name = "email_type", nullable = false, length = 50)
  private String emailType;

  @Column(name = "subject", length = 150)
  private String subject;

  @Column(name = "error_message", columnDefinition = "TEXT")
  private String errorMessage;

  @Column(name = "failed_at", nullable = false)
  private LocalDateTime failedAt;
}
