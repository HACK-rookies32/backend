package com.example.kidhackbackend.efin;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.kidhackbackend.users.efin.dto.IdentityVerifyRequest;
import com.example.kidhackbackend.users.efin.service.MockIdentityVerifier;
import org.junit.jupiter.api.Test;

/** 실명확인 mock 진위확인 — 규칙 충족 시 통과 + [VULN-EFIN-7] 조건에 맞는 임의 값 우회. */
class MockIdentityVerifierTest {

    private final MockIdentityVerifier verifier = new MockIdentityVerifier();

    @Test
    void 주민번호_앞자리와_생년월일이_맞으면_통과() {
        var req = new IdentityVerifyRequest("홍길동", "900101", "RESIDENT", "900101-1234567", "2015-03-20");
        assertThat(verifier.verify(req)).isTrue();
    }

    @Test
    void 생년월일과_주민번호_앞자리가_다르면_실패() {
        var req = new IdentityVerifyRequest("홍길동", "900101", "RESIDENT", "880101-1234567", "2015-03-20");
        assertThat(verifier.verify(req)).isFalse();
    }

    @Test
    void VULN_EFIN_7_실제_대조없이_지어낸_값으로_통과된다() {
        // 존재하지 않는 사람이라도 형식·자기일치만 맞으면 통과 → 진위확인 우회.
        var forged = new IdentityVerifyRequest("없는사람", "010101", "RESIDENT", "010101-4567890", "2099-01-01");
        assertThat(verifier.verify(forged)).isTrue();
    }
}