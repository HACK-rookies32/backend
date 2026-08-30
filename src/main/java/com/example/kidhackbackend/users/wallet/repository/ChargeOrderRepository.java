package com.example.kidhackbackend.users.wallet.repository;

import com.example.kidhackbackend.users.wallet.entity.ChargeOrder;
import com.example.kidhackbackend.users.wallet.entity.ChargeStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChargeOrderRepository extends JpaRepository<ChargeOrder, Long> {

    Optional<ChargeOrder> findByMerchantUid(String merchantUid);

    /** 소유자별 충전 주문 내역 최신순(No.37 충전내역 조회). */
    List<ChargeOrder> findByOwnerIdOrderByIdDesc(Long ownerId);

    /** 소유자별 충전 주문 내역 최신순 — 페이징(No.37). */
    Page<ChargeOrder> findByOwnerIdOrderByIdDesc(Long ownerId, Pageable pageable);

    /** 만료 배치: 특정 상태이면서 생성 시각이 기준보다 오래된 주문(READY 만료 대상 조회). */
    List<ChargeOrder> findByStatusAndCreatedAtBefore(ChargeStatus status, LocalDateTime threshold);

    /**
     * 관리자 충전 목록(#145) — 전체 주문을 회원 정보와 함께 페이징 조회한다.
     *
     * <p>{@code ChargeOrder} 가 {@code Member} 를 FK 로 물지 않고 {@code ownerId}(Long)만 들고 있어
     * 파생 쿼리로 조인이 안 된다. 그래서 네이티브 쿼리 + {@link AdminChargeProjection} 조합을 쓴다
     * ({@code QrPaymentRepository.adminSearchPayments} 와 같은 사정).
     *
     * <p>회원은 <b>LEFT JOIN</b> 이다 — 회원이 삭제돼도 충전 이력은 남아야 하고, 그 행이 목록에서
     * 사라지면 장부에 구멍이 생긴다. 그 경우 {@code userName}·{@code userEmail} 이 null 로 나간다.
     *
     * <p>취소 사유·시각은 {@code charge_order} 자체 컬럼(V10)에서 읽는다. 감사로그를 조인하지 않는다 —
     * 두 테이블 사이에 FK 가 없어 문자열 LIKE 조인이 되어버리기 때문이다(V10 주석 참고).
     *
     * @param userId  주문자 회원 id. null 이면 전체
     * @param status  주문 상태. null 이면 전체
     * @param keyword 주문자 실명 또는 이메일 부분일치. null 이면 전체
     */
    @Query(value = "SELECT co.id AS id, co.merchant_uid AS merchantUid, co.owner_id AS userId, "
            + "m.username AS userName, m.email AS userEmail, "
            + "co.amount AS amount, co.status AS status, "
            + "co.pg_transaction_id AS pgTransactionId, co.created_at AS createdAt, "
            + "co.refunded_at AS refundedAt, co.refund_reason AS refundReason "
            + "FROM charge_order co "
            + "LEFT JOIN member m ON m.id = co.owner_id "
            + "WHERE (:userId IS NULL OR co.owner_id = :userId) "
            + "AND (:status IS NULL OR co.status = :status) "
            + "AND (:keyword IS NULL OR m.username LIKE ('%' || :keyword || '%') "
            + "     OR m.email LIKE ('%' || :keyword || '%')) "
            + "ORDER BY co.id DESC",
            countQuery = "SELECT count(*) FROM charge_order co "
                    + "LEFT JOIN member m ON m.id = co.owner_id "
                    + "WHERE (:userId IS NULL OR co.owner_id = :userId) "
                    + "AND (:status IS NULL OR co.status = :status) "
                    + "AND (:keyword IS NULL OR m.username LIKE ('%' || :keyword || '%') "
                    + "     OR m.email LIKE ('%' || :keyword || '%'))",
            nativeQuery = true)
    Page<AdminChargeProjection> adminSearchCharges(@Param("userId") Long userId,
                                                   @Param("status") String status,
                                                   @Param("keyword") String keyword,
                                                   Pageable pageable);
}
