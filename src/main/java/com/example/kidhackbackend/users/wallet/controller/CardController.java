package com.example.kidhackbackend.users.wallet.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.wallet.dto.CardPublicKeyResponse;
import com.example.kidhackbackend.users.wallet.dto.CardResponse;
import com.example.kidhackbackend.users.wallet.dto.EncryptedCardRequest;
import com.example.kidhackbackend.users.wallet.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 루키즈페이 카드 CRD(안전 버전) — 등록/조회/삭제. 역할표 추가요청 · 금취분평 대응.
 *
 * <p>경로 {@code /api/wallet/cards} — 기존 {@code WalletSecurityConfig}(@Order 7)에 자동으로 걸려
 * PARENT 인가가 적용된다(신규 시큐리티 체인 없음). 소유자는 JWT 사용자({@code member.getId()}).
 *
 * <p>전송 E2E(RSA-OAEP + AES-GCM) + 재전송 방지(nonce/timestamp) + 저장 필드암호화 + CVC 미저장
 * + 마스킹 + 소유권 검증. <b>의도적 취약점 없음</b>(결제/QR 도메인 VULN-* 과 대비되는 대조군).
 */
@Tag(name = "14. 지갑 - 카드", description = "루키즈페이 카드 등록/조회/삭제(PARENT). E2E 암호화 · CVC 미저장 · 마스킹")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/wallet/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @Operation(summary = "공개키 조회",
            description = "카드 등록 전, 프론트가 payload 암호화에 쓸 RSA 공개키와 keyId를 받는다. "
                    + "keyId는 회전 대비로 매 요청 실시간 조회한다.")
    @GetMapping("/public-key")
    public ResponseEntity<ApiResponse<CardPublicKeyResponse>> publicKey() {
        return ResponseEntity.ok(ApiResponse.success(cardService.getPublicKey()));
    }

    @Operation(summary = "카드 등록 (PARENT)",
            description = "E2E 암호화된 카드정보를 복호·검증해 저장한다. 소유자=JWT 사용자. "
                    + "CVC는 복호만 하고 저장하지 않으며, 응답은 마스킹된 카드번호만 담는다.")
    @PostMapping
    public ResponseEntity<ApiResponse<CardResponse>> register(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody EncryptedCardRequest request) {
        return ResponseEntity.ok(ApiResponse.success(cardService.create(member.getId(), request)));
    }

    @Operation(summary = "내 카드 목록 (PARENT)",
            description = "로그인 사용자 본인이 등록한 카드 목록(마스킹). 최신순.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CardResponse>>> list(
            @AuthenticationPrincipal Member member) {
        return ResponseEntity.ok(ApiResponse.success(cardService.list(member.getId())));
    }

    @Operation(summary = "카드 삭제 (PARENT)",
            description = "본인 카드만 삭제한다. 소유권 검증 실패(남의 카드/없음) 시 404.")
    @DeleteMapping("/{cardId}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Member member,
            @PathVariable Long cardId) {
        cardService.delete(member.getId(), cardId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
