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

    // [VULN-PHONEVERIFY-2] 클라이언트가 자기 인증상태를 담아 보내지만, 서버는 이 값을 확인하지
    //   않는다(무시). 휴대폰 인증은 POST /api/auth/phone/verify 의 {valid} 응답을 클라가 신뢰하는
    //   것으로 끝나고 가입과 서버에서 묶이지 않는다 → verify 응답변조/생략으로 우회된다.
    @Schema(description = "(선택) 클라이언트 인증상태 표식 — 서버는 확인하지 않음(의도된 취약점)", example = "true")
    private Boolean phoneVerified;
}
