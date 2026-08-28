package com.example.kidhackbackend.adminfinance;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.wallet.entity.TxType;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.entity.WalletTransaction;
import com.example.kidhackbackend.users.wallet.repository.AdminTxProjection;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import com.example.kidhackbackend.users.wallet.repository.WalletTransactionRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * 관리자 거래내역 + 회원정보 조인 조회 슬라이스 테스트 (프론트 3차 3번).
 *
 * <p>{@code Wallet} 이 {@code Member} 를 FK 로 물지 않아 조인 쿼리가 직접 작성돼 있다.
 * userId·userName 이 올바른 회원과 매칭되는지, 회원이 없어도 거래가 누락되지 않는지(LEFT JOIN) 확인한다.
 */
@DataJpaTest
class AdminTxWithMemberTest {

    @Autowired WalletTransactionRepository txRepository;
    @Autowired WalletRepository walletRepository;
    @Autowired MemberRepository memberRepository;

    private Member newMember(String loginId, String username) {
        Member m = new Member();
        m.setLoginId(loginId);
        m.setUsername(username);
        m.setNickname(loginId);
        m.setPassword("pw");
        m.setPhoneNumber("01000000000");
        m.setBirthDate(LocalDate.of(1990, 1, 1));
        m.setRole(Role.PARENT);
        return memberRepository.save(m);
    }

    @Test
    void 거래에_소유_회원의_id와_실명이_함께_조회된다() {
        Member parent = newMember("p1", "김보호");
        Wallet wallet = walletRepository.save(new Wallet(parent.getId()));
        wallet.credit(50000);
        walletRepository.save(wallet);
        txRepository.save(new WalletTransaction(wallet.getId(), TxType.CHARGE, 50000, 50000, "kid_ab12"));

        Page<AdminTxProjection> page =
                txRepository.adminSearchWithMember(null, null, null, null, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        AdminTxProjection row = page.getContent().get(0);
        assertThat(row.getUserId()).isEqualTo(parent.getId());
        assertThat(row.getUserName()).isEqualTo("김보호");
        assertThat(row.getType()).isEqualTo("CHARGE");
        assertThat(row.getAmount()).isEqualTo(50000);
        assertThat(row.getRefUid()).isEqualTo("kid_ab12");
    }

    @Test
    void 회원별로_userId가_섞이지_않는다() {
        Member a = newMember("pa", "회원A");
        Member b = newMember("pb", "회원B");
        Wallet wa = walletRepository.save(new Wallet(a.getId()));
        Wallet wb = walletRepository.save(new Wallet(b.getId()));
        txRepository.save(new WalletTransaction(wa.getId(), TxType.CHARGE, 1000, 1000, "kid_a"));
        txRepository.save(new WalletTransaction(wb.getId(), TxType.CHARGE, 2000, 2000, "kid_b"));

        Page<AdminTxProjection> page =
                txRepository.adminSearchWithMember(null, null, null, null, PageRequest.of(0, 20));

        assertThat(page.getContent())
                .extracting(AdminTxProjection::getUserName, AdminTxProjection::getAmount)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple("회원A", 1000L),
                        org.assertj.core.groups.Tuple.tuple("회원B", 2000L));
    }

    @Test
    void walletId_필터가_그대로_동작한다() {
        Member a = newMember("pc", "회원C");
        Member b = newMember("pd", "회원D");
        Wallet wa = walletRepository.save(new Wallet(a.getId()));
        Wallet wb = walletRepository.save(new Wallet(b.getId()));
        txRepository.save(new WalletTransaction(wa.getId(), TxType.CHARGE, 1000, 1000, "kid_c"));
        txRepository.save(new WalletTransaction(wb.getId(), TxType.CHARGE, 2000, 2000, "kid_d"));

        Page<AdminTxProjection> only =
                txRepository.adminSearchWithMember(wa.getId(), null, null, null, PageRequest.of(0, 20));

        assertThat(only.getContent()).hasSize(1);
        assertThat(only.getContent().get(0).getUserName()).isEqualTo("회원C");
    }

    @Test
    void 회원이_없어도_거래는_누락되지_않는다() {
        // 회원 없이 지갑만 있는 상황(탈퇴 등) — LEFT JOIN 이라 행은 남고 userName 만 null.
        Wallet orphan = walletRepository.save(new Wallet(9999L));
        txRepository.save(new WalletTransaction(orphan.getId(), TxType.ADJUST, 100, 100, "보상"));

        Page<AdminTxProjection> page =
                txRepository.adminSearchWithMember(null, null, null, null, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getUserId()).isEqualTo(9999L);
        assertThat(page.getContent().get(0).getUserName()).isNull();
    }

    @Test
    void type_필터가_그대로_동작한다() {
        Member a = newMember("pe", "회원E");
        Wallet w = walletRepository.save(new Wallet(a.getId()));
        txRepository.save(new WalletTransaction(w.getId(), TxType.CHARGE, 1000, 1000, "kid_e"));
        txRepository.save(new WalletTransaction(w.getId(), TxType.PAYMENT, 300, 700, "qr_e"));

        Page<AdminTxProjection> onlyPayment =
                txRepository.adminSearchWithMember(null, TxType.PAYMENT, null, null, PageRequest.of(0, 20));

        assertThat(onlyPayment.getContent()).hasSize(1);
        assertThat(onlyPayment.getContent().get(0).getRefUid()).isEqualTo("qr_e");
    }
}
