package com.alvar.oasisclub.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.alvar.oasisclub.clients.dto.ClientResponse;
import com.alvar.oasisclub.clients.dto.CreateClientRequest;
import com.alvar.oasisclub.clients.entity.ClientEntity;
import com.alvar.oasisclub.clients.exception.ClientEmailAlreadyExistsException;
import com.alvar.oasisclub.clients.exception.ClientNotFoundException;
import com.alvar.oasisclub.clients.mapper.ClientMapper;
import com.alvar.oasisclub.clients.repository.ClientRepository;
import com.alvar.oasisclub.clients.service.ClientService;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientServiceTest {
}