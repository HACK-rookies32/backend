package com.example.kidhackbackend.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.kidhackbackend.users.wallet.entity.Wallet;
import org.junit.jupiter.api.Test;

/** #191(QA #35) — 잔액 계산 오버플로우가 조용히 음수로 돌지 않고 예외로 끊기는지. */
class WalletOverflowTest {

    @Test
    void 정상_금액은_그대로_더해진다() {
        Wallet w = new Wallet(1L);
        w.credit(1_000_000_000L); // 거대하지만 정상 — VULN-ADMIN-2 표적은 통과되어야 한다
        assertThat(w.getBalance()).isEqualTo(1_000_000_000L);
    }

    @Test
    void credit_오버플로우는_ArithmeticException() {
        Wallet w = new Wallet(1L);
        w.credit(Long.MAX_VALUE);
        assertThatThrownBy(() -> w.credit(1L)).isInstanceOf(ArithmeticException.class);
    }

    @Test
    void debit_언더플로우도_ArithmeticException() {
        Wallet w = new Wallet(1L);
        w.credit(Long.MIN_VALUE);  // balance = MIN (0 + MIN 은 정상)
        assertThatThrownBy(() -> w.debit(1L)).isInstanceOf(ArithmeticException.class);
    }
}
