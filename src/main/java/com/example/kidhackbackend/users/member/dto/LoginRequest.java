package com.example.kidhackbackend.users.member.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LoginRequest {
    private String loginId;
    private String password;
}