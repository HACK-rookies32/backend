package com.example.kidhackbackend.users.pairing.dto;

import java.time.LocalDateTime;

public record PairingCodeResponse(String code, LocalDateTime expiresAt) {
}