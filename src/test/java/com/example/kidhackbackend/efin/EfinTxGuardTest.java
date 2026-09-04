package com.example.kidhackbackend.efin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.kidhackbackend.global.crypto.NonceStore;
import com.example.kidhackbackend.global.sms.SmsPort;
import com.example.kidhackbackend.global.util.WeakHashUtil;
import com.example.kidhackbackend.users.efin.entity.AccountPassword;
import com.example.kidhackbackend.users.efin.repository.AccountPasswordRepository;
import com.example.kidhackbackend.users.efin.service.EfinOtpService;
import com.example.kidhackbackend.users.efin.service.EfinTxGuard;
import com.example.kidhackbackend.users.efin.service.TxSignatureVerifier;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * 거래 전 과정 검증 훅 — 정상 통과 + 결함 체이닝 재현.
 *
 * <p>2차 설계: 돈 이동은 계좌비밀번호 + OTP 둘 다 요구한다. 아래 테스트는 부모에게 계좌비밀번호(135790)를
 * 미리 설정해 두고, 각 결함(서명 skip·nonce 인메모리·소유주 대조·OTP 무제한)을 재현한다.
 */
@DataJpaTest
@Import({EfinTxGuard.class, EfinOtpService.class, TxSignatureVerifier.class, NonceStore.class,
        EfinTxGuardTest.NoOpSmsConfig.class})
class EfinTxGuardTest {

    private static final String ACCT_PW = "135790";

    @TestConfiguration
    static class NoOpSmsConfig {
        @Bean
        SmsPort smsPort() {
            return (to, code) -> { /* no-op */ };
        }
    }

    @Autowired EfinTxGuard guard;
    @Autowired EfinOtpService otpService;
    @Autowired TxSignatureVerifier signatureVerifier;
    @Autowired MemberRepository memberRepository;
    @Autowired AccountPasswordRepository accountPasswordRepository;

    private Member persistMember(String loginId, String name, Role role) {
        Member m = new Member();
        m.setLoginId(loginId);
        m.setUsername(name);
        m.setPassword("pw");
        m.setPhoneNumber("010-0000-" + (1000 + Math.abs(loginId.hashCode()) % 9000));
        m.setBirthDate(LocalDate.of(1990, 1, 1));
        m.setRole(role);
        Member saved = memberRepository.save(m);
        if (role == Role.PARENT) {
            // 거래 관문이 계좌비밀번호를 요구하므로 부모에게 미리 설정해 둔다.
            accountPasswordRepository.save(new AccountPassword(saved.getId(), WeakHashUtil.md5(ACCT_PW)));
        }
        return saved;
    }

    private String freshOtp(Member caller) {
        return otpService.issue(caller).code();
    }

    @Test
    void 정상_거래는_통과한다() {
        Member parent = persistMember("p1", "부모", Role.PARENT);
        Member child = persistMember("c1", "자녀", Role.CHILD);
        String nonce = "n-ok";
        String sig = signatureVerifier.sign(5000L, child.getId(), nonce);

        assertThatCode(() -> guard.verifyTransfer(parent, child.getId(), 5000L,
                freshOtp(parent), sig, nonce, "자녀", ACCT_PW)).doesNotThrowAnyException();
    }

    @Test
    void VULN_EFIN_2_서명_필드를_빼면_검증이_스킵된다() {
        Member parent = persistMember("p2", "부모", Role.PARENT);
        Member child = persistMember("c2", "자녀", Role.CHILD);

        // signature = null → 서명 검증 통째로 skip. otp·nonce·계좌비번만 맞으면 통과.
        assertThatCode(() -> guard.verifyTransfer(parent, child.getId(), 5000L,
                freshOtp(parent), null, "n-skip", null, ACCT_PW)).doesNotThrowAnyException();
    }

    @Test
    void 서명이_있는데_변조되면_거부된다() {
        Member parent = persistMember("p3", "부모", Role.PARENT);
        Member child = persistMember("c3", "자녀", Role.CHILD);
        String badSig = signatureVerifier.sign(5000L, child.getId(), "n3"); // 5000 으로 서명

        assertThatThrownBy(() -> guard.verifyTransfer(parent, child.getId(), 50000L,
                freshOtp(parent), badSig, "n3", null, ACCT_PW)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void VULN_EFIN_3_nonce_저장소가_인메모리라_재시작시_재사용된다() {
        Member parent = persistMember("p4", "부모", Role.PARENT);
        Member child = persistMember("c4", "자녀", Role.CHILD);

        guard.verifyTransfer(parent, child.getId(), 5000L, freshOtp(parent), null, "n-reuse", null, ACCT_PW);
        assertThatThrownBy(() -> guard.verifyTransfer(parent, child.getId(), 5000L,
                freshOtp(parent), null, "n-reuse", null, ACCT_PW)).isInstanceOf(IllegalArgumentException.class);

        // 저장소가 인메모리라 재시작(=새 인스턴스)하면 같은 nonce 가 다시 통과 → VULN-WALLET-16 과 체인.
        NonceStore afterRestart = new NonceStore(60);
        assertThat(afterRestart.tryConsume("n-reuse", System.currentTimeMillis())).isTrue();
    }

    @Test
    void VULN_EFIN_4_소유주_이름만_맞으면_페어링_무관하게_통과된다() {
        Member parent = persistMember("p5", "부모", Role.PARENT);
        Member strangerChild = persistMember("c5", "남의자녀", Role.CHILD);

        // accountHolder 에 대상 자녀 이름만 넣으면(페어링 검증 없음) 통과 → 송금 IDOR(VULN-WALLET-7) 관통.
        assertThatCode(() -> guard.verifyTransfer(parent, strangerChild.getId(), 5000L,
                freshOtp(parent), null, "n5", "남의자녀", ACCT_PW)).doesNotThrowAnyException();
    }

    @Test
    void 소유주_이름이_틀리면_거부된다() {
        Member parent = persistMember("p6", "부모", Role.PARENT);
        Member child = persistMember("c6", "자녀", Role.CHILD);

        assertThatThrownBy(() -> guard.verifyTransfer(parent, child.getId(), 5000L,
                freshOtp(parent), null, "n6", "엉뚱한이름", ACCT_PW)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 계좌비밀번호가_틀리면_거부된다() {
        Member parent = persistMember("p7", "부모", Role.PARENT);
        Member child = persistMember("c7", "자녀", Role.CHILD);

        assertThatThrownBy(() -> guard.verifyTransfer(parent, child.getId(), 5000L,
                freshOtp(parent), null, "n7", null, "000000")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 계좌비밀번호가_없으면_거부된다() {
        Member parent = persistMember("p8", "부모", Role.PARENT);
        Member child = persistMember("c8", "자녀", Role.CHILD);

        assertThatThrownBy(() -> guard.verifyTransfer(parent, child.getId(), 5000L,
                freshOtp(parent), null, "n8", null, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
