package com.example.kidhackbackend.users.appcontrol.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.appcontrol.dto.*;
import com.example.kidhackbackend.users.appcontrol.service.AppControlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 앱 통제 API — 자녀 설치 앱 동기화/조회, 앱별 사용시간 제한 규칙 CRUD.
 *
 * <p>⚠️ 이 도메인의 조회/등록/동기화 엔드포인트는 대상 자녀를 요청 값(childId)으로만 지정하고
 * 요청자와의 관계(부모-자녀 / 본인)를 검증하지 않는다(IDOR). {@code [VULN-APPCTRL-1~3]} 참고.
 */
@Tag(name = "07. 앱 통제", description = "자녀 설치 앱 동기화/조회 · 앱별 사용시간 제한 규칙 CRUD")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/appcontrol")
@RequiredArgsConstructor
public class AppControlController {

    private final AppControlService appControlService;

    @Operation(summary = "설치 앱 목록 동기화 (자녀 앱)",
            description = """
                    자녀 앱이 설치된 앱 목록을 서버에 올린다. **기존 목록을 전부 삭제한 뒤 새로 저장**(전량 교체)하고 반영 건수를 반환한다.
                    요청 리스트의 중복 패키지는 마지막 값으로 합쳐진다.

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `childId` | ✅ | 자녀 회원 ID |
                    | `apps` | ✅ | 설치 앱 목록(1건 이상) |
                    | `apps[].packageName` | ✅ | 패키지명 |
                    | `apps[].appName` | ✅ | 앱 이름 |

                    ⚠️ [VULN-APPCTRL-1] 본문 childId 본인 검증 없음 + 파괴적(전량 삭제) → 타 자녀 목록 파괴/위조 가능(IDOR).""")
    @PostMapping("/sync")
    public ResponseEntity<ApiResponse<Integer>> sync(@Valid @RequestBody SyncRequest request) {
        return ResponseEntity.ok(ApiResponse.success(appControlService.sync(request)));
    }

    @Operation(summary = "자녀 설치 앱 목록 조회 (부모)",
            description = "⚠️ [VULN-APPCTRL-3] childId 소유권 미검증 → 임의 자녀 목록 열람 가능(IDOR).")
    @GetMapping("/apps/{childId}")
    public ResponseEntity<ApiResponse<List<AppResponse>>> getInstalledApps(
            @Parameter(description = "자녀 회원 ID") @PathVariable Long childId) {
        List<AppResponse> data = appControlService.getInstalledApps(childId).stream()
                .map(AppResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "통제 규칙 추가 (부모)",
            description = """
                    패키지명 + 일일 사용시간 제한 규칙을 추가한다. 같은 앱에 이미 규칙이 있으면 409. 모든 필드 필수.

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `childId` | ✅ | 자녀 회원 ID |
                    | `packageName` | ✅ | 패키지명 |
                    | `appName` | ✅ | 앱 이름 |
                    | `dailyLimitMinutes` | ✅ | 일일 사용 제한(분, 0 이상. 0=완전 차단) |

                    ⚠️ [VULN-APPCTRL-2] 본문 childId 소유권 미검증 → 임의 자녀에 규칙 생성 가능(IDOR).""")
    @PostMapping("/rules")
    public ResponseEntity<ApiResponse<RuleResponse>> addRule(@Valid @RequestBody RuleRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                RuleResponse.from(appControlService.addRule(
                        request.childId(), request.packageName(),
                        request.appName(), request.dailyLimitMinutes()))));
    }

    @Operation(summary = "통제 규칙 목록 조회 (부모/자녀)",
            description = "자녀 앱이 규칙을 받아 차단에 적용한다. "
                    + "⚠️ [VULN-APPCTRL-3] childId 소유권 미검증 → 인증만 되면 임의 자녀 규칙 열람 가능(IDOR).")
    @GetMapping("/rules/{childId}")
    public ResponseEntity<ApiResponse<List<RuleResponse>>> getRules(
            @Parameter(description = "자녀 회원 ID") @PathVariable Long childId) {
        List<RuleResponse> data = appControlService.getRules(childId).stream()
                .map(RuleResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "통제 규칙 수정 (부모)",
            description = """
                    null(생략)인 필드는 미변경. 없는 ruleId 는 400. **모든 필드 선택.**

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `dailyLimitMinutes` | — | 일일 사용 제한(분, 0 이상) |
                    | `enabled` | — | 규칙 활성화 여부 |
                    """)
    @PatchMapping("/rules/{ruleId}")
    public ResponseEntity<ApiResponse<RuleResponse>> updateRule(
            @Parameter(description = "규칙 ID") @PathVariable Long ruleId,
            @Valid @RequestBody RuleUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                RuleResponse.from(appControlService.updateRule(
                        ruleId, request.dailyLimitMinutes(), request.enabled()))));
    }

    @Operation(summary = "통제 규칙 삭제 (부모)", description = "없는 ruleId 는 400.")
    @DeleteMapping("/rules/{ruleId}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(
            @Parameter(description = "규칙 ID") @PathVariable Long ruleId) {
        appControlService.deleteRule(ruleId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
