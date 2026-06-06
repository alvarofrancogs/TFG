package com.alvar.oasisclub.common.exception;

public class StripeOperationFailedException extends RuntimeException {

  public StripeOperationFailedException(String message) {
    super(message);
  }

  public StripeOperationFailedException(String message, Throwable cause) {
    super(message, cause);
  }
}
