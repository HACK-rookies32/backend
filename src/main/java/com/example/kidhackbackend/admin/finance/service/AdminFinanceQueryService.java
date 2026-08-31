package com.example.kidhackbackend.admin.finance.service;

import com.example.kidhackbackend.admin.finance.dto.AdminChargePageResponse;
import com.example.kidhackbackend.admin.finance.dto.AdminPaymentPageResponse;
import com.example.kidhackbackend.admin.finance.dto.AdminTxPageResponse;
import com.example.kidhackbackend.admin.finance.dto.AdminWalletPageResponse;
import com.example.kidhackbackend.admin.finance.dto.AdminUserWalletResponse;
import com.example.kidhackbackend.admin.finance.dto.FinanceStatsResponse;
import com.example.kidhackbackend.admin.finance.dto.FinanceStatsResponse.StatRow;
import com.example.kidhackbackend.users.qrpay.entity.QrPayment;
import com.example.kidhackbackend.users.qrpay.repository.QrPaymentRepository;
import com.example.kidhackbackend.users.wallet.entity.TxType;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.wallet.repository.ChargeOrderRepository;
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
    private final MemberRepository memberRepository;
    private final ChargeOrderRepository chargeOrderRepository;

    /** No.47/49 통합: 전체 충전/결제 내역. userId 지정 시 잔액도 함께 반환. */
    @Transactional(readOnly = true)
    public AdminTxPageResponse getTransactions(Long userId, TxType type, LocalDateTime from, LocalDateTime to,
                                               int page, int size) {
        Long walletId = null;
        Long walletBalance = null;
        if (userId != null) {
            Wallet wallet = walletRepository.findByOwnerId(userId).orElse(null);
            // 해당 회원 지갑이 없으면 매칭 없는 sentinel(-1)로 → 빈 결과. 잔액은 0으로 표시.
            walletId = (wallet != null) ? wallet.getId() : -1L;
            walletBalance = (wallet != null) ? wallet.getBalance() : 0L;
        }
        Pageable pageable = PageRequest.of(page, size);
        return AdminTxPageResponse.from(
                walletTransactionRepository.adminSearchWithMember(walletId, type, from, to, pageable), walletBalance);
    }

    /**
     * 회원 지갑 목록(프론트 3차 2번) — role·keyword 필터 페이징.
     *
     * <p>「회원 지갑」 탭은 잔액 조정의 진입점이라, <b>지갑이 아직 없는 회원도 balance 0 으로 포함</b>한다
     * (LEFT JOIN). 그래야 한 번도 충전받지 않은 자녀를 조정 대상으로 고를 수 있다.
     */
    @Transactional(readOnly = true)
    public AdminWalletPageResponse getWallets(String role, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return AdminWalletPageResponse.from(
                memberRepository.searchWalletsWithBalance(blankToNull(role), blankToNull(keyword), pageable));
    }

    /**
     * 매장결제 목록(3분할 2/3) — childId·status·merchant·keyword 필터 페이징.
     *
     * <p>결제된 QR(paidAt 존재)만 대상이다. 거래 원장에 없는 <b>매장명·결제 후 잔액</b>을 제공한다.
     */
    @Transactional(readOnly = true)
    public AdminPaymentPageResponse getPayments(Long childId, String status, String merchant,
                                                String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return AdminPaymentPageResponse.from(qrPaymentRepository.adminSearchPayments(
                childId, blankToNull(status), blankToNull(merchant), blankToNull(keyword), pageable));
    }

    /**
     * 관리자 충전 목록 (3분할 3/3, #145).
     *
     * <p>거래 원장에 없는 <b>주문 상태·PG 거래 ID·취소 사유</b>를 제공한다. 원장은 "얼마가 오갔는가"만
     * 남기므로, 아직 결제되지 않은 READY 주문이나 만료된 주문은 원장에 아예 나타나지 않는다.
     *
     * <p>취소 사유·시각은 {@code charge_order} 자체 컬럼(V10)에서 읽는다 — 감사로그를 조인하지 않는다.
     */
    @Transactional(readOnly = true)
    public AdminChargePageResponse getCharges(Long userId, String status, String keyword,
                                              int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return AdminChargePageResponse.from(chargeOrderRepository.adminSearchCharges(
                userId, blankToNull(status), blankToNull(keyword), pageable));
    }

    /** 빈 문자열 쿼리 파라미터를 null 로 바꿔 "필터 없음"으로 처리한다. */
    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    /** 고객관리(AdminCustomerService.getSubscription)에서 재사용 — 잔액 + 최근거래 요약. */
    @Transactional(readOnly = true)
    public AdminUserWalletResponse getUserWallet(Long userId) {
        return walletRepository.findByOwnerId(userId)
                .map(w -> AdminUserWalletResponse.of(userId, w.getBalance(),
                        walletTransactionRepository.findTop5ByWalletIdRecentFirst(w.getId())))
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
