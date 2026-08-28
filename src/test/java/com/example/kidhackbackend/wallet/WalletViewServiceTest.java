package com.example.kidhackbackend.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.wallet.dto.ChildWalletResponse;
import com.example.kidhackbackend.users.wallet.dto.MyWalletResponse;
import com.example.kidhackbackend.users.wallet.dto.TxPageResponse;
import com.example.kidhackbackend.users.wallet.entity.TxType;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import com.example.kidhackbackend.users.wallet.service.WalletViewService;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * 지갑 조회(No.38·41) 슬라이스 테스트 — 내 지갑(필터·페이징·월간요약)/자녀 지갑 요약·상세·이체이력
 * + 취약점(페어링 IDOR) 재현 + V-2(자녀 존재 검증).
 */
@DataJpaTest
@Import({WalletViewService.class})
class WalletViewServiceTest {

    private static final long OWNER_ID = 100L;

    @Autowired WalletViewService service;
    @Autowired WalletRepository walletRepository;
    @Autowired WalletTransactionRepository txRepository;
    @Autowired MemberRepository memberRepository;

    private Long persistChild() {
        Member m = new Member();
        m.setLoginId("CHILD_" + System.nanoTime());
        m.setUsername("자녀");
        m.setPassword("pw");
        m.setPhoneNumber("010-1234-5678");
        m.setBirthDate(LocalDate.of(2015, 3, 21));
        m.setRole(Role.CHILD);
        return memberRepository.save(m).getId();
    }

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
        assertThat(res.monthlyChargeTotal()).isEqualTo(5000L);
        assertThat(res.monthlySpendTotal()).isEqualTo(2000L);
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
    void 자녀지갑_요약_잔액과_최근거래_limit이_적용된다() {
        Long childId = persistChild();
        long wid = makeWallet(childId, 8000L);
        tx(wid, TxType.TRANSFER_IN, 10000L, 10000L);
        tx(wid, TxType.PAYMENT, 2000L, 8000L);
        tx(wid, TxType.PAYMENT, 500L, 7500L);

        ChildWalletResponse res = service.getChildWallet(999L, childId, 2);

        assertThat(res.balance()).isEqualTo(8000L);
        assertThat(res.recentTransactions()).hasSize(2); // limit=2
    }

    @Test
    void 자녀지갑_상세원장은_전체를_페이징한다() {
        Long childId = persistChild();
        long wid = makeWallet(childId, 8000L);
        tx(wid, TxType.CHARGE, 5000L, 5000L);
        tx(wid, TxType.TRANSFER_IN, 3000L, 8000L);

        MyWalletResponse res = service.getChildTransactions(999L, childId, null, null, null, 0, 10);

        assertThat(res.balance()).isEqualTo(8000L);
        assertThat(res.totalElements()).isEqualTo(2);
    }

    @Test
    void 자녀별_이체이력은_부모지갑의_해당자녀_TRANSFER만_반환한다() {
        Long parentId = 500L;
        Long childId = persistChild();
        Long otherChildId = persistChild();
        long pwid = makeWallet(parentId, 0L);
        // 이 자녀에게 보낸/회수한 것
        txRepository.save(new WalletTransaction(pwid, TxType.TRANSFER_OUT, 3000L, -3000L, String.valueOf(childId)));
        txRepository.save(new WalletTransaction(pwid, TxType.TRANSFER_IN, 1000L, -2000L, String.valueOf(childId)));
        // 다른 자녀 / 다른 유형 — 제외돼야 함
        txRepository.save(new WalletTransaction(pwid, TxType.TRANSFER_OUT, 9999L, 0L, String.valueOf(otherChildId)));
        txRepository.save(new WalletTransaction(pwid, TxType.CHARGE, 5000L, 5000L, "kid_x"));

        TxPageResponse res = service.getChildTransfers(parentId, childId, 0, 10);

        assertThat(res.totalElements()).isEqualTo(2);
    }

    @Test
    void V2_존재하지_않는_자녀_지갑조회는_거부된다() {
        assertThatThrownBy(() -> service.getChildWallet(500L, 999_999L, 5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("자녀를 찾을 수 없습니다");
    }

    @Test
    void VULN_WALLET_20_페어링검증없이_남의_자녀_지갑을_조회할수있다() {
        Long childId = persistChild(); // 500L 의 자녀로 연결하지 않음
        long wid = makeWallet(childId, 8000L);
        tx(wid, TxType.PAYMENT, 2000L, 8000L);

        // 999L 은 childId 의 부모가 아님 — 그런데도 조회됨
        ChildWalletResponse res = service.getChildWallet(999L, childId, 5);

        assertThat(res.balance()).isEqualTo(8000L); // 취약: 남의 자녀 잔액 노출
        assertThat(res.recentTransactions()).hasSize(1);
    }
}
