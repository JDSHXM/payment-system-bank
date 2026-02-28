package com.paynetSystem.paynetSystemBank.exceptions;

public class InvalidTransactionException extends RuntimeException {
    public InvalidTransactionException(String error) {
        super(error);
    }
}
