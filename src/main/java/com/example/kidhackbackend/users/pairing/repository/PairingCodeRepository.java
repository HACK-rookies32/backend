package com.example.kidhackbackend.users.pairing.repository;

import com.example.kidhackbackend.users.pairing.entity.PairingCode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PairingCodeRepository extends JpaRepository<PairingCode, Long> {
}
