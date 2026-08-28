package com.example.kidhackbackend.users.wallet.repository;

import com.example.kidhackbackend.users.wallet.entity.ChargeOrder;
import com.example.kidhackbackend.users.wallet.entity.ChargeStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargeOrderRepository extends JpaRepository<ChargeOrder, Long> {

    Optional<ChargeOrder> findByMerchantUid(String merchantUid);

    /** 소유자별 충전 주문 내역 최신순(No.37 충전내역 조회). */
    List<ChargeOrder> findByOwnerIdOrderByIdDesc(Long ownerId);

    /** 소유자별 충전 주문 내역 최신순 — 페이징(No.37). */
    Page<ChargeOrder> findByOwnerIdOrderByIdDesc(Long ownerId, Pageable pageable);

    /** 만료 배치: 특정 상태이면서 생성 시각이 기준보다 오래된 주문(READY 만료 대상 조회). */
    List<ChargeOrder> findByStatusAndCreatedAtBefore(ChargeStatus status, LocalDateTime threshold);
}
