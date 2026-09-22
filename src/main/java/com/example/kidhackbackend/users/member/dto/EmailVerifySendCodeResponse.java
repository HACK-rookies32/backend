package com.example.kidhackbackend.users.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 회원가입 이메일 인증 - 인증코드 발송 결과.
 *
 * <p>⚠️ [VULN-EMAILVERIFY-2] 발송 코드를 응답 {@code data.code} 에도 그대로 노출한다
 * (기존 아이디찾기·비번재설정의 [VULN-AUTH-MAIL] 관례와 동일). 메일 발송은 mock 이라
 * 실제 수신함이 없어도 실습이 가능하도록 둔 노출이다 — 화면에는 노출 금지.
 */
@Schema(description = "회원가입 이메일 인증 - 인증코드 발송 결과")
public record EmailVerifySendCodeResponse(
        @Schema(description = "안내 메시지", example = "인증코드가 발송되었습니다.") String message,
        @Schema(description = "발송된 6자리 인증코드(mock — 실제 화면 노출 금지)", example = "126207") String code
) {
}
