package com.example.kidhackbackend.wallet.repository;

import com.example.kidhackbackend.wallet.entity.ChargeOrder;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargeOrderRepository extends JpaRepository<ChargeOrder, Long> {

    Optional<ChargeOrder> findByMerchantUid(String merchantUid);
}
