package com.example.kidhackbackend.users.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 회원가입 휴대폰 인증 - 코드 검증 결과.
 *
 * <p>⚠️ [VULN-PHONEVERIFY-2] 클라이언트가 이 {@code valid} 응답을 신뢰해 다음 단계로 넘어간다.
 * 서버는 이 결과를 가입과 묶어 강제하지 않으므로, 응답을 {@code valid:true} 로 변조하면
 * 틀린 코드로도 인증을 통과시킬 수 있다(응답 변조 우회).
 */
@Schema(description = "회원가입 휴대폰 인증 - 코드 검증 결과")
public record PhoneVerifyResponse(
        @Schema(description = "인증 성공 여부", example = "true") boolean valid
) {
}
