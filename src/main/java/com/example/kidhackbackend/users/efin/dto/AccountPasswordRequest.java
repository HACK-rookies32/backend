package com.example.kidhackbackend.users.efin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 계좌비밀번호 최초 설정 요청. 6자리 숫자.
 *
 * <p>거래용 별도 비밀번호를 만드는 민감 작업이라, 앱 로그인 비밀번호로 본인확인을 거친다(step-up).
 */
@Schema(description = "계좌비밀번호 설정 요청")
public record AccountPasswordRequest(
        @Schema(description = "(필수) 6자리 숫자 계좌비밀번호", example = "135790", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "계좌비밀번호는 필수입니다.")
        @Pattern(regexp = "\\d{6}", message = "계좌비밀번호는 6자리 숫자여야 합니다.") String password,
        @Schema(description = "(필수) 본인확인용 앱 로그인 비밀번호", example = "1234", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "로그인 비밀번호는 필수입니다.") String loginPassword) {
}