package com.example.kidhackbackend.global.mail;

import com.example.kidhackbackend.global.mail.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findTopByEmailAndPurposeOrderByCreatedAtDesc(String email, EmailVerification.Purpose purpose);
}