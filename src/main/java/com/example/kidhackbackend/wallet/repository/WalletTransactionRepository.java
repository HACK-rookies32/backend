package com.example.kidhackbackend.wallet.repository;

import com.example.kidhackbackend.wallet.entity.WalletTransaction;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    /** 지갑별 원장 최신순(충전내역 조회 No.37에서 재사용). */
    List<WalletTransaction> findByWalletIdOrderByIdDesc(Long walletId);
}
