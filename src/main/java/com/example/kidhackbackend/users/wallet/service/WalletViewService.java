package com.example.kidhackbackend.users.wallet.service;

import com.example.kidhackbackend.users.wallet.dto.ChildWalletResponse;
import com.example.kidhackbackend.users.wallet.dto.MyWalletResponse;
import com.example.kidhackbackend.users.wallet.entity.TxType;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지갑 조회 — 내 지갑(No.38)·자녀 지갑(No.41). 잔액 + 원장 조회 로직을 공유한다.
 *
 * <p>내 지갑: 본인(JWT) 잔액 + type/기간 필터·페이징 원장.
 * 자녀 지갑: 부모가 자녀 잔액 + 최근거래 요약(본인 자녀만). 조회만(변경 없음).
 */
@Service
@RequiredArgsConstructor
public class WalletViewService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    /** 내 지갑(No.38): 본인 잔액 + 필터·페이징 원장. 지갑이 없으면 빈 응답. */
    @Transactional(readOnly = true)
    public MyWalletResponse getMyWallet(Long ownerId, TxType type, LocalDateTime from, LocalDateTime to,
                                        int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return walletRepository.findByOwnerId(ownerId)
                .map(w -> MyWalletResponse.from(w.getBalance(),
                        walletTransactionRepository.search(w.getId(), type, from, to, pageable)))
                .orElseGet(() -> MyWalletResponse.empty(page, size));
    }

    /**
     * 자녀 지갑(No.41): 부모가 자녀 잔액 + 최근거래 요약을 조회한다.
     *
     * @param parentId 조회 부모 id
     * @param childId  대상 자녀 id
     */
    @Transactional(readOnly = true)
    public ChildWalletResponse getChildWallet(Long parentId, Long childId) {
        // [VULN-WALLET-20] 자녀 지갑 조회 페어링 미검증(IDOR / BOLA).
        //   childId가 이 부모(parentId)의 자녀(Member.parent)인지 확인하지 않는다('본인 자녀만' 위반).
        //   → 아무 PARENT나 임의 childId로 남의 자녀 잔액·최근거래를 열람할 수 있다. (이체 VULN-7·내역 VULN-19와 동형)
        //   방어: memberRepository.findById(childId) 후 child.getParent().getId()==parentId + CHILD 확인.

        return walletRepository.findByOwnerId(childId)
                .map(w -> ChildWalletResponse.of(childId, w.getBalance(),
                        walletTransactionRepository.findTop5ByWalletIdOrderByIdDesc(w.getId())))
                .orElseGet(() -> ChildWalletResponse.of(childId, 0L, List.of()));
    }
}
