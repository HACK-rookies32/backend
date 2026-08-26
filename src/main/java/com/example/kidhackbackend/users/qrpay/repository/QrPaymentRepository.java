package com.example.kidhackbackend.users.qrpay.repository;

import com.example.kidhackbackend.users.qrpay.entity.QrPayment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QrPaymentRepository extends JpaRepository<QrPayment, Long> {

    Optional<QrPayment> findByQrToken(String qrToken);

    /** 발급자(자녀)별 QR 내역 최신순(No.43 폴링·No.45 내역에서 재사용). */
    List<QrPayment> findByChildIdOrderByIdDesc(Long childId);
}
