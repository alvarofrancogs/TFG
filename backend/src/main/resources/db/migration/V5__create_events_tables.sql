-- =============================================
-- V14: Sistema de Eventos
-- =============================================

-- Tabla de eventos
CREATE TABLE events (
  id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title         VARCHAR(200) NOT NULL,
  description   TEXT,
  event_date    DATE NOT NULL,
  start_time    TIME NOT NULL,
  end_time      TIME NOT NULL,
  max_capacity  INTEGER NOT NULL,
  category      VARCHAR(30) NOT NULL,
  sport         VARCHAR(20),
  is_active     BOOLEAN NOT NULL DEFAULT TRUE,
  created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de inscripciones
CREATE TABLE event_registrations (
  id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id   UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
  client_id  UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(event_id, client_id)
);

CREATE INDEX idx_event_date ON events (event_date);
CREATE INDEX idx_event_reg_event ON event_registrations (event_id);
CREATE INDEX idx_event_reg_client ON event_registrations (client_id);
