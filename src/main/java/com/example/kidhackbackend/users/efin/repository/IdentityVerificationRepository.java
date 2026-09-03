package com.example.kidhackbackend.users.efin.repository;

import com.example.kidhackbackend.users.efin.entity.IdentityVerification;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdentityVerificationRepository extends JpaRepository<IdentityVerification, Long> {
    Optional<IdentityVerification> findTopByMemberIdOrderByCreatedAtDesc(Long memberId);
}