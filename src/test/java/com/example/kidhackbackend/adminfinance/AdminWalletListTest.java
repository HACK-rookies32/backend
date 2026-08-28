package com.example.kidhackbackend.adminfinance;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.AdminWalletProjection;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.wallet.entity.Wallet;
import com.example.kidhackbackend.users.wallet.repository.WalletRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * 관리자 회원 지갑 목록 슬라이스 테스트 (프론트 3차 2번).
 *
 * <p>핵심은 <b>지갑이 없는 회원도 목록에 나오는가</b>다. 이 목록은 잔액 조정의 진입점이라,
 * 한 번도 충전받지 않은 자녀가 빠지면 조정 대상으로 고를 수 없어 기능이 반쪽이 된다.
 */
@DataJpaTest
class AdminWalletListTest {

    @Autowired MemberRepository memberRepository;
    @Autowired WalletRepository walletRepository;

    private Member member(String loginId, String username, Role role) {
        Member m = new Member();
        m.setLoginId(loginId);
        m.setUsername(username);
        m.setNickname(loginId);
        m.setPassword("pw");
        m.setPhoneNumber("01000000000");
        m.setBirthDate(LocalDate.of(1990, 1, 1));
        m.setRole(role);
        return memberRepository.save(m);
    }

    private Page<AdminWalletProjection> search(String role, String keyword) {
        return memberRepository.searchWalletsWithBalance(role, keyword, PageRequest.of(0, 20));
    }

    @Test
    void 지갑이_있는_회원은_잔액이_함께_나온다() {
        Member p = member("wp1", "김보호", Role.PARENT);
        Wallet w = walletRepository.save(new Wallet(p.getId()));
        w.credit(35000);
        walletRepository.save(w);

        Page<AdminWalletProjection> page = search(null, null);

        assertThat(page.getContent()).hasSize(1);
        AdminWalletProjection row = page.getContent().get(0);
        assertThat(row.getUserId()).isEqualTo(p.getId());
        assertThat(row.getUserName()).isEqualTo("김보호");
        assertThat(row.getRole()).isEqualTo("PARENT");
        assertThat(row.getBalance()).isEqualTo(35000);
    }

    @Test
    void 지갑이_없는_회원도_잔액_0으로_포함된다() {
        // 이 기능의 존재 이유 — 조정 대상으로 고를 수 있어야 한다.
        member("wc1", "김자녀", Role.CHILD);

        Page<AdminWalletProjection> page = search(null, null);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getUserName()).isEqualTo("김자녀");
        assertThat(page.getContent().get(0).getBalance()).isZero();
        assertThat(page.getContent().get(0).getUpdatedAt()).isNull();
    }

    @Test
    void 관리자_계정은_목록에서_제외된다() {
        member("wp2", "부모", Role.PARENT);
        member("wa1", "슈퍼관리자", Role.SUPER_ADMIN);
        member("wa2", "일반관리자", Role.GENERAL_ADMIN);

        Page<AdminWalletProjection> page = search(null, null);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getUserName()).isEqualTo("부모");
    }

    @Test
    void role_필터로_자녀만_조회된다() {
        member("wp3", "부모A", Role.PARENT);
        member("wc3", "자녀A", Role.CHILD);

        Page<AdminWalletProjection> onlyChild = search("CHILD", null);

        assertThat(onlyChild.getContent()).hasSize(1);
        assertThat(onlyChild.getContent().get(0).getUserName()).isEqualTo("자녀A");
    }

    @Test
    void keyword_로_실명_부분검색이_된다() {
        member("wp4", "김보호", Role.PARENT);
        member("wp5", "이보호", Role.PARENT);
        member("wc5", "박자녀", Role.CHILD);

        assertThat(search(null, "보호").getContent()).hasSize(2);
        assertThat(search(null, "김").getContent()).hasSize(1);
        assertThat(search(null, "없는이름").getContent()).isEmpty();
    }

    @Test
    void 페이징_메타가_유지된다() {
        for (int i = 0; i < 5; i++) {
            member("wpg" + i, "회원" + i, Role.PARENT);
        }

        Page<AdminWalletProjection> first =
                memberRepository.searchWalletsWithBalance(null, null, PageRequest.of(0, 2));

        assertThat(first.getContent()).hasSize(2);
        assertThat(first.getTotalElements()).isEqualTo(5);
        assertThat(first.getTotalPages()).isEqualTo(3);
    }
}
