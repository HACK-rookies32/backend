package com.example.kidhackbackend.users.wallet.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.wallet.dto.TransferRequest;
import com.example.kidhackbackend.users.wallet.dto.TransferResponse;
import com.example.kidhackbackend.users.wallet.service.WalletTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 부모↔자녀 지갑 이체 API — 역할표 No.39(용돈 이체)·No.40(잔액 회수). 부모(PARENT) 전용.
 *
 * <p>충전 컨트롤러와 분리해 결합도를 낮춘다. 시큐리티는 기존 {@code WalletSecurityConfig}(/api/wallet/**)가 담당.
 */
@Tag(name = "12. 지갑 - 이체", description = "부모↔자녀 용돈 이체 · 잔액 회수 — PARENT 전용")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletTransferController {

    private final WalletTransferService walletTransferService;

    private void requireParent(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        if (member.getRole() != Role.PARENT) {
            throw new IllegalArgumentException("부모 회원만 이용할 수 있는 기능입니다.");
        }
    }

    /** 용돈 이체: 부모 지갑 → 자녀 지갑 — No.39. */
    @Operation(summary = "용돈 이체",
            description = """
                    부모 지갑에서 자녀 지갑으로 용돈을 이체한다(보내는 쪽 debit + 받는 쪽 credit + 원장 TRANSFER_OUT/IN 2건).

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `childId` | ✅ | 대상 자녀 회원 ID(실재하는 CHILD 여야 함, 자기 자신 불가) |
                    | `amount` | ✅ | 이체 금액(원). 보내는 쪽 잔액보다 크면 400(잔액 부족) |

                    이체 성공 시 자녀에게 `ALLOWANCE_RECEIVED` 알림이 간다.

                    ⚠️ [VULN-WALLET-7] 부모-자녀 페어링(소유권) 미검증(IDOR) → PARENT 권한만 있으면 **남의 자녀** 지갑에도 입금 가능. (유령 id·비-CHILD·자기 자신만 차단됨)""")
    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<TransferResponse>> transfer(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody TransferRequest request) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(
                walletTransferService.transfer(member.getId(), request.childId(), request.amount())));
    }

    /** 잔액 회수: 자녀 지갑 → 부모 지갑 — No.40. */
    @Operation(summary = "자녀 잔액 회수",
            description = """
                    자녀 지갑에서 부모 지갑으로 잔액을 회수한다. 방향만 반대인 이체와 동일 로직.

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `childId` | ✅ | 잔액을 회수할 자녀 회원 ID(실재하는 CHILD, 자기 자신 불가) |
                    | `amount` | ✅ | 회수 금액(원). 자녀 잔액보다 크면 400(잔액 부족) |

                    회수 성공 시 자녀에게 `ALLOWANCE_RECLAIMED` 알림이 간다.

                    ⚠️ [VULN-WALLET-7] 페어링(소유권) 미검증(IDOR) → **남의 자녀** 잔액도 회수(탈취) 가능. (유령 id·비-CHILD·자기 자신만 차단됨)""")
    @PostMapping("/recover")
    public ResponseEntity<ApiResponse<TransferResponse>> recover(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody TransferRequest request) {
        requireParent(member);
        return ResponseEntity.ok(ApiResponse.success(
                walletTransferService.recover(member.getId(), request.childId(), request.amount())));
    }
}
