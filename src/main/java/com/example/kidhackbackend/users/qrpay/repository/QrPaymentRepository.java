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
}
