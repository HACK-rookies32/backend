package com.example.kidhackbackend.users.efin.service;

import com.example.kidhackbackend.global.util.WeakHashUtil;
import com.example.kidhackbackend.users.efin.entity.AccountPassword;
import com.example.kidhackbackend.users.efin.entity.AccountPasswordHistory;
import com.example.kidhackbackend.users.efin.repository.AccountPasswordHistoryRepository;
import com.example.kidhackbackend.users.efin.repository.AccountPasswordRepository;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 전자금융 별도 비밀번호(계좌비밀번호) 체계 — 설정·변경·본인확인 + 이전 비밀번호 재사용 방지.
 *
 * <p>로그인 비밀번호와 분리된 거래용 비밀번호다. 변경 시 기존 계좌비밀번호로 본인확인하고,
 * 과거에 쓰던 비밀번호로는 바꿀 수 없다(이력 대조).
 *
 * <p>⚠️ [VULN-EFIN-5] 계좌비밀번호를 salt 없는 MD5 로 저장(6자리라 즉시 역산).
 * ⚠️ [VULN-EFIN-6] 재사용 방지용 이력을 <b>평문</b>으로 쌓아 DB 탈취 시 과거 비밀번호가 전부 노출된다.
 */
@Service
@RequiredArgsConstructor
public class AccountPasswordService {

    private final AccountPasswordRepository accountPasswordRepository;
    private final AccountPasswordHistoryRepository historyRepository;
    private final MemberRepository memberRepository;

    /** 계좌비밀번호 최초 설정. 이미 있으면 예외(변경은 change 로). 앱 로그인 비밀번호로 본인확인. */
    @Transactional
    public void set(Long memberId, String rawPassword, String loginPassword) {
        requireLoginPassword(memberId, loginPassword);
        if (accountPasswordRepository.existsByMemberId(memberId)) {
            throw new IllegalArgumentException("이미 계좌비밀번호가 설정되어 있습니다. 변경을 이용하세요.");
        }
        accountPasswordRepository.save(new AccountPassword(memberId, WeakHashUtil.md5(rawPassword)));
        historyRepository.save(new AccountPasswordHistory(memberId, rawPassword)); // [VULN-EFIN-6] 평문
    }

    /**
     * 계좌비밀번호 변경. 앱 로그인 비밀번호 본인확인 + 기존 계좌비밀번호 확인 + 과거 재사용 차단.
     */
    @Transactional
    public void change(Long memberId, String currentRaw, String newRaw, String loginPassword) {
        requireLoginPassword(memberId, loginPassword);
        AccountPassword current = accountPasswordRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("계좌비밀번호가 설정되어 있지 않습니다."));

        // 본인확인: 기존 계좌비밀번호 일치.
        if (!current.matches(WeakHashUtil.md5(currentRaw))) {
            throw new IllegalArgumentException("기존 계좌비밀번호가 일치하지 않습니다.");
        }
        // 이전 비밀번호 재사용 방지: 평문 이력과 대조([VULN-EFIN-6] — 대조 대상이 평문).
        boolean reused = historyRepository.findByMemberId(memberId).stream()
                .anyMatch(h -> h.getPassword().equals(newRaw));
        if (reused) {
            throw new IllegalArgumentException("과거에 사용한 비밀번호는 다시 사용할 수 없습니다.");
        }

        current.changeHash(WeakHashUtil.md5(newRaw));
        historyRepository.save(new AccountPasswordHistory(memberId, newRaw)); // [VULN-EFIN-6] 평문
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

    /** 계좌비밀번호 일치 여부(거래 승인 시 본인확인 용도). */
    @Transactional(readOnly = true)
    public boolean matches(Long memberId, String rawPassword) {
        return accountPasswordRepository.findByMemberId(memberId)
                .map(ap -> ap.matches(WeakHashUtil.md5(rawPassword)))
                .orElse(false);
    }
}