package com.example.kidhackbackend.users.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProfileResponse {
    private Long id;
    private String loginId;
    private String username;
    private String nickname;
    private String email;
    private String phoneNumber;
    private String address;
    private String profileImageUrl;
}