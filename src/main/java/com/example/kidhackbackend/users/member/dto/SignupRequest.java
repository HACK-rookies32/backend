package com.example.kidhackbackend.users.member.dto;

import com.example.kidhackbackend.users.member.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;

@Schema(description = "회원가입 요청")
@Getter
public class SignupRequest {

    @Schema(description = "로그인 아이디", example = "parent01")
    @NotBlank(message = "로그인 아이디는 필수입니다.")
    private String loginId;

    @Schema(description = "실명", example = "홍길동")
    @NotBlank(message = "이름은 필수입니다.")
    private String username;

    @Schema(description = "비밀번호(평문 저장 — 의도된 취약점)", example = "1234")
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    @Schema(description = "닉네임", example = "보라")
    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    @Schema(description = "이메일. role 이 CHILD 가 아니면 필수", example = "parent01@test.com")
    private String email;

    @Schema(description = "휴대폰 번호", example = "010-1111-2222")
    @NotBlank(message = "전화번호는 필수입니다.")
    private String phoneNumber;

    @Schema(description = "생년월일(yyyy-MM-dd)", example = "1985-05-10")
    @NotNull(message = "생년월일은 필수입니다.")
    private LocalDate birthDate;

    @Schema(description = "주소(선택)", example = "서울시 강남구 테헤란로 123")
    private String address;

    @Schema(description = "프로필 이미지 URL(선택)")
    private String profileImageUrl;

    @Schema(description = "역할", example = "PARENT")
    @NotNull(message = "역할은 필수입니다.")
    private Role role;

    @Schema(description = "role 이 CHILD 일 때 연결할 부모 회원 id(선택)", example = "1")
    private Long parentId;
}
