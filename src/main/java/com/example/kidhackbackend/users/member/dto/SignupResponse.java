package com.example.kidhackbackend.users.member.dto;

import com.example.kidhackbackend.users.member.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "회원가입 결과 — 가입된 회원 정보")
@Getter
@AllArgsConstructor
public class SignupResponse {

    @Schema(description = "회원 PK", example = "1")
    private Long id;

    @Schema(description = "로그인 아이디", example = "parent01")
    private String loginId;

    @Schema(description = "실명", example = "홍길동")
    private String username;

    @Schema(description = "닉네임", example = "보라")
    private String nickname;

    @Schema(description = "이메일(CHILD 로 생략 가입 시 null)", example = "parent01@test.com")
    private String email;

    @Schema(description = "휴대폰 번호", example = "010-1111-2222")
    private String phoneNumber;

    @Schema(description = "생년월일", example = "1985-05-10")
    private LocalDate birthDate;

    @Schema(description = "주소", example = "서울시 강남구")
    private String address;

    @Schema(description = "프로필 이미지 URL")
    private String profileImageUrl;

    @Schema(description = "역할", example = "PARENT")
    private Role role;

    @Schema(description = "연결된 부모 회원 id(PARENT 는 null)", example = "1")
    private Long parentId;

    @Schema(description = "생성 일시")
    private LocalDateTime createdAt;

    @Schema(description = "수정 일시")
    private LocalDateTime updatedAt;
}
