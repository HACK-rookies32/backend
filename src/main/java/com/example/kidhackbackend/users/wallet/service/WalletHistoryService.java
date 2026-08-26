package com.example.kidhackbackend.users.wallet.service;

import com.example.kidhackbackend.users.wallet.dto.ChargeOrderResponse;
import com.example.kidhackbackend.users.wallet.dto.TxResponse;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.repository.ChargeOrderRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지갑 내역 조회 — 역할표 No.37. 충전/환불 원장과 충전 주문 내역을 조회한다.
 *
 * <p>No.35·36에서 쌓인 데이터를 재사용(조회만). 충전 서비스와 분리해 결합도를 낮춘다.
 */
@Service
@RequiredArgsConstructor
public class WalletHistoryService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final ChargeOrderRepository chargeOrderRepository;

    /** 본인 지갑 원장(충전/환불) 내역 최신순. 지갑이 없으면 빈 목록. */
    @Transactional(readOnly = true)
    public List<TxResponse> getTransactions(Long ownerId) {
        return walletRepository.findByOwnerId(ownerId)
                .map(Wallet::getId)
                .map(walletTransactionRepository::findByWalletIdOrderByIdDesc)
                .orElseGet(List::of)
                .stream()
                .map(TxResponse::from)
                .toList();
    }

    /** 본인 충전 주문 내역 최신순(상태 포함). */
    @Transactional(readOnly = true)
    public List<ChargeOrderResponse> getCharges(Long ownerId) {
        return chargeOrderRepository.findByOwnerIdOrderByIdDesc(ownerId).stream()
                .map(ChargeOrderResponse::from)
                .toList();
    }
}
