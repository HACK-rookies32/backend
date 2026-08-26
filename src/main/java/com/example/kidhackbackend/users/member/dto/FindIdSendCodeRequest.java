package com.example.kidhackbackend.users.member.dto;

import lombok.Getter;

@Getter
public class FindIdSendCodeRequest {
    private String username;
    private String email;
}