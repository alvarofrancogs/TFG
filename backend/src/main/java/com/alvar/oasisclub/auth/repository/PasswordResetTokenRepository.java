package com.alvar.oasisclub.auth.repository;

import com.alvar.oasisclub.auth.entity.PasswordResetTokenEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

  Optional<PasswordResetTokenEntity> findByToken(String token);

  
  List<PasswordResetTokenEntity> findByClientIdAndUsedFalseAndExpirationAfter(
      UUID clientId, LocalDateTime now
  );

  
  @Modifying
  @Query("DELETE FROM PasswordResetTokenEntity t WHERE t.expiration < :now")
  int deleteExpiredTokens(@Param("now") LocalDateTime now);
}
