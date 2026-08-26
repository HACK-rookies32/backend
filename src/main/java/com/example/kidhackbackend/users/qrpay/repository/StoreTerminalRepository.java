package com.example.kidhackbackend.users.qrpay.repository;

import com.example.kidhackbackend.users.qrpay.entity.StoreTerminal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreTerminalRepository extends JpaRepository<StoreTerminal, Long> {

    Optional<StoreTerminal> findByTerminalToken(String terminalToken);

    /** 등록 단말 목록 최신순. */
    List<StoreTerminal> findAllByOrderByIdDesc();
}
