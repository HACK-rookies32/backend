package com.example.kidhackbackend.users.qrpay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

/**
 * QR 스캔 결제 승인 요청 (No.44). 매장 단말이 스캔한 QR과 입력한 금액.
 *
 * <p>{@code qrToken}·{@code amount} 는 필수. 전자금융 보안 필드({@code signature}·{@code nonce})는
 * {@code EfinTxGuard.verifyQrPay} 가 검사한다 — nullable 이라 서명을 빼면 검증이 skip 된다([VULN-EFIN-2]).
 */
@Schema(description = "QR 스캔 결제 승인 요청 (매장 단말)")
public record QrApproveRequest(
        @Schema(description = "(필수) 스캔한 결제 QR 토큰", requiredMode = RequiredMode.REQUIRED)
        @NotBlank(message = "qrToken은 필수입니다.") String qrToken,
        @Schema(description = "(필수) 결제 금액(원, 1 이상 — 단말 입력)", example = "3000",
                requiredMode = RequiredMode.REQUIRED)
        @Positive(message = "결제 금액은 1원 이상이어야 합니다.") long amount,
        @Schema(description = "(전자금융) 결제정보 전자서명(amount:0:nonce HMAC). 없으면 검증 skip — [VULN-EFIN-2]")
        String signature,
        @Schema(description = "(전자금융) 재사용 방지 nonce(일회성 임의 문자열)")
        String nonce) {
}
