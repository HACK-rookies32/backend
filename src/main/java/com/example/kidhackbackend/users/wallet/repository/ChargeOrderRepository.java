package com.example.kidhackbackend.users.wallet.repository;

import com.example.kidhackbackend.users.wallet.entity.ChargeOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargeOrderRepository extends JpaRepository<ChargeOrder, Long> {

    Optional<ChargeOrder> findByMerchantUid(String merchantUid);

    /** 소유자별 충전 주문 내역 최신순(No.37 충전내역 조회). */
    List<ChargeOrder> findByOwnerIdOrderByIdDesc(Long ownerId);
}
