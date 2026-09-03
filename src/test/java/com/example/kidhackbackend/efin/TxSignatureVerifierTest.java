package com.example.kidhackbackend.efin;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kidhackbackend.users.efin.service.TxSignatureVerifier;
import org.junit.jupiter.api.Test;

/** 거래정보 전자서명 검증 — 정상 서명 통과 + 금액 변조 시 서명 불일치. */
class TxSignatureVerifierTest {

    private final TxSignatureVerifier verifier = new TxSignatureVerifier("test-secret");

    @Test
    void 정상_서명은_검증을_통과한다() {
        String sig = verifier.sign(5000L, 7L, "nonce-1");
        assertThat(verifier.verify(5000L, 7L, "nonce-1", sig)).isTrue();
    }

    @Test
    void 금액이_변조되면_서명이_깨진다() {
        String sig = verifier.sign(5000L, 7L, "nonce-1");
        // 중간자가 금액만 50000 으로 바꾸면 서명은 그대로 → 불일치.
        assertThat(verifier.verify(50000L, 7L, "nonce-1", sig)).isFalse();
    }

    @Test
    void 서명이_없으면_검증_실패로_취급() {
        assertThat(verifier.verify(5000L, 7L, "nonce-1", null)).isFalse();
    }
}