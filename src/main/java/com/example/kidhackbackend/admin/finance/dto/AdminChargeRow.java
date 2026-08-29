package com.example.kidhackbackend.admin.finance.dto;

import com.example.kidhackbackend.users.wallet.repository.AdminChargeProjection;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 관리자 충전 목록의 한 행 (3분할 3/3).
 *
 * <p>거래 원장({@code GET /finance/transactions})은 "얼마가 오갔는가"만 남기므로 <b>주문 상태와 PG 거래
 * ID 가 없다</b>. 그래서 원장만 봐서는 아직 결제 대기(READY)인지, 만료됐는지, 취소됐는지 알 수 없다.
 * 이 API 가 {@code charge_order} 를 직접 읽어 그 값들을 제공한다.
 *
 * <p><b>필드명은 모바일(회원) 응답에 맞춘다.</b> PK 는 {@code id}(#146 이 관리자 공지의
 * {@code noticeId} 를 {@code id} 로 통일한 것과 같은 규약), 환불 시각·사유는
 * {@code ChargeHistoryResponse.refundedAt} 및 {@code AdminPaymentRow.refundReason} 과 같은
 * {@code refundedAt}/{@code refundReason} 을 쓴다. {@code canceledAt} 이라 부르면
 * {@code status} 의 {@code CANCELLED}(사용자가 결제 진행 중 취소)와 헷갈린다 — 이 필드가
 * 가리키는 상태는 {@code REFUNDED} 다.
 */
@Schema(description = "관리자 충전 1행")
public record AdminChargeRow(
        @Schema(description = "충전 주문 id", example = "12") Long id,
        @Schema(description = "가맹점 주문번호. 충전취소 API 의 경로 파라미터로 그대로 쓴다",
                example = "kid_6de30504395748c1993a6ef46efe62e4") String merchantUid,
        @Schema(description = "주문자 회원 id", example = "3") Long userId,
        @Schema(description = "주문자 실명. 회원이 삭제됐으면 null", example = "김보호") String userName,
        @Schema(description = "주문자 이메일. 자녀는 가입 시 선택이라 null 일 수 있다",
                example = "p1@test.com") String userEmail,
        @Schema(description = "충전 금액(원)", example = "50000") Long amount,
        @Schema(description = "주문 상태. 철자 주의 — CANCELLED(L 2개)", example = "PAID",
                allowableValues = {"READY", "PAID", "FAILED", "REFUNDED", "CANCELLED", "EXPIRED"})
        String status,
        @Schema(description = "결제 수단. MockPG 라 수단 개념이 없어 고정값", example = "카드") String method,
        @Schema(description = "PG 승인 거래 ID. 승인 전에는 null", example = "imp_1a2b3c")
        String pgTransactionId,
        @Schema(description = "충전 준비(주문 생성) 시각") LocalDateTime createdAt,
        @Schema(description = "환불 시각. REFUNDED 주문이면 경로와 무관하게 값이 있다") LocalDateTime refundedAt,
        @Schema(description = "환불 사유. 관리자 취소만 값이 있다 — 부모가 직접 환불한 건은 null",
                example = "잘못 충전 민원 처리") String refundReason) {

    /**
     * MockPG 라 결제수단을 저장하지 않는다. 충전 요청에 카드 선택이 없고 {@code ChargeOrder} 에도
     * 수단 필드가 없어, 프론트 컬럼을 채우기 위한 고정값을 내려준다(3분할 확정 3번).
     */
    private static final String FIXED_METHOD = "카드";

    public static AdminChargeRow from(AdminChargeProjection p) {
        return new AdminChargeRow(
                p.getId(),
                p.getMerchantUid(),
                p.getUserId(),
                p.getUserName(),
                p.getUserEmail(),
                p.getAmount(),
                p.getStatus(),
                FIXED_METHOD,
                p.getPgTransactionId(),
                p.getCreatedAt(),
                p.getRefundedAt(),
                p.getRefundReason());
    }
}
