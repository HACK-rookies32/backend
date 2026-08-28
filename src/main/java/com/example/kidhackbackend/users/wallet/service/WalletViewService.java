package com.example.kidhackbackend.users.wallet.service;

import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.wallet.dto.ChildWalletResponse;
import com.example.kidhackbackend.users.wallet.dto.MyWalletResponse;
import com.example.kidhackbackend.users.wallet.dto.TxPageResponse;
import com.example.kidhackbackend.users.wallet.entity.TxType;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지갑 조회 — 내 지갑(No.38)·자녀 지갑(No.41). 잔액 + 원장 조회 로직을 공유한다.
 *
 * <p>내 지갑: 본인(JWT) 잔액 + 이번 달 요약 + type/기간 필터·페이징 원장.
 * 자녀 지갑: 부모가 자녀 잔액 + 최근거래 요약/상세 원장/이체 이력. 조회만(변경 없음).
 */
@Service
@RequiredArgsConstructor
public class WalletViewService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final MemberRepository memberRepository;

    /** 내 지갑(No.38): 본인 잔액 + 이번 달 요약 + 필터·페이징 원장. 지갑이 없으면 빈 응답. */
    @Transactional(readOnly = true)
    public MyWalletResponse getMyWallet(Long ownerId, TxType type, LocalDateTime from, LocalDateTime to,
                                        int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return walletRepository.findByOwnerId(ownerId)
                .map(w -> MyWalletResponse.from(
                        w.getBalance(),
                        monthlySum(w.getId(), TxType.CHARGE),
                        monthlySum(w.getId(), TxType.PAYMENT),
                        walletTransactionRepository.search(w.getId(), type, from, to, pageable)))
                .orElseGet(() -> MyWalletResponse.empty(page, size));
    }

    /**
     * 자녀 지갑(No.41): 부모가 자녀 잔액 + 최근거래 요약을 조회한다.
     *
     * <p>[VULN-WALLET-20] 소유권(페어링)은 여전히 검증하지 않는다(IDOR/BOLA).
     *   childId 가 실재 CHILD 인지만 확인하고 child.getParent()==parentId 는 보지 않으므로,
     *   아무 PARENT 나 남의 자녀 지갑을 열람할 수 있다. (유령 id·비-CHILD 만 차단됨)
     *
     * @param limit 최근 거래 개수(1~50)
     */
    @Transactional(readOnly = true)
    public ChildWalletResponse getChildWallet(Long parentId, Long childId, int limit) {
        requireExistingChild(childId);
        int capped = Math.max(1, Math.min(limit, 50));
        return walletRepository.findByOwnerId(childId)
                .map(w -> ChildWalletResponse.of(childId, w.getBalance(),
                        walletTransactionRepository
                                .findByWalletIdOrderByIdDesc(w.getId(), PageRequest.of(0, capped))
                                .getContent()))
                .orElseGet(() -> ChildWalletResponse.of(childId, 0L, List.of()));
    }

    /** 자녀 지갑 상세 원장(V-1): 부모가 자녀의 전체 거래 원장을 필터·페이징 조회. 소유권 미검증(VULN-WALLET-20). */
    @Transactional(readOnly = true)
    public MyWalletResponse getChildTransactions(Long parentId, Long childId, TxType type,
                                                 LocalDateTime from, LocalDateTime to, int page, int size) {
        requireExistingChild(childId);
        Pageable pageable = PageRequest.of(page, size);
        return walletRepository.findByOwnerId(childId)
                .map(w -> MyWalletResponse.from(
                        w.getBalance(),
                        monthlySum(w.getId(), TxType.CHARGE),
                        monthlySum(w.getId(), TxType.PAYMENT),
                        walletTransactionRepository.search(w.getId(), type, from, to, pageable)))
                .orElseGet(() -> MyWalletResponse.empty(page, size));
    }

    /** 자녀별 이체 이력(T-4): 이 부모↔자녀 사이의 TRANSFER_IN/OUT 만 (부모 지갑 기준). 소유권 미검증. */
    @Transactional(readOnly = true)
    public TxPageResponse getChildTransfers(Long parentId, Long childId, int page, int size) {
        requireExistingChild(childId);
        Pageable pageable = PageRequest.of(page, size);
        return walletRepository.findByOwnerId(parentId)
                .map(w -> TxPageResponse.from(
                        walletTransactionRepository.findByWalletIdAndTypeInAndRefUidOrderByIdDesc(
                                w.getId(),
                                List.of(TxType.TRANSFER_IN, TxType.TRANSFER_OUT),
                                String.valueOf(childId),
                                pageable)))
                .orElseGet(() -> TxPageResponse.empty(page, size));
    }

    /** 이번 달(1일 00:00 ~ 다음 달 1일 00:00) 특정 유형 합계. */
    private long monthlySum(Long walletId, TxType type) {
        YearMonth ym = YearMonth.now();
        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to = ym.plusMonths(1).atDay(1).atStartOfDay();
        return walletTransactionRepository.sumAmount(walletId, type, from, to);
    }

    private void requireExistingChild(Long childId) {
        Member child = memberRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("자녀를 찾을 수 없습니다."));
        if (child.isDeleted() || child.getRole() != Role.CHILD) {
            throw new IllegalArgumentException("자녀를 찾을 수 없습니다.");
        }
    }
}
