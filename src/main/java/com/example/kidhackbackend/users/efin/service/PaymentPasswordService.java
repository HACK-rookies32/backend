package com.example.kidhackbackend.users.efin.service;

import com.example.kidhackbackend.global.util.WeakHashUtil;
import com.example.kidhackbackend.users.efin.entity.PaymentPassword;
import com.example.kidhackbackend.users.efin.entity.PaymentPasswordHistory;
import com.example.kidhackbackend.users.efin.repository.PaymentPasswordHistoryRepository;
import com.example.kidhackbackend.users.efin.repository.PaymentPasswordRepository;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 비밀번호 체계 — 설정·변경·본인확인 + 이전 비밀번호 재사용 방지.
 *
 * <p>로그인 비밀번호와 분리된 4자리 결제 PIN 이다. 접근매체(카드) 발급 시 최초 설정하고, 변경 시에는
 * 앱 로그인 비밀번호로 본인확인한다. 과거에 쓰던 비밀번호로는 바꿀 수 없다(이력 대조).
 *
 * <p>⚠️ [VULN-EFIN-5] 결제 비밀번호를 salt 없는 MD5 로 저장(4자리라 즉시 역산).
 * ⚠️ [VULN-EFIN-6] 재사용 방지용 이력을 <b>평문</b>으로 쌓아 DB 탈취 시 과거 비밀번호가 전부 노출된다.
 */
@Service
@RequiredArgsConstructor
public class PaymentPasswordService {

    private final PaymentPasswordRepository paymentPasswordRepository;
    private final PaymentPasswordHistoryRepository historyRepository;
    private final MemberRepository memberRepository;

    /**
     * 접근매체 발급(카드 등록) 흐름의 최초 설정.
     *
     * <p>직전 실명확인이 본인확인을 대신하므로 로그인 비밀번호 step-up 을 요구하지 않는다. 이미 있으면 예외.
     */
    @Transactional
    public void setup(Long memberId, String rawPassword) {
        if (paymentPasswordRepository.existsByMemberId(memberId)) {
            throw new IllegalArgumentException("이미 결제 비밀번호가 설정되어 있습니다.");
        }
        paymentPasswordRepository.save(new PaymentPassword(memberId, WeakHashUtil.md5(rawPassword)));
        historyRepository.save(new PaymentPasswordHistory(memberId, rawPassword)); // [VULN-EFIN-6] 평문
    }

    /**
     * 결제 비밀번호 변경. 앱 로그인 비밀번호 본인확인 + 기존 비밀번호 확인 + 과거 재사용 차단.
     */
    @Transactional
    public void change(Long memberId, String currentRaw, String newRaw, String loginPassword) {
        requireLoginPassword(memberId, loginPassword);
        PaymentPassword current = paymentPasswordRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("결제 비밀번호가 설정되어 있지 않습니다."));

        // 본인확인: 기존 결제 비밀번호 일치.
        if (!current.matches(WeakHashUtil.md5(currentRaw))) {
            throw new IllegalArgumentException("기존 결제 비밀번호가 일치하지 않습니다.");
        }
        // 이전 비밀번호 재사용 방지: 평문 이력과 대조([VULN-EFIN-6] — 대조 대상이 평문).
        boolean reused = historyRepository.findByMemberId(memberId).stream()
                .anyMatch(h -> h.getPassword().equals(newRaw));
        if (reused) {
            throw new IllegalArgumentException("과거에 사용한 비밀번호는 다시 사용할 수 없습니다.");
        }

        current.changeHash(WeakHashUtil.md5(newRaw));
        historyRepository.save(new PaymentPasswordHistory(memberId, newRaw)); // [VULN-EFIN-6] 평문
    }

    /** 결제 비밀번호가 설정되어 있는지. */
    @Transactional(readOnly = true)
    public boolean exists(Long memberId) {
        return paymentPasswordRepository.existsByMemberId(memberId);
    }

    /** 결제 비밀번호 일치 여부(거래 승인·카드 발급 시 본인확인 용도). */
    @Transactional(readOnly = true)
    public boolean matches(Long memberId, String rawPassword) {
        return paymentPasswordRepository.findByMemberId(memberId)
                .map(pp -> pp.matches(WeakHashUtil.md5(rawPassword)))
                .orElse(false);
    }

    /**
     * 앱 로그인 비밀번호로 본인확인.
     *
     * <p>로그인 비밀번호는 평문 저장([VULN-1])이라 그대로 비교한다 — 본인확인 절차는 실제로 존재하지만
     * 그 비교 대상(저장 비번)이 평문이라는 점이 기존 취약점 그대로다.
     */
    private void requireLoginPassword(Long memberId, String loginPassword) {
        if (loginPassword == null || loginPassword.isBlank()) {
            throw new IllegalArgumentException("본인확인용 로그인 비밀번호가 필요합니다.");
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
        if (!member.getPassword().equals(loginPassword)) {
            throw new IllegalArgumentException("로그인 비밀번호가 일치하지 않습니다.");
        }
    }
}
