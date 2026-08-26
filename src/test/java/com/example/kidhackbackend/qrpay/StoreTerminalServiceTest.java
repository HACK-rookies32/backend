package com.example.kidhackbackend.qrpay;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.users.qrpay.dto.TerminalResponse;
import com.example.kidhackbackend.users.qrpay.entity.StoreTerminal;
import com.example.kidhackbackend.users.qrpay.entity.TerminalStatus;
import com.example.kidhackbackend.users.qrpay.repository.StoreTerminalRepository;
import com.example.kidhackbackend.users.qrpay.service.StoreTerminalService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * 매장단말 토큰 발급·검증(No.44 선행) 슬라이스 테스트 — 정상 발급/검증 + 취약점(폐기 미확인) 재현.
 */
@DataJpaTest
@Import({StoreTerminalService.class})
class StoreTerminalServiceTest {

    @Autowired StoreTerminalService service;
    @Autowired StoreTerminalRepository repository;

    @Test
    void 단말발급_예측불가_토큰과_함께_ACTIVE로_생성된다() {
        TerminalResponse res = service.issue("GS25");

        assertThat(res.storeName()).isEqualTo("GS25");
        assertThat(res.status()).isEqualTo("ACTIVE");
        assertThat(res.terminalToken()).startsWith("term_");
        assertThat(res.terminalToken().length()).isGreaterThan(40); // opaque, 충분히 긴 난수
    }

    @Test
    void 유효토큰_검증시_해당_단말을_반환한다() {
        String token = service.issue("CU").terminalToken();

        StoreTerminal terminal = service.verify(token);

        assertThat(terminal.getStoreName()).isEqualTo("CU");
        assertThat(terminal.getStatus()).isEqualTo(TerminalStatus.ACTIVE);
    }

    @Test
    void 토큰이_없거나_존재하지_않으면_거부된다() {
        assertThatThrownBy(() -> service.verify("  "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.verify("term_nonexistent"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void VULN_WALLET_12_폐기된_단말토큰도_검증을_통과한다() {
        String token = service.issue("GS25").terminalToken();
        // 단말을 폐기(REVOKED) 처리
        StoreTerminal terminal = repository.findByTerminalToken(token).orElseThrow();
        terminal.revoke();
        repository.save(terminal);

        // 취약: 폐기 상태를 확인하지 않아 REVOKED 토큰으로도 인증이 통과된다.
        StoreTerminal authed = service.verify(token);
        assertThat(authed.getStatus()).isEqualTo(TerminalStatus.REVOKED);
    }
}
