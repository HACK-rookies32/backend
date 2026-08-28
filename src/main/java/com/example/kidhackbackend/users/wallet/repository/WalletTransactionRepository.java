package com.example.kidhackbackend.users.wallet.repository;

import com.example.kidhackbackend.users.wallet.entity.TxType;
import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    /** 지갑별 원장 최신순(충전내역 조회 No.37에서 재사용). */
    List<WalletTransaction> findByWalletIdOrderByIdDesc(Long walletId);

    /** 지갑별 원장 최신순 — 페이징(No.37). */
    Page<WalletTransaction> findByWalletIdOrderByIdDesc(Long walletId, Pageable pageable);

    /** 지갑별 최근거래 5건(자녀 지갑 요약 No.41). */
    List<WalletTransaction> findTop5ByWalletIdOrderByIdDesc(Long walletId);

    /** 참조 UID(merchantUid 등) + 유형으로 가장 최근 원장 1건 — 충전 내역 통합 뷰의 환불 시각 조회(P-3). */
    Optional<WalletTransaction> findFirstByRefUidAndTypeOrderByIdDesc(String refUid, TxType type);

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

    /** 관리자 전체 원장 조회(No.47): walletId optional(null=전체) + type/기간 필터·페이징. */
    @Query("SELECT t FROM WalletTransaction t WHERE (:walletId IS NULL OR t.walletId = :walletId) "
            + "AND (:type IS NULL OR t.type = :type) "
            + "AND (:from IS NULL OR t.createdAt >= :from) "
            + "AND (:to IS NULL OR t.createdAt <= :to) "
            + "ORDER BY t.id DESC")
    Page<WalletTransaction> adminSearch(@Param("walletId") Long walletId,
                                        @Param("type") TxType type,
                                        @Param("from") LocalDateTime from,
                                        @Param("to") LocalDateTime to,
                                        Pageable pageable);

    /** 통계 집계용(No.51 DAY): 기간 내 원장 전체(기간 null이면 전체). */
    @Query("SELECT t FROM WalletTransaction t WHERE (:from IS NULL OR t.createdAt >= :from) "
            + "AND (:to IS NULL OR t.createdAt <= :to)")
    List<WalletTransaction> findForStats(@Param("from") LocalDateTime from,
                                         @Param("to") LocalDateTime to);

    /** 특정 상대(refUid)와의 이체/회수 이력 — 자녀별 이체 이력(T-4). */
    Page<WalletTransaction> findByWalletIdAndTypeInAndRefUidOrderByIdDesc(
            Long walletId, Collection<TxType> types, String refUid, Pageable pageable);

    /** 지갑의 기간 내 특정 유형 금액 합계 — 내 지갑 월간 요약(V-3). 없으면 0. */
    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM WalletTransaction t "
            + "WHERE t.walletId = :walletId AND t.type = :type "
            + "AND t.createdAt >= :from AND t.createdAt < :to")
    long sumAmount(@Param("walletId") Long walletId,
                   @Param("type") TxType type,
                   @Param("from") LocalDateTime from,
                   @Param("to") LocalDateTime to);
}
