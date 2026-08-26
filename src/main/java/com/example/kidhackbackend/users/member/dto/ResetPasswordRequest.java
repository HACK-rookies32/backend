package com.example.kidhackbackend.users.member.dto;

import lombok.Getter;

@Getter
public class ResetPasswordRequest {
    private String email;
    private String code;
    private String newPassword;
}