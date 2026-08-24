package com.example.kidhackbackend.users.member.dto;

import com.example.kidhackbackend.users.member.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
    private Role role;
}