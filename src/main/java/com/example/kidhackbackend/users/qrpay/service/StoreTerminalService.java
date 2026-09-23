package com.example.kidhackbackend.users.qrpay.service;

import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.users.qrpay.dto.TerminalResponse;
import com.example.kidhackbackend.users.qrpay.entity.StoreTerminal;
import com.example.kidhackbackend.users.qrpay.repository.StoreTerminalRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 매장 단말 토큰 발급·검증 — QR결제 No.44 선행. PG 웹훅(No.36) 서명검증 체계를 미러링하되,
 * 단말은 재사용되는 장기 자격증명이라 등록형 opaque 토큰으로 발급·대조한다.
 *
 * <p>발급(issue): 예측 불가 토큰과 함께 ACTIVE 단말 생성.
 * 검증(verify): 요청의 X-Store-Terminal-Token으로 단말을 특정(No.44가 결제 승인 전에 호출).
 */
@Service
@RequiredArgsConstructor
public class StoreTerminalService {

    private final StoreTerminalRepository storeTerminalRepository;

    /** 매장 단말 등록 + 토큰 발급. 토큰은 예측 불가능한 opaque 문자열. */
    @Transactional
    public TerminalResponse issue(String storeName) {
        String token = "term_" + UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        StoreTerminal terminal = storeTerminalRepository.save(new StoreTerminal(token, storeName));
        return TerminalResponse.from(terminal);
    }

    /**
     * 매장 단말 인증: X-Store-Terminal-Token으로 단말을 조회해 반환한다. (No.44 결제 승인 전 호출)
     *
     * @param token 요청 헤더의 매장단말 토큰
     */
    @Transactional(readOnly = true)
    public StoreTerminal verify(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("매장단말 토큰이 필요합니다.");
        }
        StoreTerminal terminal = storeTerminalRepository.findByTerminalToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("유효하지 않은 매장단말 토큰입니다."));

        // [VULN-WALLET-12] 단말 토큰 검증 부실 — 폐기(REVOKED) 상태를 보지 않는다. 판정: 조건부
        //   바로 위 findByTerminalToken 은 "이 토큰이 DB 에 있는가"만 본다. 그 뒤에 있어야 할
        //   terminal.getStatus() != ACTIVE 검사가 통째로 빠져 있어, 폐기 처리한 토큰도 그대로
        //   인증을 통과한다. 토큰을 회수해도 실제로는 회수되지 않는 구조다.
        //   → 유출되었거나 이미 폐기한 단말 토큰으로 결제 승인(No.44)을 계속 호출할 수 있다.
        //
        //   판정이 '취약'이 아니라 '조건부'인 이유(2026-09-07 모의해킹):
        //     단말을 REVOKED 상태로 바꾸는 API 자체가 아직 없어서, 실서버에서 "폐기된 토큰으로
        //     결제가 되더라"를 재현할 방법이 없었다. 결함은 코드에 분명히 존재하지만 현재
        //     도달 경로가 없어 실증하지 못한 것이다. 폐기 API 가 추가되는 순간 바로 성립한다.
        //   (덧붙여 토큰을 해시 없이 평문으로 저장하므로 DB 가 유출되면 그대로 재사용 가능하다.)
        //   방어: if (terminal.getStatus() != TerminalStatus.ACTIVE) throw ...; + 토큰 해시 저장.

        return terminal;
    }

    /** 등록 단말 목록(관리자 조회용). */
    @Transactional(readOnly = true)
    public List<TerminalResponse> list() {
        return storeTerminalRepository.findAllByOrderByIdDesc().stream()
                .map(TerminalResponse::from)
                .toList();
    }
}
