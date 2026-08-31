package com.example.kidhackbackend.users.wallet.service;

import com.example.kidhackbackend.users.wallet.dto.ChargeHistoryPageResponse;
import com.example.kidhackbackend.users.wallet.dto.ChargeHistoryResponse;
import com.example.kidhackbackend.users.wallet.dto.ChargeOrderPageResponse;
import com.example.kidhackbackend.users.wallet.dto.TxPageResponse;
import com.example.kidhackbackend.users.wallet.entity.ChargeOrder;
import com.example.kidhackbackend.users.wallet.entity.ChargeStatus;
import com.example.kidhackbackend.users.wallet.entity.TxType;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.repository.ChargeOrderRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지갑 내역 조회 — 역할표 No.37. 충전/환불 원장, 충전 주문 내역, 통합 내역을 페이징으로 조회한다.
 *
 * <p>No.35·36에서 쌓인 데이터를 재사용(조회만). 충전 서비스와 분리해 결합도를 낮춘다.
 */
@Service
@RequiredArgsConstructor
public class WalletHistoryService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final ChargeOrderRepository chargeOrderRepository;

    /** 본인 지갑 원장(충전/환불 등) 내역 최신순 — 페이징. 지갑이 없으면 빈 페이지. */
    @Transactional(readOnly = true)
    public TxPageResponse getTransactions(Long ownerId, Pageable pageable) {
        return walletRepository.findByOwnerId(ownerId)
                .map(Wallet::getId)
                .map(walletId -> walletTransactionRepository.findByWalletIdRecentFirst(walletId, pageable))
                .map(TxPageResponse::from)
                .orElseGet(() -> TxPageResponse.empty(pageable.getPageNumber(), pageable.getPageSize()));
    }

    /** 본인 충전 주문 내역 최신순(상태 포함) — 페이징. */
    @Transactional(readOnly = true)
    public ChargeOrderPageResponse getCharges(Long ownerId, Pageable pageable) {
        return ChargeOrderPageResponse.from(
                chargeOrderRepository.findByOwnerIdOrderByIdDesc(ownerId, pageable));
    }

    /**
     * 충전 내역 통합 뷰 — 주문 + 환불 시각을 한 줄로. (P-3)
     *
     * <p>REFUNDED 주문은 merchantUid 로 REFUND 원장을 찾아 환불 시각을 채운다(그 외에는 null).
     */
    @Transactional(readOnly = true)
    public ChargeHistoryPageResponse getChargeHistory(Long ownerId, Pageable pageable) {
        Page<ChargeOrder> page = chargeOrderRepository.findByOwnerIdOrderByIdDesc(ownerId, pageable);

        var content = page.getContent().stream()
                .map(order -> new ChargeHistoryResponse(
                        order.getMerchantUid(),
                        order.getAmount(),
                        order.getStatus().name(),
                        order.getPgTransactionId(),
                        order.getCreatedAt(),
                        refundedAt(order)))
                .toList();

        return new ChargeHistoryPageResponse(
                content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private LocalDateTime refundedAt(ChargeOrder order) {
        if (order.getStatus() != ChargeStatus.REFUNDED) {
            return null;
        }
        return walletTransactionRepository
                .findLatestByRefUidAndType(order.getMerchantUid(), TxType.REFUND)
                .map(tx -> tx.getCreatedAt())
                .orElse(null);
    }
}
