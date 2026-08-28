package com.example.kidhackbackend.users.qrpay.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.qrpay.dto.QrApproveRequest;
import com.example.kidhackbackend.users.qrpay.dto.QrApproveResponse;
import com.example.kidhackbackend.users.qrpay.dto.TerminalAuthResponse;
import com.example.kidhackbackend.users.qrpay.entity.StoreTerminal;
import com.example.kidhackbackend.users.qrpay.service.QrApprovalService;
import com.example.kidhackbackend.users.qrpay.service.StoreTerminalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 매장 단말 인증 경로 — {@code /api/qrpay/terminal/**}. 앱 JWT가 아니라 {@code X-Store-Terminal-Token}
 * 헤더로 단말을 인증한다(시큐리티는 permitAll, 진위는 토큰 검증으로 담보). No.44 결제 승인이 이 경로/검증을 사용한다.
 *
 * <p>본 컨트롤러의 {@code /me}는 단말 토큰이 유효한지 확인하는 용도(발급 후 단말 프로비저닝 점검).
 */
@Tag(name = "16. QR결제 - 매장 단말",
        description = "매장 단말이 QR을 스캔해 결제 승인 · 단말 토큰 확인. **앱 JWT 불필요** — `X-Store-Terminal-Token` 헤더로 인증(경로 permitAll, 진위는 토큰 검증).")
@RestController
@RequestMapping("/api/qrpay/terminal")
@RequiredArgsConstructor
public class QrTerminalController {

    private final StoreTerminalService storeTerminalService;
    private final QrApprovalService qrApprovalService;

    /** 단말 토큰 인증 확인: 헤더의 토큰으로 단말을 검증해 매장 정보를 돌려준다. */
    @Operation(summary = "매장 단말 인증 확인",
            description = """
                    `X-Store-Terminal-Token` 을 검증하고 인증된 매장 정보를 반환한다. 단말 프로비저닝 점검용.
                    토큰 없음/공백 → 400, DB 에 없는 토큰 → 404.

                    ⚠️ [VULN-WALLET-12] 폐기(REVOKED) 단말 토큰을 걸러내지 않는다(토큰이 DB 에 있기만 하면 통과) + 토큰 평문 저장.""")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<TerminalAuthResponse>> me(
            @Parameter(in = ParameterIn.HEADER, name = "X-Store-Terminal-Token", required = true,
                    description = "매장 단말 인증 토큰(발급은 태그 17)")
            @RequestHeader(value = "X-Store-Terminal-Token", required = false) String terminalToken) {
        StoreTerminal terminal = storeTerminalService.verify(terminalToken);
        return ResponseEntity.ok(ApiResponse.success(TerminalAuthResponse.from(terminal)));
    }

    /** QR 스캔 결제 승인 — No.44. 단말이 스캔한 QR과 금액으로 결제를 확정한다. */
    @Operation(summary = "QR 스캔 결제 승인 (매장 단말)",
            description = """
                    매장 단말이 자녀 QR 을 스캔해 결제를 승인한다: 자녀 지갑 차감(PAYMENT 원장) → QR `PAID` 전이 → 부모에게 `PAYMENT_APPROVED` 알림.

                    | 요청 요소 | 필수 | 설명 |
                    |---|:---:|---|
                    | `X-Store-Terminal-Token` (헤더) | ⚠️ 사실상 선택 | 매장 단말 토큰. **없으면 검증을 건너뜀([VULN-WALLET-15])** |
                    | `qrToken` (본문) | ✅ | 스캔한 결제 QR 토큰 |
                    | `amount` (본문) | ✅ | 결제 금액(원, 1 이상 — 단말 입력) |

                    ⚠️ [VULN-WALLET-15] 단말 토큰 헤더가 없으면(null) 검증 스킵 → 미인증 외부인이 임의 자녀 지갑을 결제 가능(웹훅 VULN-4 와 동형).
                    [VULN-WALLET-16] QR 미사용·멱등 미검증 → 같은 qrToken 재전송 시 자녀 잔액 반복 차감(중복 결제).
                    [VULN-WALLET-17] QR 만료 미검증 → 60초 지난 QR 도 승인됨.
                    [VULN-WALLET-18] 자녀 잔액 미검증 → 잔액보다 큰 금액도 통과, 잔액 음수 가능.""")
    @PostMapping("/pay")
    public ResponseEntity<ApiResponse<QrApproveResponse>> pay(
            @Parameter(in = ParameterIn.HEADER, name = "X-Store-Terminal-Token",
                    description = "매장 단말 인증 토큰. 없으면 검증을 건너뜀 — VULN-WALLET-15")
            @RequestHeader(value = "X-Store-Terminal-Token", required = false) String terminalToken,
            @Valid @RequestBody QrApproveRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                qrApprovalService.approve(terminalToken, request.qrToken(), request.amount())));
    }
}
