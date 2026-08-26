package com.example.kidhackbackend.users.member.dto;

import lombok.Getter;

@Getter
public class TokenRefreshRequest {
    private String refreshToken;
}