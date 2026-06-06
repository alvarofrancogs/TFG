package com.alvar.oasisclub.common.email.repository;

import com.alvar.oasisclub.common.email.entity.EmailFailureEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailFailureRepository extends JpaRepository<EmailFailureEntity, UUID> {
}
