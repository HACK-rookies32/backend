package com.example.kidhackbackend.users.member.dto;

import lombok.Getter;

@Getter
public class FindIdVerifyRequest {
    private String email;
    private String code;
}