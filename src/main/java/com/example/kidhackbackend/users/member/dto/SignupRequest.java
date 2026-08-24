package com.example.kidhackbackend.users.member.dto;

import com.example.kidhackbackend.users.member.entity.Role;
import java.time.LocalDate;
import lombok.Getter;

// [VULN-6] 검증 애너테이션 없음
@Getter
public class SignupRequest {
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String phoneNumber;
    private LocalDate birthDate;
    private String address;
    private String profileImageUrl;
    private Role role;
    private Long parentId;
}