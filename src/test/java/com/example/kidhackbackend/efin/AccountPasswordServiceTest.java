package com.example.kidhackbackend.efin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.kidhackbackend.users.efin.entity.AccountPasswordHistory;
import com.example.kidhackbackend.users.efin.repository.AccountPasswordHistoryRepository;
import com.example.kidhackbackend.users.efin.service.AccountPasswordService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/** 계좌비밀번호 설정·변경·재사용 방지 + [VULN-EFIN-6] 이력 평문 저장 재현. */
@DataJpaTest
@Import(AccountPasswordService.class)
class AccountPasswordServiceTest {

    private static final long MEMBER = 42L;

    @Autowired AccountPasswordService service;
    @Autowired AccountPasswordHistoryRepository historyRepository;

    @Test
    void 설정한_비밀번호로_본인확인이_된다() {
        service.set(MEMBER, "135790");
        assertThat(service.matches(MEMBER, "135790")).isTrue();
        assertThat(service.matches(MEMBER, "000000")).isFalse();
    }

    @Test
    void 변경시_기존_비밀번호가_틀리면_거부() {
        service.set(MEMBER, "135790");
        assertThatThrownBy(() -> service.change(MEMBER, "999999", "246802"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 과거에_쓴_비밀번호로는_바꿀_수_없다() {
        service.set(MEMBER, "135790");
        service.change(MEMBER, "135790", "246802");
        // 다시 최초 비밀번호(135790)로 되돌리려 하면 이력 대조로 거부.
        assertThatThrownBy(() -> service.change(MEMBER, "246802", "135790"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void VULN_EFIN_6_이력이_평문으로_저장된다() {
        service.set(MEMBER, "135790");
        service.change(MEMBER, "135790", "246802");

        List<AccountPasswordHistory> history = historyRepository.findByMemberId(MEMBER);
        // 두 비밀번호가 모두 평문 그대로 남아 있다 → DB 탈취 시 과거 비번 전량 노출.
        assertThat(history).extracting(AccountPasswordHistory::getPassword)
                .containsExactlyInAnyOrder("135790", "246802");
    }
}