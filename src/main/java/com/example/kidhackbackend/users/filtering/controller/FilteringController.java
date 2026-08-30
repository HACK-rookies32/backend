package com.example.kidhackbackend.users.filtering.controller;

import com.example.kidhackbackend.users.filtering.dto.BlockRuleRequest;
import com.example.kidhackbackend.users.filtering.dto.BlockRuleResponse;
import com.example.kidhackbackend.users.filtering.dto.VisitRequest;
import com.example.kidhackbackend.users.filtering.dto.VisitResponse;
import com.example.kidhackbackend.users.filtering.service.BlockRuleService;
import com.example.kidhackbackend.users.filtering.service.FilteringService;
import com.example.kidhackbackend.global.exception.AdminAccessDeniedException;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.List;

/**
 * 유해사이트/앱 차단 API — 역할표 No.5(감지)·No.6(부모 차단).
 */
@Tag(name = "06. 유해사이트/앱 차단",
        description = "차단 규칙 등록/조회/삭제(부모) · 자녀 접속 대상 감지(자녀 앱). "
                + "감지 판정은 자녀별 차단 규칙 + 관리자 전역 유해 키워드(태그 37)를 함께 사용한다.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/filtering")
@RequiredArgsConstructor
public class FilteringController {

    private final BlockRuleService blockRuleService;
    private final FilteringService filteringService;
    private final MemberRepository memberRepository;

    /** childId 가 실재하는(삭제되지 않은) CHILD 계정인지 확인. 소유권(IDOR)은 검증하지 않는다. */
    private void requireExistingChild(Long childId) {
        Member child = memberRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("자녀를 찾을 수 없습니다."));
        if (child.isDeleted() || child.getRole() != Role.CHILD) {
            throw new IllegalArgumentException("자녀를 찾을 수 없습니다.");
        }
    }

    @Operation(summary = "차단 규칙 등록 (부모)",
            description = """
                    자녀에게 차단 패턴을 추가한다. 모든 필드 필수.

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `childId` | ✅ | 자녀 회원 ID |
                    | `pattern` | ✅ | 차단 패턴(도메인/키워드 등) |

                    childId 가 실재 CHILD 인지 검증. ⚠️ 부모-자녀 소유권은 미검증(IDOR).""")
    @PostMapping("/rules")
    public ResponseEntity<ApiResponse<BlockRuleResponse>> addRule(@Valid @RequestBody BlockRuleRequest request) {
        requireExistingChild(request.childId());
        BlockRuleResponse data = BlockRuleResponse.from(
                blockRuleService.addRule(request.childId(), request.pattern()));
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "차단 규칙 목록 조회 (부모 · 자녀 앱)",
            description = """
                    자녀의 차단 규칙 목록.

                    - PARENT: 자녀 규칙 조회(⚠️ 부모-자녀 소유권 미검증, IDOR — 기존 사양).
                    - CHILD: **본인 childId 만** 허용. 자녀 앱의 접근성 서비스가 기기 안에서 유해 콘텐츠를
                      매칭하기 위해 읽는다 — 자녀 사용자에게 보여 주는 화면은 없다.""")
    @GetMapping("/rules/{childId}")
    public ResponseEntity<ApiResponse<List<BlockRuleResponse>>> getRules(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "자녀 회원 ID") @PathVariable Long childId) {
        if (member != null && member.getRole() == Role.CHILD && !member.getId().equals(childId)) {
            throw new AdminAccessDeniedException("본인의 차단 규칙만 조회할 수 있습니다.");
        }
        List<BlockRuleResponse> data = blockRuleService.getRules(childId).stream()
                .map(BlockRuleResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "차단 규칙 삭제",
            description = "없는 ruleId 는 조용히 성공(멱등). ⚠️ 소유권 미검증 / 하드 삭제 / 감사 부재 (의도된 취약점).")
    @DeleteMapping("/rules/{ruleId}")
    public ResponseEntity<ApiResponse<Void>> deleteRule(
            @Parameter(description = "차단 규칙 ID") @PathVariable Long ruleId) {
        blockRuleService.deleteRule(ruleId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "접속 대상 감지 (자녀 앱)",
            description = """
                    자녀 앱이 접속하려는 대상을 보고한다. 차단 대상이면 `blocked=true` 와 함께 알림을 발행한다. 모든 필드 필수.

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `childId` | ✅ | 자녀 회원 ID |
                    | `target` | ✅ | 접속하려는 대상(URL/앱 패키지 등) |

                    ⚠️ target 미정규화 · 본문 childId 본인 검증 없음 (의도된 취약점).""")
    @PostMapping("/visit")
    public ResponseEntity<ApiResponse<VisitResponse>> reportVisit(@Valid @RequestBody VisitRequest request) {
        boolean blocked = filteringService.detect(request.childId(), request.target());
        VisitResponse data = new VisitResponse(request.childId(), request.target(), blocked);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
