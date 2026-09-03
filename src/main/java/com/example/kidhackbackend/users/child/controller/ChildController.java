package com.example.kidhackbackend.users.child.controller;

import com.example.kidhackbackend.users.child.dto.ChildDashboardResponse;
import com.example.kidhackbackend.users.child.dto.ChildDetailResponse;
import com.example.kidhackbackend.users.child.dto.ChildSummaryResponse;
import com.example.kidhackbackend.users.child.dto.ChildUpdateRequest;
import com.example.kidhackbackend.users.child.service.ChildDashboardService;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.child.service.ChildService;
import com.example.kidhackbackend.users.member.entity.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "04. 자녀 관리", description = "자녀 목록/상세/수정/페어링 해제 · 강제 로그아웃 · 통합 대시보드 — PARENT 전용")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/children")
@RequiredArgsConstructor
public class ChildController {

    private final ChildDashboardService childDashboardService;
    private final ChildService childService;

    @Operation(summary = "통합 대시보드 조회 (PARENT)",
            description = """
                    현재 위치·실시간 추적 상태·최근 알림(자녀 수신분/부모 수신분 각 3건)·차단 목록·등록 공간을 한 번에 조회한다.
                    `currentLocation` 은 동선 없으면 `null`, 목록류는 빈 배열.

                    - 존재하지 않거나 삭제된 childId, 또는 CHILD 계정이 아닌 id: `400 INVALID_PARAMETER` "자녀를 찾을 수 없습니다."

                    ⚠️ 요청자(부모)와 childId 간 페어링 관계는 검증하지 않음(IDOR).
                    """)
    @GetMapping("/{childId}/dashboard")
    public ResponseEntity<ApiResponse<ChildDashboardResponse>> dashboard(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "자녀 회원 ID") @PathVariable Long childId) {
        return ResponseEntity.ok(ApiResponse.success(
                childDashboardService.getDashboard(childId, member.getId())));
    }

    @Operation(summary = "자녀 목록 조회 (PARENT)",
            description = "로그인한 부모와 페어링된 자녀 목록. 페어링 해제·소프트 삭제된 자녀는 제외. 각 항목에 실시간 추적 on/off 포함.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChildSummaryResponse>>> getChildren(@AuthenticationPrincipal Member member) {
        return ResponseEntity.ok(ApiResponse.success(childService.getChildren(member.getId())));
    }

    @Operation(summary = "자녀 상세 조회 (PARENT)",
            description = """
                    자녀 회원 ID 로 상세 프로필을 조회한다. `paired` 는 부모 연결 여부.

                    - 존재하지 않거나 삭제된 childId, 또는 CHILD 계정이 아닌 id: `400 INVALID_PARAMETER` "자녀를 찾을 수 없습니다."

                    ⚠️ 소유권 미검증(IDOR). 단 대상이 CHILD 역할이 아니면 거부됨.
                    """)
    @GetMapping("/{childId}")
    public ResponseEntity<ApiResponse<ChildDetailResponse>> getChild(
            @Parameter(description = "자녀 회원 ID") @PathVariable Long childId) {
        return ResponseEntity.ok(ApiResponse.success(childService.getChild(childId)));
    }

    @Operation(summary = "자녀 프로필 수정 (PARENT)",
            description = """
                    포함된(non-null) 필드만 반영. 응답은 자녀 상세 조회와 동일 구조. **모든 필드 선택.**

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `username` | — | 변경할 실명 |
                    | `nickname` | — | 변경할 닉네임 |
                    | `email` | — | 변경할 이메일(다른 회원과 중복이면 409) |
                    | `phoneNumber` | — | 변경할 전화번호 |
                    | `birthDate` | — | 변경할 생년월일 |
                    | `address` | — | 변경할 주소 |
                    | `password` | — | 변경할 비밀번호 |
                    | `profileImageUrl` | — | 변경할 프로필 이미지 URL |

                    ⚠️ 소유권 미검증(IDOR). `nickname` 을 새니타이징 없이 저장 → Stored XSS 가능.
                    """)
    @PatchMapping("/{childId}")
    public ResponseEntity<ApiResponse<ChildDetailResponse>> updateChild(
            @Parameter(description = "자녀 회원 ID") @PathVariable Long childId,
            @RequestBody ChildUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(childService.updateChild(childId, request)));
    }

    @Operation(summary = "자녀 페어링 해제 (PARENT)",
            description = """
                    부모-자녀 페어링을 해제하고 해당 자녀 계정을 소프트 삭제(`isDeleted=true`)한다. 스케줄러가 일정 시간 후 완전 삭제.

                    ⚠️ 소유권 미검증(IDOR). 단 대상이 CHILD 역할이 아니면 거부되어 부모·관리자 계정은 삭제 불가. 자녀 동의 절차 없음(의도).
                    """)
    @DeleteMapping("/{childId}")
    public ResponseEntity<ApiResponse<Void>> unpairChild(
            @Parameter(description = "자녀 회원 ID") @PathVariable Long childId) {
        childService.unpairChild(childId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "자녀 강제 로그아웃 (PARENT)",
            description = """
                    지정한 자녀의 refresh token 을 전부 무효화한다. 자녀는 스스로 로그아웃할 수 없고 이 API 로만 세션을 끊을 수 있다.

                    - 존재하지 않거나 삭제된 childId, 또는 CHILD 계정이 아닌 id: `400 INVALID_PARAMETER` "자녀를 찾을 수 없습니다."

                    ⚠️ 소유권 미검증(IDOR).
                    """)
    @PostMapping("/{childId}/logout")
    public ResponseEntity<ApiResponse<Void>> logoutChild(
            @Parameter(description = "자녀 회원 ID") @PathVariable Long childId) {
        childService.logoutChild(childId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
