package com.example.kidhackbackend.users.wallet.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.wallet.dto.PgWebhookRequest;
import com.example.kidhackbackend.users.wallet.service.WalletChargeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PG 결제 웹훅 수신 API — 역할표 No.36. PG 서버가 서버-투-서버로 호출한다.
 *
 * <p>앱 JWT 인증이 아니라 요청 헤더의 서명({@code X-PG-Signature})으로 진위를 검증한다.
 * 시큐리티는 {@code PgWebhookSecurityConfig}(/api/pg/**, permitAll)가 담당한다.
 */
@Tag(name = "14. 지갑 - PG 웹훅", description = "PG 서버가 호출하는 결제 결과 웹훅 — X-PG-Signature 서명 검증(앱 JWT 불필요)")
@RestController
@RequestMapping("/api/pg")
@RequiredArgsConstructor
public class PgWebhookController {

    private final WalletChargeService walletChargeService;
    // ObjectMapper는 빈으로 노출돼 있지 않아 직접 생성(웹훅 원본 바디 파싱 전용).
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * PG 결제 결과 웹훅. 원본 바디로 서명을 검증해야 하므로 바디를 문자열 그대로 받아 파싱한다.
     */
    @Operation(summary = "PG 결제 웹훅",
            description = """
                    PG 서버가 결제 결과를 서버-투-서버로 통지한다. 앱 JWT 불필요, 진위는 서명으로 검증. 항상 200 을 응답한다(처리 결과는 pg_webhook_event 에 기록).

                    **status 별 처리**
                    - `paid` → READY 주문이면 확정(PAID)·적립. 이미 처리된 주문이면 **멱등 무시**(재적립 안 함).
                    - `failed` / `cancelled` / `canceled` / `aborted` → READY 주문이면 FAILED 로 전이.
                    - 그 외 → 무시.

                    | 요청 요소 | 필수 | 설명 |
                    |---|:---:|---|
                    | `X-PG-Signature` (헤더) | ⚠️ 사실상 선택 | 본문 HMAC-SHA256 서명. **없으면 검증을 건너뜀([VULN-WALLET-4])**. 있는데 틀리면 400 |
                    | `merchantUid` (본문) | ✅ | 가맹점 주문번호 |
                    | `status` (본문) | ✅ | 위 처리 참고 |
                    | `paidAmount` (본문) | ✅ | PG 가 통지한 결제 금액(원) |
                    | `pgTransactionId` (본문) | ✅ | PG 거래 ID |

                    ⚠️ [VULN-WALLET-4] `X-PG-Signature` 헤더가 없으면(null) 서명 검증을 통째로 건너뛴다 → 헤더 없이 위조 바디만 보내면 임의 결제 확정·적립 가능(결제 위조).
                    [VULN-WALLET-3] 재생(replay) 취약점은 이 웹훅 경로에서는 멱등 가드로 막혔고, `POST /api/wallet/charge/approve` 경로에만 남아 있다.""",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "PG 웹훅 페이로드(원본 JSON)",
                    content = @Content(schema = @Schema(implementation = PgWebhookRequest.class))))
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> webhook(
            @Parameter(in = ParameterIn.HEADER, name = "X-PG-Signature",
                    description = "본문 HMAC-SHA256 서명(hex). 없으면 서명 검증을 건너뜀 — VULN-WALLET-4")
            @RequestHeader(value = "X-PG-Signature", required = false) String signature,
            @RequestBody String rawBody) throws JsonProcessingException {
        PgWebhookRequest payload = objectMapper.readValue(rawBody, PgWebhookRequest.class);
        walletChargeService.confirmByWebhook(rawBody, signature, payload);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
