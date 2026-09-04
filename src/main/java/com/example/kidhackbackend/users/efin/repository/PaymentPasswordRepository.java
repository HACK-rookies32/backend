package com.example.kidhackbackend.users.efin.repository;

import com.example.kidhackbackend.users.efin.entity.PaymentPassword;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentPasswordRepository extends JpaRepository<PaymentPassword, Long> {
    Optional<PaymentPassword> findByMemberId(Long memberId);
    boolean existsByMemberId(Long memberId);
}
