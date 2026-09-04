package com.example.kidhackbackend.users.efin.service;

import com.example.kidhackbackend.global.crypto.NonceStore;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.efin.repository.AccountPasswordRepository;
import com.example.kidhackbackend.global.util.WeakHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 거래 전 과정(요청→승인→처리) 보안 검증 훅. 송금·회수·QR결제 진입부에서 호출된다.
 *
 * <p>4가지를 검사한다: 2차 인증(OTP), 전자서명(무결성), nonce(재사용 방지), 소유주 검증.
 * 각각에 <b>의도적 결함</b>을 심어 기존 취약점과 체이닝된다.
 *
 * <ul>
 *   <li>[VULN-EFIN-1b] OTP 실패 시도 무제한(브루트포스) — 로그인 VULN-4 계열</li>
 *   <li>[VULN-EFIN-2] {@code signature} 가 없으면(null/blank) 서명 검증을 통째로 skip
 *       — 웹훅 VULN-WALLET-4 · QR VULN-WALLET-15 와 동형(필드 빼면 우회)</li>
 *   <li>[VULN-EFIN-3] nonce 재사용 저장소가 <b>인메모리</b>({@link NonceStore}) — 서버 재시작 시 소실되어
 *       같은 nonce 재사용 가능. QR 멱등 미검증(VULN-WALLET-16)과 겹쳐 재시작 후 재결제로 이어진다</li>
 *   <li>[VULN-EFIN-4] 소유주 검증이 {@code accountHolder}(요청 body 값)와 대상 이름만 대조하고
 *       <b>부모-자녀 페어링은 확인하지 않는다</b> — 송금 IDOR(VULN-WALLET-7)를 그대로 관통</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class EfinTxGuard {

    private final EfinOtpService otpService;
    private final TxSignatureVerifier signatureVerifier;
    private final NonceStore nonceStore;
    private final MemberRepository memberRepository;
    private final AccountPasswordRepository accountPasswordRepository;

    /**
     * 송금/회수 거래 검증. amount·childId 를 서명·소유주 검증 대상으로 삼는다.
     */
    public void verifyTransfer(Member caller, Long childId, long amount,
                               String otp, String signature, String nonce, String accountHolder,
                               String accountPassword) {
        // 1) 전자서명 무결성 — [VULN-EFIN-2] 서명 필드가 없으면 검증을 건너뛴다.
        if (signature != null && !signature.isBlank()) {
            if (!signatureVerifier.verify(amount, childId, nonce, signature)) {
                throw new IllegalArgumentException("거래정보 서명이 유효하지 않습니다(변조 의심).");
            }
        }

        // 2) 재사용 방지 nonce — [VULN-EFIN-3] 인메모리 저장(재시작 시 소실).
        if (nonce == null || nonce.isBlank()) {
            throw new IllegalArgumentException("재사용 방지 값(nonce)이 필요합니다.");
        }
        if (!nonceStore.tryConsume(nonce, System.currentTimeMillis())) {
            throw new IllegalArgumentException("이미 처리된 거래입니다(재전송).");
        }

        // 3) 소유주 검증 — [VULN-EFIN-4] 이름만 대조, 페어링은 확인하지 않음(IDOR 관통).
        if (accountHolder != null && !accountHolder.isBlank()) {
            String targetName = memberRepository.findById(childId)
                    .map(Member::getUsername).orElse(null);
            if (targetName == null || !targetName.equals(accountHolder)) {
                throw new IllegalArgumentException("대상 계좌의 소유주 정보가 일치하지 않습니다.");
            }
        }

        // 4) 계좌비밀번호(거래 비밀번호) 본인확인 — 필수. 저장은 salt 없는 MD5 그대로([VULN-EFIN-5]).
        requireAccountPassword(caller.getId(), accountPassword);

        // 5) 2차 인증(OTP) — [VULN-EFIN-1b] 실패 시도 무제한. 마지막에 소비.
        otpService.consume(caller.getId(), caller.getPhoneNumber(), otp);
    }

    /**
     * 충전 거래 검증(prepare 단계). 계좌비밀번호 + OTP 필수 + nonce 재사용 방지.
     * 자녀 대상이 아니라 refId=0, 소유주 검증은 없다. 서명은 있으면 amount:0:nonce 로 검증(없으면 skip, EFIN-2).
     */
    public void verifyCharge(Member caller, long amount,
                             String otp, String signature, String nonce, String accountPassword) {
        if (signature != null && !signature.isBlank()
                && !signatureVerifier.verify(amount, 0L, nonce, signature)) {
            throw new IllegalArgumentException("충전정보 서명이 유효하지 않습니다(변조 의심).");
        }
        if (nonce == null || nonce.isBlank()) {
            throw new IllegalArgumentException("재사용 방지 값(nonce)이 필요합니다.");
        }
        if (!nonceStore.tryConsume(nonce, System.currentTimeMillis())) {
            throw new IllegalArgumentException("이미 처리된 충전입니다(재전송).");
        }
        requireAccountPassword(caller.getId(), accountPassword);
        otpService.consume(caller.getId(), caller.getPhoneNumber(), otp);
    }

    /** 계좌비밀번호 본인확인. 미설정이면 먼저 설정하도록 안내. [VULN-EFIN-5] salt 없는 MD5 대조. */
    private void requireAccountPassword(Long memberId, String accountPassword) {
        if (accountPassword == null || accountPassword.isBlank()) {
            throw new IllegalArgumentException("계좌비밀번호가 필요합니다.");
        }
        var stored = accountPasswordRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("계좌비밀번호를 먼저 설정하세요."));
        if (!stored.matches(WeakHashUtil.md5(accountPassword))) {
            throw new IllegalArgumentException("계좌비밀번호가 올바르지 않습니다.");
        }
    }

    /**
     * QR 결제 거래 검증(매장 단말 경로). 단말엔 회원 세션이 없어 서명·nonce 만 검사한다.
     * qrToken 을 서명·재사용 방지 대상으로 삼는다.
     */
    public void verifyQrPay(String qrToken, long amount, String signature, String nonce) {
        // 전자서명 — [VULN-EFIN-2] 서명 없으면 skip.
        if (signature != null && !signature.isBlank()) {
            // refId 자리에 qrToken 해시 대신 0 고정(단말은 childId 를 모른다) — payload 는 amount:0:nonce.
            if (!signatureVerifier.verify(amount, 0L, nonce, signature)) {
                throw new IllegalArgumentException("결제정보 서명이 유효하지 않습니다(변조 의심).");
            }
        }
        // nonce — [VULN-EFIN-3] 인메모리(재시작 시 소실) → VULN-WALLET-16 과 체인.
        if (nonce != null && !nonce.isBlank()
                && !nonceStore.tryConsume(nonce, System.currentTimeMillis())) {
            throw new IllegalArgumentException("이미 처리된 결제입니다(재전송).");
        }
    }
}