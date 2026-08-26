package com.example.kidhackbackend.admin.finance.service;

import com.example.kidhackbackend.admin.finance.dto.AdminTxPageResponse;
import com.example.kidhackbackend.admin.finance.dto.AdminUserWalletResponse;
import com.example.kidhackbackend.admin.finance.dto.FinanceStatsResponse;
import com.example.kidhackbackend.admin.finance.dto.FinanceStatsResponse.StatRow;
import com.example.kidhackbackend.users.qrpay.entity.QrPayment;
import com.example.kidhackbackend.users.qrpay.repository.QrPaymentRepository;
import com.example.kidhackbackend.users.wallet.entity.TxType;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 자금 조회 — 역할표 No.47(전체 내역)·49(회원 지갑)·51(통계). 읽기 전용.
 *
 * <p>전체 회원 대상(관리자 권한). 기존 지갑/QR 데이터를 조회·집계만 한다.
 */
@Service
@RequiredArgsConstructor
public class AdminFinanceQueryService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final QrPaymentRepository qrPaymentRepository;

    /** No.47 전체 충전/결제 내역: userId(optional)·type·기간 필터 페이징. */
    @Transactional(readOnly = true)
    public AdminTxPageResponse getTransactions(Long userId, TxType type, LocalDateTime from, LocalDateTime to,
                                               int page, int size) {
        Long walletId = null;
        if (userId != null) {
            // 해당 회원 지갑이 없으면 매칭 없는 sentinel(-1)로 → 빈 결과.
            walletId = walletRepository.findByOwnerId(userId).map(Wallet::getId).orElse(-1L);
        }
        Pageable pageable = PageRequest.of(page, size);
        return AdminTxPageResponse.from(
                walletTransactionRepository.adminSearch(walletId, type, from, to, pageable));
    }

    /** No.49 특정 회원 지갑: 잔액 + 최근거래 요약. */
    @Transactional(readOnly = true)
    public AdminUserWalletResponse getUserWallet(Long userId) {
        return walletRepository.findByOwnerId(userId)
                .map(w -> AdminUserWalletResponse.of(userId, w.getBalance(),
                        walletTransactionRepository.findTop5ByWalletIdOrderByIdDesc(w.getId())))
                .orElseGet(() -> AdminUserWalletResponse.of(userId, 0L, List.of()));
    }

    /** No.51 통계: groupBy DAY(일자별 충전/결제 합계·건수) 또는 MERCHANT(매장별 결제 합계·건수). */
    @Transactional(readOnly = true)
    public FinanceStatsResponse getStats(String groupBy, LocalDateTime from, LocalDateTime to) {
        if ("MERCHANT".equalsIgnoreCase(groupBy)) {
            return new FinanceStatsResponse("MERCHANT", statsByMerchant(from, to));
        }
        return new FinanceStatsResponse("DAY", statsByDay(from, to));
    }

    private List<StatRow> statsByDay(LocalDateTime from, LocalDateTime to) {
        Map<LocalDate, long[]> acc = new LinkedHashMap<>(); // [chargeSum, paymentSum, count]
        for (WalletTransaction t : walletTransactionRepository.findForStats(from, to)) {
            if (t.getCreatedAt() == null) {
                continue;
            }
            long[] row = acc.computeIfAbsent(t.getCreatedAt().toLocalDate(), k -> new long[3]);
            if (t.getType() == TxType.CHARGE) {
                row[0] += t.getAmount();
            } else if (t.getType() == TxType.PAYMENT) {
                row[1] += t.getAmount();
            }
            row[2] += 1;
        }
        List<StatRow> rows = new ArrayList<>();
        acc.forEach((day, r) -> rows.add(new StatRow(day.toString(), r[0], r[1], r[2])));
        return rows;
    }

    private List<StatRow> statsByMerchant(LocalDateTime from, LocalDateTime to) {
        Map<String, long[]> acc = new LinkedHashMap<>(); // [paymentSum, count]
        for (QrPayment q : qrPaymentRepository.findPaidForStats(from, to)) {
            String store = q.getStoreName() != null ? q.getStoreName() : "UNKNOWN";
            long[] row = acc.computeIfAbsent(store, k -> new long[2]);
            row[0] += (q.getAmount() != null ? q.getAmount() : 0L);
            row[1] += 1;
        }
        List<StatRow> rows = new ArrayList<>();
        acc.forEach((store, r) -> rows.add(new StatRow(store, 0L, r[0], r[1])));
        return rows;
    }
}
