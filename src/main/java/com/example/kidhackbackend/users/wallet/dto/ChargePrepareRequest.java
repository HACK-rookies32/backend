package com.example.kidhackbackend.users.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.Positive;

/**
 * 충전 준비 요청.
 *
 * <p>충전도 돈이 움직이는 거래라 전자금융 관문(계좌비밀번호 + OTP)을 통과해야 한다. 관문은 prepare
 * 단계에서 앱→서버 사전차단으로 검사한다 — 실패하면 charge_order 를 만들지 않고 400 이라, 충전
 * 상태(ChargeStatus)에는 새 값이 생기지 않는다(웹 관리자 화면 무영향).
 */
@Schema(description = "충전 준비 요청")
public record ChargePrepareRequest(
        @Schema(description = "(필수) 충전 금액(원, 1 이상)", example = "10000", requiredMode = RequiredMode.REQUIRED)
        @Positive(message = "충전 금액은 1원 이상이어야 합니다.") long amount,
        @Schema(description = "(전자금융) 거래 OTP", example = "428193") String otp,
        @Schema(description = "(전자금융) 결제 비밀번호 4자리", example = "1357") String paymentPassword,
        @Schema(description = "(전자금융) 충전정보 전자서명(amount:0:nonce HMAC). 없으면 검증 skip — [VULN-EFIN-2]") String signature,
        @Schema(description = "(전자금융) 재사용 방지 nonce") String nonce) {

    /** 금액만 주는 편의 생성자 — 관문을 거치지 않는 서비스 레벨 호출/테스트용. */
    public ChargePrepareRequest(long amount) {
        this(amount, null, null, null, null);
    }
}
