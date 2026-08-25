package com.example.kidhackbackend.users.mypage.dto;

import lombok.Getter;

@Getter
public class ProfileUpdateRequest {
    private String nickname;
    private String phoneNumber;
    private String email;
    private String address;
    private String profileImageUrl;
}