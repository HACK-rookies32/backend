package com.example.kidhackbackend.users.qrpay.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.qrpay.dto.TerminalIssueRequest;
import com.example.kidhackbackend.users.qrpay.dto.TerminalResponse;
import com.example.kidhackbackend.users.qrpay.service.StoreTerminalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 매장 단말 등록/토큰 발급 API — QR결제 No.44 선행. <b>관리자(SUPER_ADMIN/GENERAL_ADMIN) 전용이어야 한다.</b>
 *
 * <p>발급되는 토큰은 매장 단말이 결제 승인(No.44)을 호출할 자격증명 — 사실상 '자금 이동 열쇠'다.
 * 시큐리티는 {@code QrPaySecurityConfig}(/api/qrpay/** 인증 체인)가 담당(로그인만 요구).
 */
@Tag(name = "17. QR결제 - 단말 등록", description = "매장 단말 등록 + 토큰 발급/목록 (관리자 전용 의도 — 실제로는 로그인만 요구)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/qrpay/terminals")
@RequiredArgsConstructor
public class StoreTerminalController {

    private final StoreTerminalService storeTerminalService;

    /** 매장 단말 등록 + 토큰 발급. */
    @Operation(summary = "매장 단말 토큰 발급",
            description = """
                    매장 단말을 등록하고 `X-Store-Terminal-Token`(결제 승인 호출용 자격증명)을 발급한다.
                    응답 `TerminalResponse.terminalToken` 에 발급 토큰이 담긴다(노출 주의).

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `storeName` | ✅ | 가맹점(매장) 이름 |

                    ⚠️ [VULN-WALLET-11] 권한검증 미흡(BFLA/권한상승) — SUPER_ADMIN/GENERAL_ADMIN 전용이어야 하지만
                    시큐리티는 '인증만' 요구하고 컨트롤러도 역할을 안 본다 → 일반 유저(PARENT·CHILD)도 로그인만 하면
                    유효 단말 토큰을 발급받아 태그 16 결제 승인으로 임의 자녀 지갑을 차감할 수 있다(자금 이동 열쇠 탈취).""")
    @PostMapping
    public ResponseEntity<ApiResponse<TerminalResponse>> issue(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody TerminalIssueRequest request) {
        // [VULN-WALLET-11] 관리자 전용이어야 할 기능에 역할 검사가 없다(BFLA / 권한상승).
        //   막아야 할 곳이 두 군데인데 둘 다 비어 있다.
        //     · 시큐리티 체인(QrPaySecurityConfig)은 '로그인했는지'(authenticated)만 요구하고
        //       hasAnyRole("SUPER_ADMIN","GENERAL_ADMIN") 같은 역할 제한을 걸지 않는다.
        //     · 컨트롤러인 여기서도 member.getRole() 을 확인하지 않는다.
        //   → 일반 이용자(PARENT·CHILD)도 로그인만 하면 유효한 매장 단말 토큰을 발급받는다.
        //     이 토큰은 결제 승인(No.44)을 호출할 수 있는 자격증명이라, 사실상 남의 자녀 지갑에서
        //     돈을 빼는 열쇠를 아무나 발급받는 셈이다. 2026-09-07 모의해킹에서 실제로 성립했다.
        //   방어: requireAdmin(member) — SUPER_ADMIN/GENERAL_ADMIN 인지 확인하고 아니면 403.
        return ResponseEntity.ok(ApiResponse.success(storeTerminalService.issue(request.storeName())));
    }

    /** 등록 단말 목록 조회(관리자 전용 의도 — 위와 동일하게 역할 미검증). */
    @Operation(summary = "매장 단말 목록",
            description = "등록된 매장 단말을 최신순으로 조회한다(**응답에 terminalToken 포함**). "
                    + "⚠️ [VULN-WALLET-11] 위와 동일 — 역할 미검증으로 일반 유저가 발급된 단말 토큰 목록까지 열람 가능.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<TerminalResponse>>> list(
            @AuthenticationPrincipal Member member) {
        // [VULN-WALLET-11] 위와 동일 — 역할 미검증으로 일반 유저가 발급된 단말 토큰 목록까지 열람 가능.
        return ResponseEntity.ok(ApiResponse.success(storeTerminalService.list()));
    }
}
