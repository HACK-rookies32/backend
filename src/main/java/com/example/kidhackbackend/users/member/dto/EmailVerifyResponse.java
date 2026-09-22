package com.example.kidhackbackend.users.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 회원가입 이메일 인증 - 검증 결과.
 *
 * <p>⚠️ [VULN-EMAILVERIFY-1] 검증 판정({@code verified})을 그대로 응답에 돌려준다.
 * 서버는 이 결과로 아무것도 강제하지 않고(로그인·가입은 인증 여부를 검사하지 않는다),
 * 인증 통과 판단을 <b>전적으로 모바일 클라이언트가 응답의 이 boolean 으로</b> 내린다.
 * 따라서 프록시(Burp 등)로 응답의 {@code verified:false → true} 를 변조하면 인증 단계를
 * 우회할 수 있다(클라이언트 사이드 게이트 우회). efin 실명확인 [VULN-EFIN-7] 과 같은 계열.
 */
@Schema(description = "회원가입 이메일 인증 - 검증 결과")
public record EmailVerifyResponse(
        @Schema(description = "인증코드 검증 통과 여부", example = "true") boolean verified
) {
}
