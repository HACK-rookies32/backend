package com.example.kidhackbackend.users.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 회원가입 휴대폰 인증 - 인증코드 발송 결과.
 *
 * <p>⚠️ [VULN-PHONEVERIFY-1] 발신 채널(SMS)과 같은 응답 body 에 코드를 그대로 실어 준다.
 * 정상이라면 코드는 그 번호의 소유자에게만 SMS 로 가야 하므로, 이 노출은 소지 기반 인증을 무력화한다.
 * 클라이언트는 이 코드를 화면에 표시하지 않는다(패킷 가로채기로만 확인 가능).
 */
@Schema(description = "회원가입 휴대폰 인증 - 인증코드 발송 결과")
public record PhoneVerifySendCodeResponse(
        @Schema(description = "안내 메시지", example = "인증번호가 발송되었습니다.") String message,
        @Schema(description = "발송된 6자리 인증코드(mock — 실제 화면 노출 금지)", example = "126207") String code
) {
}
