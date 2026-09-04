package com.example.kidhackbackend.users.efin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 결제 비밀번호 최초 설정 요청(접근매체 발급 흐름). 4자리 숫자.
 *
 * <p>직전 실명확인이 본인확인을 대신하므로 로그인 비밀번호 step-up 은 요구하지 않는다.
 */
@Schema(description = "결제 비밀번호 최초 설정 요청")
public record PaymentPasswordSetupRequest(
        @Schema(description = "(필수) 4자리 숫자 결제 비밀번호", example = "1357", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "결제 비밀번호는 필수입니다.")
        @Pattern(regexp = "\\d{4}", message = "결제 비밀번호는 4자리 숫자여야 합니다.") String password) {
}
