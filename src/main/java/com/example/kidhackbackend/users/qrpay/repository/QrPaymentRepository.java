package com.example.kidhackbackend.users.qrpay.repository;

import com.example.kidhackbackend.users.qrpay.entity.QrPayment;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QrPaymentRepository extends JpaRepository<QrPayment, Long> {

    Optional<QrPayment> findByQrToken(String qrToken);

    /** 발급자(자녀)별 QR 내역 최신순(No.43 폴링에서 재사용). */
    List<QrPayment> findByChildIdOrderByIdDesc(Long childId);

    /** 자녀별 '결제된' 내역(paidAt 존재 = PAID/CANCELLED) 최신순 페이징(No.45). */
    Page<QrPayment> findByChildIdAndPaidAtIsNotNullOrderByIdDesc(Long childId, Pageable pageable);

    /** 매장 통계용(No.51 MERCHANT): 결제된(paidAt 존재) QR을 기간 필터로. */
    @Query("SELECT q FROM QrPayment q WHERE q.paidAt IS NOT NULL "
            + "AND (:from IS NULL OR q.paidAt >= :from) AND (:to IS NULL OR q.paidAt <= :to)")
    List<QrPayment> findPaidForStats(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * 관리자 매장결제 목록(3분할 2/3) — 결제된 QR + 자녀/보호자 + 결제 후 잔액.
     *
     * <p>결제된 건만 대상이라 {@code paid_at IS NOT NULL} 이다(발급만 되고 안 쓴 QR·만료 QR 제외).
     * 자녀는 {@code member}, 보호자는 자녀의 {@code parent_id} 를 한 번 더 조인해 이름까지 내려준다
     * (행마다 회원을 다시 조회하면 N+1).
     *
     * <p>{@code balanceAfter} 는 상관 서브쿼리로 <b>가장 최근 PAYMENT 원장 1건</b>을 고른다 —
     * 같은 qrToken 으로 중복 결제가 가능해({@code [VULN-WALLET-16]}) 단순 조인하면 결제 행이 복제된다.
     *
     * <p>LIKE 바인딩 이슈(MemberRepository.searchCustomers 주석)와 서브쿼리 때문에 네이티브로 작성한다.
     */
    @Query(value = "SELECT q.id AS id, q.child_id AS childId, c.username AS childName, "
            + "p.id AS parentId, p.username AS parentName, "
            + "q.store_name AS storeName, q.amount AS amount, q.status AS status, "
            + "(SELECT t.balance_after FROM wallet_transaction t "
            + " WHERE t.id = (SELECT MAX(t2.id) FROM wallet_transaction t2 "
            + "               WHERE t2.ref_uid = q.qr_token AND t2.type = 'PAYMENT')) AS balanceAfter, "
            + "q.paid_at AS paidAt "
            + "FROM qr_payment q "
            + "LEFT JOIN member c ON c.id = q.child_id "
            + "LEFT JOIN member p ON p.id = c.parent_id "
            + "WHERE q.paid_at IS NOT NULL "
            + "AND (:childId IS NULL OR q.child_id = :childId) "
            + "AND (:status IS NULL OR q.status = :status) "
            + "AND (:merchant IS NULL OR q.store_name LIKE CONCAT('%', :merchant, '%')) "
            + "AND (:keyword IS NULL OR c.username LIKE CONCAT('%', :keyword, '%') "
            + "     OR p.username LIKE CONCAT('%', :keyword, '%')) "
            + "ORDER BY q.id DESC",
            countQuery = "SELECT count(*) FROM qr_payment q "
                    + "LEFT JOIN member c ON c.id = q.child_id "
                    + "LEFT JOIN member p ON p.id = c.parent_id "
                    + "WHERE q.paid_at IS NOT NULL "
                    + "AND (:childId IS NULL OR q.child_id = :childId) "
                    + "AND (:status IS NULL OR q.status = :status) "
                    + "AND (:merchant IS NULL OR q.store_name LIKE CONCAT('%', :merchant, '%')) "
                    + "AND (:keyword IS NULL OR c.username LIKE CONCAT('%', :keyword, '%') "
                    + "     OR p.username LIKE CONCAT('%', :keyword, '%'))",
            nativeQuery = true)
    Page<AdminPaymentProjection> adminSearchPayments(@Param("childId") Long childId,
                                                     @Param("status") String status,
                                                     @Param("merchant") String merchant,
                                                     @Param("keyword") String keyword,
                                                     Pageable pageable);
}
