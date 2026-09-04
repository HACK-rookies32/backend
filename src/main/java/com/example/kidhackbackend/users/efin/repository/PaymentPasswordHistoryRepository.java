package com.example.kidhackbackend.users.efin.repository;

import com.example.kidhackbackend.users.efin.entity.PaymentPasswordHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentPasswordHistoryRepository extends JpaRepository<PaymentPasswordHistory, Long> {
    List<PaymentPasswordHistory> findByMemberId(Long memberId);
}
