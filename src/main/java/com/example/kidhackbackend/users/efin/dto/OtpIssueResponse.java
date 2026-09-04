package com.example.kidhackbackend.users.efin.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 거래 OTP 발급 응답.
 *
 * <p>⚠️ [VULN-EFIN-1] 발급된 인증번호를 <b>응답 body({@code code})에 그대로 노출</b>한다. mock SMS 라
 * 실제 문자는 발신되지 않지만, 응답만 가로채면 OTP 를 알 수 있어 2차 인증이 무력화된다
 * (아이디찾기/비번재설정 코드 응답노출 — VULN-AUTH-MAIL 과 동형).
 */
@Schema(description = "거래 OTP 발급 응답")
public record OtpIssueResponse(
        @Schema(description = "발송 대상 전화번호(마스킹)", example = "010-****-1234")
        String phone,
        @Schema(description = "[VULN-EFIN-1] 발급된 인증번호(응답 노출)", example = "428193")
        String code,
        @Schema(description = "유효시간(초)", example = "180")
        long expiresIn) {
}