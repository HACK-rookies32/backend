package com.example.kidhackbackend.users.member.dto;

import com.example.kidhackbackend.users.member.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Getter;

@Schema(description = "회원가입 요청")
@Getter
public class SignupRequest {

    @Schema(description = "(필수) 로그인 아이디", example = "parent01", requiredMode = RequiredMode.REQUIRED)
    @NotBlank(message = "로그인 아이디는 필수입니다.")
    private String loginId;

    @Schema(description = "(필수) 실명", example = "홍길동", requiredMode = RequiredMode.REQUIRED)
    @NotBlank(message = "이름은 필수입니다.")
    private String username;

    @Schema(description = "(필수) 비밀번호(평문 저장 — 의도된 취약점)", example = "1234", requiredMode = RequiredMode.REQUIRED)
    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    @Schema(description = "(필수) 닉네임", example = "보라", requiredMode = RequiredMode.REQUIRED)
    @NotBlank(message = "닉네임은 필수입니다.")
    private String nickname;

    @Schema(description = "(조건부 필수) 이메일 — role 이 CHILD 가 아니면 필수", example = "parent01@test.com")
    private String email;

    @Schema(description = "(필수) 휴대폰 번호", example = "010-1111-2222", requiredMode = RequiredMode.REQUIRED)
    @NotBlank(message = "전화번호는 필수입니다.")
    private String phoneNumber;

    @Schema(description = "(필수) 생년월일(yyyy-MM-dd)", example = "1985-05-10", requiredMode = RequiredMode.REQUIRED)
    @NotNull(message = "생년월일은 필수입니다.")
    private LocalDate birthDate;

    @Schema(description = "(선택) 주소", example = "서울시 강남구 테헤란로 123")
    private String address;

    @Schema(description = "(선택) 프로필 이미지 URL")
    private String profileImageUrl;

    @Schema(description = "(필수) 역할: PARENT / CHILD", example = "PARENT", requiredMode = RequiredMode.REQUIRED)
    @NotNull(message = "역할은 필수입니다.")
    private Role role;

    @Schema(description = "(선택) role 이 CHILD 일 때 연결할 부모 회원 id", example = "1")
    private Long parentId;
}
