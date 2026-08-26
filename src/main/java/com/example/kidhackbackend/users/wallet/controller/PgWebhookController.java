package com.example.kidhackbackend.users.wallet.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.wallet.dto.PgWebhookRequest;
import com.example.kidhackbackend.users.wallet.service.WalletChargeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "PG Webhook", description = "PG 결제 웹훅 수신 API — 서명 검증(인증 토큰 불필요)")
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
    @Operation(summary = "PG 결제 웹훅", description = "서명 검증 후 충전 주문을 확정하고 잔액을 적립한다.")
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> webhook(
            @RequestHeader(value = "X-PG-Signature", required = false) String signature,
            @RequestBody String rawBody) throws JsonProcessingException {
        PgWebhookRequest payload = objectMapper.readValue(rawBody, PgWebhookRequest.class);
        walletChargeService.confirmByWebhook(rawBody, signature, payload);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
