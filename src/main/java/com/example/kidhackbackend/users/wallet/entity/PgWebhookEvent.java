package com.example.kidhackbackend.users.wallet.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * PG 웹훅 수신 기록 — 역할표 No.36 (W-3).
 *
 * <p>어떤 웹훅이 언제 왔고 어떻게 처리됐는지(확정/무시/실패) 남긴다. 결제 분쟁·재전송 추적용.
 * rawBody 는 저장하지 않는다(민감·용량).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "pg_webhook_event",
        indexes = @Index(name = "idx_pg_webhook_merchant", columnList = "merchantUid,createdAt")
)
public class PgWebhookEvent extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 웹훅이 가리킨 가맹점 주문번호(파싱 실패 시 null). */
    private String merchantUid;

    /** 웹훅이 통지한 결제 상태 문자열(paid/failed/…). */
    private String status;

    /** 웹훅이 통지한 결제 금액(원). */
    private long paidAmount;

    /** X-PG-Signature 헤더가 왔는지. */
    @Column(nullable = false)
    private boolean signatureProvided;

    /**
     * 처리 결과:
     * CONFIRMED(확정·적립) / MARKED_FAILED(실패 반영) / IGNORED_NOT_READY(멱등: 이미 처리됨) /
     * IGNORED_STATUS(paid/실패 외 상태) / ORDER_NOT_FOUND / SIGNATURE_INVALID.
     */
    @Column(nullable = false)
    private String outcome;

    public PgWebhookEvent(String merchantUid, String status, long paidAmount,
                          boolean signatureProvided, String outcome) {
        this.merchantUid = merchantUid;
        this.status = status;
        this.paidAmount = paidAmount;
        this.signatureProvided = signatureProvided;
        this.outcome = outcome;
    }
}
