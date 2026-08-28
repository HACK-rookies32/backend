package com.example.kidhackbackend.users.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "아이디 찾기 - 인증코드 발송 결과")
public record FindIdSendCodeResponse(
        @Schema(description = "안내 메시지", example = "인증코드가 발송되었습니다.") String message,
        @Schema(description = "발송된 6자리 인증코드(mock — 실제 화면 노출 금지)", example = "126207") String code
) {
}
