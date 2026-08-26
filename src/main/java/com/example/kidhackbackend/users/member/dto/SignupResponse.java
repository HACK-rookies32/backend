package com.example.kidhackbackend.users.member.dto;

import com.example.kidhackbackend.users.member.entity.Role;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SignupResponse {
    private Long id;
    private String loginId;
    private String username; // 실명
    private String nickname;
    private String email;
    private String phoneNumber;
    private LocalDate birthDate;
    private String address;
    private String profileImageUrl;
    private Role role;
    private Long parentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}