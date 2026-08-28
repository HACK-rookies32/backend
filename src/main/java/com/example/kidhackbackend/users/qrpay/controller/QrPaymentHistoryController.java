package com.example.kidhackbackend.users.qrpay.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.qrpay.dto.QrPaymentHistoryPageResponse;
import com.example.kidhackbackend.users.qrpay.service.QrPaymentHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 자녀 결제 내역 조회 API — QR결제 No.45. 부모(PARENT)/자녀(CHILD) 접근.
 *
 * <p>시큐리티는 {@code QrPaySecurityConfig} 앱 체인(@Order 10, /api/qrpay/** 인증)이 담당.
 * CHILD는 본인 내역, PARENT는 childId로 자녀 내역을 조회한다.
 */
@Tag(name = "19. QR결제 - 결제 내역", description = "자녀 결제 내역 페이징 조회 — CHILD는 본인, PARENT는 childId로 조회")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/qrpay/payments")
@RequiredArgsConstructor
public class QrPaymentHistoryController {

    private final QrPaymentHistoryService qrPaymentHistoryService;

    private void requireParentOrChild(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        if (member.getRole() != Role.PARENT && member.getRole() != Role.CHILD) {
            throw new IllegalArgumentException("부모 또는 자녀 회원만 이용할 수 있는 기능입니다.");
        }
    }

    /** 자녀 결제 내역 페이징 조회 — No.45. */
    @Operation(summary = "자녀 결제 내역 조회 (PARENT/CHILD)",
            description = """
                    결제된(paidAt 있는) QR 내역을 매장명·금액·상태(PAID/CANCELLED)·결제시각과 함께 최신순 페이징 조회한다.

                    | 파라미터 | 필수 | 설명 |
                    |---|:---:|---|
                    | `childId` | **PARENT 필수 / CHILD 무시** | 조회할 자녀 ID. CHILD 는 JWT 자기 id 로 강제(파라미터 무시 → 남의 것 조회 불가, 안전) |
                    | `page` / `size` | — | 페이징(기본 0 / 20) |

                    ⚠️ [VULN-WALLET-19] PARENT 경로 페어링 미검증(IDOR) — childId 가 이 부모의 자녀인지 안 본다 →
                    아무 PARENT 나 임의 childId 로 남의 자녀 결제 내역(매장·금액·시각)을 열람 가능. (CHILD 경로는 안전)""")
    @GetMapping
    public ResponseEntity<ApiResponse<QrPaymentHistoryPageResponse>> payments(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "조회할 자녀 ID (PARENT 필수, CHILD 는 무시)") @RequestParam(required = false) Long childId,
            @Parameter(description = "페이지 번호(0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수") @RequestParam(defaultValue = "20") int size) {
        requireParentOrChild(member);
        return ResponseEntity.ok(ApiResponse.success(
                qrPaymentHistoryService.getPayments(member, childId, page, size)));
    }
}
