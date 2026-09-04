package com.example.kidhackbackend.users.efin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 결제 비밀번호 변경 요청. 본인확인(기존 결제 비밀번호 + 앱 로그인 비밀번호) + 새 비밀번호.
 *
 * <p>과거에 쓰던 비밀번호로는 변경할 수 없다(이력 대조). 단, 이력이 평문 저장이라 대조 과정 자체가
 * 정보노출을 키운다([VULN-EFIN-6]).
 */
@Schema(description = "결제 비밀번호 변경 요청")
public record PaymentPasswordChangeRequest(
        @Schema(description = "(필수) 기존 결제 비밀번호(본인확인)", example = "1357", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "기존 결제 비밀번호는 필수입니다.")
        @Pattern(regexp = "\\d{4}", message = "결제 비밀번호는 4자리 숫자여야 합니다.") String currentPassword,
        @Schema(description = "(필수) 새 결제 비밀번호", example = "2468", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "새 결제 비밀번호는 필수입니다.")
        @Pattern(regexp = "\\d{4}", message = "결제 비밀번호는 4자리 숫자여야 합니다.") String newPassword,
        @Schema(description = "(필수) 본인확인용 앱 로그인 비밀번호", example = "1234", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "로그인 비밀번호는 필수입니다.") String loginPassword) {
}
