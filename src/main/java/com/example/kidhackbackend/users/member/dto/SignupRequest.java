package com.example.kidhackbackend.users.member.dto;

import com.example.kidhackbackend.users.member.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;

@Getter
public class SignupRequest {
    private String loginId;
    private String username;
    private String password;
    private String nickname;
    private String email; // role이 CHILD가 아니면 필수 — AuthService에서 검증

    @NotBlank(message = "전화번호는 필수입니다.")
    private String phoneNumber;

    @NotNull(message = "생년월일은 필수입니다.")
    private LocalDate birthDate;

    private String address;
    private String profileImageUrl;
    private Role role;
    private Long parentId;
}