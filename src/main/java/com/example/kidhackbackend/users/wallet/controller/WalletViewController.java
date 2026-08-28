package com.example.kidhackbackend.users.wallet.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.wallet.dto.ChildWalletResponse;
import com.example.kidhackbackend.users.wallet.dto.MyWalletResponse;
import com.example.kidhackbackend.users.wallet.dto.TxPageResponse;
import com.example.kidhackbackend.users.wallet.entity.TxType;
import com.example.kidhackbackend.users.wallet.service.WalletViewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지갑 조회 API — 내 지갑(No.38, PARENT/CHILD)·자녀 지갑(No.41, PARENT).
 *
 * <p>시큐리티: {@code WalletSecurityConfig}에서 {@code /api/wallet/me}는 PARENT+CHILD, 그 외 wallet 경로는 PARENT.
 */
@Tag(name = "13. 지갑 - 조회", description = "내 지갑 잔액/요약/원장(PARENT·CHILD) · 자녀 지갑 요약/상세원장/이체이력(PARENT)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletViewController {

    private final WalletViewService walletViewService;

    private void requireLogin(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
    }

    private void requireParent(Member member) {
        requireLogin(member);
        if (member.getRole() != Role.PARENT) {
            throw new IllegalArgumentException("부모 회원만 이용할 수 있는 기능입니다.");
        }
    }

    /** 내 지갑 조회(잔액 + 이번 달 요약 + 필터·페이징 원장) — No.38. 본인(PARENT/CHILD). */
    @Operation(summary = "내 지갑 조회",
            description = """
                    로그인한 본인 지갑의 잔액, 이번 달 충전/결제 합계, 거래 원장을 조회한다. PARENT·CHILD 모두 자기 지갑만 본다(JWT 기준).

                    | 파라미터 | 필수 | 설명 |
                    |---|:---:|---|
                    | `type` | — | 거래 유형 필터: CHARGE / REFUND / TRANSFER_IN / TRANSFER_OUT / PAYMENT / PAYMENT_CANCEL / ADJUST |
                    | `from` / `to` | — | 기간 필터(ISO-8601 date-time) |
                    | `page` | — | 페이지 번호(0부터, 기본 0) |
                    | `size` | — | 페이지당 항목 수(기본 20) |

                    지갑이 없으면 잔액 0 · 합계 0 · 빈 목록을 반환한다.""")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<MyWalletResponse>> myWallet(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "거래 유형 필터") @RequestParam(required = false) TxType type,
            @Parameter(description = "기간 시작(ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "기간 종료(ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = "페이지 번호(0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수") @RequestParam(defaultValue = "20") int size) {
        requireLogin(member);
        return ResponseEntity.ok(ApiResponse.success(
                walletViewService.getMyWallet(member.getId(), type, from, to, page, size)));
    }

    /** 자녀 지갑 조회(잔액 + 최근거래 요약) — No.41. 부모 전용. */
    @Operation(summary = "자녀 지갑 요약 조회",
            description = """
                    부모가 자녀 지갑 잔액과 최근 거래 요약을 조회한다. 자녀 지갑이 없으면 잔액 0 · 빈 목록.

                    | 파라미터 | 필수 | 설명 |
                    |---|:---:|---|
                    | `limit` | — | 최근 거래 개수(1~50, 기본 5) |

                    ⚠️ [VULN-WALLET-20] 부모-자녀 페어링(소유권) 미검증(IDOR/BOLA) → 아무 PARENT 나 임의 childId 로 남의 자녀 잔액·거래 열람 가능. (유령 id·비-CHILD 만 차단됨)""")
    @GetMapping("/children/{childId}")
    public ResponseEntity<ApiResponse<ChildWalletResponse>> childWallet(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "자녀 회원 ID") @PathVariable Long childId,
            @Parameter(description = "최근 거래 개수(1~50)") @RequestParam(defaultValue = "5") int limit) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(
                walletViewService.getChildWallet(member.getId(), childId, limit)));
    }

    /** 자녀 지갑 상세 원장(V-1) — 부모가 자녀의 전체 거래 원장을 필터·페이징 조회. */
    @Operation(summary = "자녀 지갑 상세 원장 조회",
            description = """
                    부모가 자녀 지갑의 **전체 거래 원장**을 type/기간 필터·페이징으로 조회한다(요약 5건이 아니라 전체).
                    응답 형태는 내 지갑 조회와 동일(`MyWalletResponse`).

                    | 파라미터 | 필수 | 설명 |
                    |---|:---:|---|
                    | `type` | — | 거래 유형 필터 |
                    | `from` / `to` | — | 기간 필터(ISO-8601) |
                    | `page` / `size` | — | 페이징(기본 0 / 20) |

                    ⚠️ [VULN-WALLET-20] 소유권 미검증(IDOR).""")
    @GetMapping("/children/{childId}/transactions")
    public ResponseEntity<ApiResponse<MyWalletResponse>> childTransactions(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "자녀 회원 ID") @PathVariable Long childId,
            @Parameter(description = "거래 유형 필터") @RequestParam(required = false) TxType type,
            @Parameter(description = "기간 시작(ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "기간 종료(ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = "페이지 번호(0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수") @RequestParam(defaultValue = "20") int size) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(
                walletViewService.getChildTransactions(member.getId(), childId, type, from, to, page, size)));
    }

    /** 자녀별 이체 이력(T-4) — 이 부모↔자녀 사이의 이체/회수 원장만. */
    @Operation(summary = "자녀별 이체 이력 조회",
            description = """
                    이 부모와 해당 자녀 사이의 **이체/회수(TRANSFER_OUT/IN) 원장만** 최신순 페이징으로 조회한다.
                    "이 아이에게 언제 얼마 보냈고 얼마 회수했나" 화면용. (부모 지갑 기준)

                    | 파라미터 | 필수 | 설명 |
                    |---|:---:|---|
                    | `page` / `size` | — | 페이징(기본 0 / 20) |

                    ⚠️ [VULN-WALLET-20] 소유권 미검증(IDOR).""")
    @GetMapping("/children/{childId}/transfers")
    public ResponseEntity<ApiResponse<TxPageResponse>> childTransfers(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "자녀 회원 ID") @PathVariable Long childId,
            @Parameter(description = "페이지 번호(0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수") @RequestParam(defaultValue = "20") int size) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(
                walletViewService.getChildTransfers(member.getId(), childId, page, size)));
    }
}
