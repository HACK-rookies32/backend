package com.example.kidhackbackend.global.mail;

public interface EmailPort {
    void sendVerificationCode(String to, String code);
}