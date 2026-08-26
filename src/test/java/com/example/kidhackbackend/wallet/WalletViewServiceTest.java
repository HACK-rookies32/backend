package com.example.kidhackbackend.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kidhackbackend.users.wallet.dto.ChildWalletResponse;
import com.example.kidhackbackend.users.wallet.dto.MyWalletResponse;
import com.example.kidhackbackend.users.wallet.entity.TxType;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import com.example.kidhackbackend.users.wallet.service.WalletViewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * 지갑 조회(No.38·41) 슬라이스 테스트 — 내 지갑(필터·페이징)/자녀 지갑 요약 + 취약점(페어링 IDOR) 재현.
 */
@DataJpaTest
@Import({WalletViewService.class})
class WalletViewServiceTest {

    private static final long OWNER_ID = 100L;
    private static final long CHILD_ID = 200L;
    private static final long PARENT_ID = 500L;
    private static final long ATTACKER_PARENT_ID = 999L;

    @Autowired WalletViewService service;
    @Autowired WalletRepository walletRepository;
    @Autowired WalletTransactionRepository txRepository;

    private Long makeWallet(long ownerId, long balance) {
        Wallet w = new Wallet(ownerId);
        w.credit(balance);
        return walletRepository.save(w).getId();
    }

    private void tx(long walletId, TxType type, long amount, long balanceAfter) {
        txRepository.save(new WalletTransaction(walletId, type, amount, balanceAfter, "ref"));
    }

    @Test
    void 내지갑_잔액과_원장_전체가_조회된다() {
        long wid = makeWallet(OWNER_ID, 5000L);
        tx(wid, TxType.CHARGE, 5000L, 5000L);
        tx(wid, TxType.PAYMENT, 2000L, 3000L);

        MyWalletResponse res = service.getMyWallet(OWNER_ID, null, null, null, 0, 10);

        assertThat(res.balance()).isEqualTo(5000L);
        assertThat(res.totalElements()).isEqualTo(2);
    }

    @Test
    void 내지갑_type필터가_적용된다() {
        long wid = makeWallet(OWNER_ID, 5000L);
        tx(wid, TxType.CHARGE, 5000L, 5000L);
        tx(wid, TxType.PAYMENT, 2000L, 3000L);

        MyWalletResponse res = service.getMyWallet(OWNER_ID, TxType.PAYMENT, null, null, 0, 10);

        assertThat(res.totalElements()).isEqualTo(1);
        assertThat(res.content().get(0).type()).isEqualTo("PAYMENT");
    }

    @Test
    void 내지갑_페이징_size1이면_totalPages_2() {
        long wid = makeWallet(OWNER_ID, 5000L);
        tx(wid, TxType.CHARGE, 5000L, 5000L);
        tx(wid, TxType.PAYMENT, 2000L, 3000L);

        MyWalletResponse res = service.getMyWallet(OWNER_ID, null, null, null, 0, 1);

        assertThat(res.content()).hasSize(1);
        assertThat(res.totalPages()).isEqualTo(2);
    }

    @Test
    void 지갑이_없으면_빈_응답() {
        MyWalletResponse res = service.getMyWallet(OWNER_ID, null, null, null, 0, 10);
        assertThat(res.balance()).isZero();
        assertThat(res.totalElements()).isZero();
    }

    @Test
    void 자녀지갑_잔액과_최근거래_요약이_조회된다() {
        long wid = makeWallet(CHILD_ID, 8000L);
        tx(wid, TxType.TRANSFER_IN, 10000L, 10000L);
        tx(wid, TxType.PAYMENT, 2000L, 8000L);

        ChildWalletResponse res = service.getChildWallet(PARENT_ID, CHILD_ID);

        assertThat(res.balance()).isEqualTo(8000L);
        assertThat(res.recentTransactions()).hasSize(2);
    }

    @Test
    void VULN_WALLET_20_페어링검증없이_남의_자녀_지갑을_조회할수있다() {
        long wid = makeWallet(CHILD_ID, 8000L);
        tx(wid, TxType.PAYMENT, 2000L, 8000L);

        // ATTACKER_PARENT_ID는 CHILD_ID의 부모가 아님 — 그런데도 조회됨
        ChildWalletResponse res = service.getChildWallet(ATTACKER_PARENT_ID, CHILD_ID);

        assertThat(res.balance()).isEqualTo(8000L); // 취약: 남의 자녀 잔액 노출
        assertThat(res.recentTransactions()).hasSize(1);
    }
}
