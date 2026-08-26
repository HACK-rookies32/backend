package com.example.kidhackbackend.users.wallet.repository;

import com.example.kidhackbackend.users.wallet.entity.TxType;
import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    /** 지갑별 원장 최신순(충전내역 조회 No.37에서 재사용). */
    List<WalletTransaction> findByWalletIdOrderByIdDesc(Long walletId);

    /** 지갑별 최근거래 5건(자녀 지갑 요약 No.41). */
    List<WalletTransaction> findTop5ByWalletIdOrderByIdDesc(Long walletId);

    /** 내 지갑 원장 필터(type·기간 optional)·페이징 조회(No.38). */
    @Query("SELECT t FROM WalletTransaction t WHERE t.walletId = :walletId "
            + "AND (:type IS NULL OR t.type = :type) "
            + "AND (:from IS NULL OR t.createdAt >= :from) "
            + "AND (:to IS NULL OR t.createdAt <= :to) "
            + "ORDER BY t.id DESC")
    Page<WalletTransaction> search(@Param("walletId") Long walletId,
                                   @Param("type") TxType type,
                                   @Param("from") LocalDateTime from,
                                   @Param("to") LocalDateTime to,
                                   Pageable pageable);
}
