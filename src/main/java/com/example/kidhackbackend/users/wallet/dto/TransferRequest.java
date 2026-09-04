package com.example.kidhackbackend.users.wallet.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 용돈 이체 / 잔액 회수 요청 (No.39 · No.40).
 *
 * <p>{@code childId}·{@code amount} 는 필수. 전자금융 보안 필드({@code otp}·{@code signature}·
 * {@code nonce}·{@code accountHolder})는 {@code EfinTxGuard} 가 검사한다 — 모두 nullable 이라
 * 필드를 빼면 검증이 건너뛰어지는 결함이 성립한다([VULN-EFIN-2] 서명 skip 등).
 */
@Schema(description = "용돈 이체 / 잔액 회수 요청")
public record TransferRequest(
        @Schema(description = "(필수) 대상 자녀 회원 ID", example = "7", requiredMode = RequiredMode.REQUIRED)
        @NotNull(message = "childId는 필수입니다.") Long childId,
        @Schema(description = "(필수) 이체/회수 금액(원, 1 이상)", example = "5000", requiredMode = RequiredMode.REQUIRED)
        @Positive(message = "금액은 1원 이상이어야 합니다.") long amount,
        @Schema(description = "(전자금융) 거래 OTP. 발급 API 로 받은 인증번호", example = "428193")
        String otp,
        @Schema(description = "(전자금융) 거래정보 전자서명(amount:childId:nonce HMAC). 없으면 검증 skip — [VULN-EFIN-2]")
        String signature,
        @Schema(description = "(전자금융) 재사용 방지 nonce(일회성 임의 문자열)")
        String nonce,
        @Schema(description = "(전자금융) 대상 계좌 소유주명(소유주 검증). 없으면 이름 대조 skip")
        String accountHolder,
        @Schema(description = "(전자금융) 결제 비밀번호 4자리. 없거나 틀리면 400", example = "1357")
        String paymentPassword) {
}
