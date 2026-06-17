package com.alvar.oasisclub.clients.service;

import com.alvar.oasisclub.clients.dto.ClientResponse;
import com.alvar.oasisclub.clients.dto.CreateClientRequest;
import com.alvar.oasisclub.clients.entity.ClientEntity;
import com.alvar.oasisclub.clients.exception.ClientEmailAlreadyExistsException;
import com.alvar.oasisclub.clients.exception.ClientNotFoundException;
import com.alvar.oasisclub.clients.mapper.ClientMapper;
import com.alvar.oasisclub.clients.repository.ClientRepository;
import com.alvar.oasisclub.common.email.EmailService;
import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ClientService {

  private final ClientRepository clientRepository;
  private final ClientMapper clientMapper;
  private final EmailService emailService;

  @Transactional(readOnly = true)
  public List<ClientResponse> getAllClients() {
    return clientRepository.findAll().stream()
        .map(clientMapper::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<ClientResponse> getClients(String query, Integer limit) {
    int safeLimit = limit == null ? 10 : Math.max(1, Math.min(limit, 50));
    var pageable = PageRequest.of(0, safeLimit);

    boolean hasQuery = query != null && !query.isBlank();
    if (hasQuery) {
      String normalizedQuery = query.trim();
      return clientRepository
          .findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrderByJoinDateDesc(
              normalizedQuery,
              normalizedQuery,
              pageable
          )
          .stream()
          .map(clientMapper::toResponse)
          .toList();
    }

    return clientRepository.findAllByOrderByJoinDateDesc(pageable).stream()
        .map(clientMapper::toResponse)
        .toList();
  }

  @Transactional
  public ClientResponse createClient(CreateClientRequest request) {
    String email = request.getEmail().trim().toLowerCase();
    clientRepository.findByEmailIgnoreCase(email)
        .ifPresent(c -> {
          throw new ClientEmailAlreadyExistsException("Ya existe una cuenta con ese email");
        });

    if (request.getPhone() != null && !request.getPhone().isBlank()) {
      String normalizedPhone = normalizePhone(request.getPhone());
      clientRepository.findByPhone(normalizedPhone)
          .ifPresent(c -> {
            throw new ClientEmailAlreadyExistsException("Ya existe una cuenta con ese teléfono");
          });
    }

    if (request.getBirthDate() != null
        && Period.between(request.getBirthDate(), LocalDate.now()).getYears() < 14) {
      throw new IllegalArgumentException("El cliente debe tener al menos 14 años");
    }

    ClientEntity saved = clientRepository.save(clientMapper.toEntity(request));
    emailService.sendWelcomeEmail(saved.getEmail(), saved.getName());
    return clientMapper.toResponse(saved);
  }

  @Transactional
  public void deleteClient(UUID id) {
    ClientEntity client = getEntityById(id);

    if ("ADMIN".equalsIgnoreCase(client.getRole())) {
      throw new IllegalArgumentException("No se puede eliminar un usuario administrador.");
    }

    clientRepository.delete(client);
  }

  @Transactional(readOnly = true)
  public ClientEntity getEntityById(UUID id) {
    return clientRepository.findById(id)
        .orElseThrow(() -> new ClientNotFoundException("Cliente no encontrado"));
  }

  @Transactional(readOnly = true)
  public Map<UUID, ClientEntity> getEntitiesByIds(Collection<UUID> ids) {
    if (ids == null || ids.isEmpty()) {
      return Map.of();
    }
    return clientRepository.findAllById(ids).stream()
        .collect(Collectors.toMap(ClientEntity::getId, Function.identity()));
  }

  @Transactional(readOnly = true)
  public ClientEntity findByEmail(String email) {
    return clientRepository.findByEmailIgnoreCase(email).orElse(null);
  }

  @Transactional(readOnly = true)
  public ClientEntity findByPhone(String phone) {
    return clientRepository.findByPhone(normalizePhone(phone)).orElse(null);
  }

  
  public static String normalizePhone(String raw) {
    if (raw == null) return null;
    String stripped = raw.strip();
    boolean hasPlus = stripped.startsWith("+");
    String digits = stripped.replaceAll("[^0-9]", "");
    return hasPlus ? "+" + digits : digits;
  }

  @Transactional
  public void save(ClientEntity client) {
    clientRepository.save(client);
  }
}
