package com.example.kidhackbackend.users.member.dto;

import lombok.Getter;

@Getter
public class ResetPasswordSendCodeRequest {
    private String loginId;
    private String email;
}