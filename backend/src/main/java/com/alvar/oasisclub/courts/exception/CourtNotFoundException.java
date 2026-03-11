package com.alvar.oasisclub.courts.exception;

public class CourtNotFoundException extends RuntimeException {
  public CourtNotFoundException(String message) {
    super(message);
  }
}
