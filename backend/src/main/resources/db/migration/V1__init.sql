CREATE TABLE clients (
  id UUID PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  email VARCHAR(180) NOT NULL UNIQUE,
  join_date DATE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  role VARCHAR(20) NOT NULL,
  phone VARCHAR(20) UNIQUE,
  birth_date DATE,
  newsletter_subscribed BOOLEAN NOT NULL DEFAULT FALSE,
  password_changed_at TIMESTAMP
);

CREATE TABLE courts (
  id UUID PRIMARY KEY,
  name VARCHAR(120) NOT NULL,
  sport VARCHAR(20) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL
);

CREATE TABLE reservations (
  id UUID PRIMARY KEY,
  client_id UUID NULL,
  user_name VARCHAR(120) NOT NULL,
  sport VARCHAR(20) NOT NULL,
  court_id UUID NOT NULL,
  reservation_date DATE NOT NULL,
  reservation_time TIME NOT NULL,
  status VARCHAR(30) NOT NULL,
  created_at TIMESTAMP NOT NULL,
  stripe_session_id VARCHAR(255),
  refunded_at TIMESTAMP NULL,
  CONSTRAINT fk_reservation_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE,
  CONSTRAINT fk_reservation_court FOREIGN KEY (court_id) REFERENCES courts(id) ON DELETE CASCADE
);

CREATE TABLE schedule_slots (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  slot_time TIME NOT NULL UNIQUE
);

CREATE TABLE gym_routine_days (
  id UUID PRIMARY KEY,
  client_id UUID NOT NULL,
  day_order INTEGER NOT NULL,
  name VARCHAR(120) NOT NULL,
  CONSTRAINT fk_routine_day_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
);

CREATE TABLE gym_routine_exercises (
  id UUID PRIMARY KEY,
  routine_day_id UUID NOT NULL,
  exercise_order INTEGER NOT NULL,
  name VARCHAR(160) NOT NULL,
  sets_count INTEGER NOT NULL,
  reps VARCHAR(30) NOT NULL,
  rest_interval VARCHAR(30) NOT NULL,
  CONSTRAINT fk_exercise_day FOREIGN KEY (routine_day_id) REFERENCES gym_routine_days(id) ON DELETE CASCADE
);

CREATE TABLE password_reset_tokens (
  id UUID PRIMARY KEY,
  client_id UUID NOT NULL,
  token VARCHAR(255) NOT NULL UNIQUE,
  expiration TIMESTAMP NOT NULL,
  used BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_reset_token_client FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
);

CREATE TABLE events (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  title VARCHAR(200) NOT NULL,
  description TEXT,
  event_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  max_capacity INTEGER NOT NULL,
  category VARCHAR(30) NOT NULL,
  sport VARCHAR(20),
  is_active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  court_names TEXT
);

CREATE TABLE event_registrations (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_id UUID NOT NULL REFERENCES events(id) ON DELETE CASCADE,
  client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE(event_id, client_id)
);

CREATE TABLE stripe_processed_events (
  id UUID PRIMARY KEY,
  stripe_event_id VARCHAR(255) NOT NULL UNIQUE,
  event_type VARCHAR(100) NOT NULL,
  processed_at TIMESTAMP NOT NULL
);

CREATE TABLE email_failures (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  to_email VARCHAR(255) NOT NULL,
  email_type VARCHAR(50) NOT NULL,
  subject VARCHAR(150),
  error_message TEXT,
  failed_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_reservation_date_court ON reservations (reservation_date, court_id);
CREATE INDEX idx_reservation_client ON reservations (client_id);
CREATE UNIQUE INDEX uq_reservation_slot ON reservations (court_id, reservation_date, reservation_time);
CREATE UNIQUE INDEX uq_reservation_stripe_session ON reservations (stripe_session_id);
CREATE INDEX idx_routine_day_client ON gym_routine_days (client_id, day_order);
CREATE INDEX idx_reset_token ON password_reset_tokens (token);
CREATE INDEX idx_event_date ON events (event_date);
CREATE INDEX idx_event_reg_event ON event_registrations (event_id);
CREATE INDEX idx_event_reg_client ON event_registrations (client_id);

INSERT INTO clients (id, name, email, join_date, password_hash, role, phone, birth_date)
VALUES ('33333333-3333-3333-3333-333333333333', 'Admin Club', 'alvaro@admin.com', DATE '2025-12-01',
        '$2a$10$48yJIxScSScEXagEc5.hoOXJV8vPyoIqeEnlesgrE05K/LnY5EHWW', 'ADMIN', '600000003', DATE '1990-01-01');

INSERT INTO clients (id, name, email, join_date, password_hash, role, phone, birth_date)
VALUES ('11111111-1111-1111-1111-111111111111', 'Álvaro Martínez', 'alvaro@example.com', DATE '2026-01-15',
        '$2a$10$48yJIxScSScEXagEc5.hoOXJV8vPyoIqeEnlesgrE05K/LnY5EHWW', 'MEMBER', '600000001', DATE '1995-06-15');

INSERT INTO courts (id, name, sport, is_active, created_at) VALUES
('55555555-5555-5555-5555-555555555551', 'Pista Cristal 1', 'PADEL', true, CURRENT_TIMESTAMP),
('55555555-5555-5555-5555-555555555552', 'Pista Muro', 'PADEL', true, CURRENT_TIMESTAMP),
('55555555-5555-5555-5555-555555555553', 'Pista 1 (F11)', 'FUTBOL', true, CURRENT_TIMESTAMP),
('55555555-5555-5555-5555-555555555554', 'Pista 2 (F7)', 'FUTBOL', true, CURRENT_TIMESTAMP);

INSERT INTO schedule_slots (slot_time) VALUES
('09:00'), ('10:30'), ('12:00'), ('13:30'),
('15:00'), ('16:30'), ('18:00'), ('19:30'), ('21:00');

INSERT INTO gym_routine_days (id, client_id, day_order, name) VALUES
('44444444-4444-4444-4444-444444444441', '11111111-1111-1111-1111-111111111111', 1, 'Fuerza'),
('44444444-4444-4444-4444-444444444442', '11111111-1111-1111-1111-111111111111', 2, 'Cardio'),
('44444444-4444-4444-4444-444444444443', '11111111-1111-1111-1111-111111111111', 3, 'Core');

INSERT INTO gym_routine_exercises (id, routine_day_id, exercise_order, name, sets_count, reps, rest_interval) VALUES
('66666666-6666-6666-6666-666666666661', '44444444-4444-4444-4444-444444444441', 1, 'Sentadilla Libre', 4, '10', '90s'),
('66666666-6666-6666-6666-666666666662', '44444444-4444-4444-4444-444444444441', 2, 'Press de Banca', 4, '8', '90s'),
('66666666-6666-6666-6666-666666666663', '44444444-4444-4444-4444-444444444441', 3, 'Dominadas', 3, '10', '60s');
