package com.paynetSystem.paynetSystemBank.exceptions;

public class NotFoundException extends RuntimeException{
    public NotFoundException(String error){
        super(error);
    }
}
