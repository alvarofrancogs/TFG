-- =============================================
-- V15: Add court_names column to events
-- =============================================
ALTER TABLE events
  ADD COLUMN IF NOT EXISTS court_names TEXT;
