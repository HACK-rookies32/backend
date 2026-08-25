package com.example.kidhackbackend.global.exception;


public class AdminAccessDeniedException extends RuntimeException {
    public AdminAccessDeniedException(String message) {
        super(message);
    }
}