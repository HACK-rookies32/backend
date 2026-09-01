package com.example.kidhackbackend.admin.member.controller;

import com.example.kidhackbackend.admin.audit.entity.AdminActivityType;
import com.example.kidhackbackend.admin.audit.service.AdminAuditLogService;
import com.example.kidhackbackend.admin.finance.dto.AdminUserWalletResponse;
import com.example.kidhackbackend.admin.member.dto.AdminCustomerResponse;
import com.example.kidhackbackend.admin.member.dto.AdminCustomerUpdateRequest;
import com.example.kidhackbackend.admin.member.dto.AdminDeviceResponse;
import com.example.kidhackbackend.admin.member.dto.MemberSort;
import com.example.kidhackbackend.admin.member.service.AdminCustomerService;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "32. 관리자 - 고객 관리", description = "고객(PARENT/CHILD) 목록/상세/수정/삭제 · 기기·구독 조회 (SUPER_ADMIN)")
@RestController
@RequestMapping("/api/admin/members/customers")
@RequiredArgsConstructor
public class AdminCustomerController {

    private final AdminCustomerService adminCustomerService;
    private final AdminAuditLogService adminAuditLogService;

    @Operation(summary = "고객 목록 조회",
            description = """
                    PARENT/CHILD 회원을 role·keyword(로그인아이디/실명/닉네임 부분일치) 로 필터해 페이징 조회한다.
                    탈퇴 회원은 제외된다.

                    | `sort` | 순서 |
                    |---|---|
                    | `RECENT` (기본) | 가입 최신순 |
                    | `OLDEST` | 가입 오래된순 |
                    | `NAME` | 이름 가나다순 |
                    | `NAME_DESC` | 이름 역순 |
                    """)
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminCustomerResponse>>> getCustomers(
            @Parameter(description = "역할 필터: PARENT / CHILD") @RequestParam(required = false) Role role,
            @Parameter(description = "검색어(로그인아이디/실명/닉네임 부분일치)") @RequestParam(required = false) String keyword,
            @Parameter(description = "페이지 번호(0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "정렬 기준(웹 드롭다운)") @RequestParam(defaultValue = "RECENT") MemberSort sort) {
        return ResponseEntity.ok(ApiResponse.success(
                adminCustomerService.getCustomers(role, keyword, PageRequest.of(page, size, sort.toSort()))));
    }

    @Operation(summary = "고객 상세 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminCustomerResponse>> getCustomer(
            @Parameter(description = "회원 ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminCustomerService.getCustomer(id)));
    }

    @Operation(summary = "고객 정보 수정", description = "감사 로그(MEMBER_UPDATE)를 남긴다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "존재하지 않는 회원",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": false, "data": null,
                             "error": {"code": "NOT_FOUND", "message": "존재하지 않는 회원입니다."}}"""))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "⚠️ 알려진 결함 — 이미 사용 중인 이메일로 수정 시 500 (회원용 PATCH /api/users/me 는 409 반환)",
                    content = @Content(mediaType = "application/json", examples = @ExampleObject(value = """
                            {"success": false, "data": null,
                             "error": {"code": "INTERNAL_ERROR", "message": "서버 오류가 발생했습니다."}}""")))
    })
    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminCustomerResponse>> updateCustomer(
            @Parameter(description = "회원 ID") @PathVariable Long id, @RequestBody AdminCustomerUpdateRequest request) {
        AdminCustomerResponse response = adminCustomerService.updateCustomer(id, request);
        adminAuditLogService.record(AdminActivityType.MEMBER_UPDATE, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "고객 삭제", description = "감사 로그(MEMBER_DELETE)를 남긴다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(
            @Parameter(description = "회원 ID") @PathVariable Long id) {
        adminCustomerService.deleteCustomer(id);
        adminAuditLogService.record(AdminActivityType.MEMBER_DELETE, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "고객 기기 목록 조회")
    @GetMapping("/{id}/devices")
    public ResponseEntity<ApiResponse<List<AdminDeviceResponse>>> getDevices(
            @Parameter(description = "회원 ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminCustomerService.getDevices(id)));
    }

    @Operation(summary = "고객 구독/지갑 정보 조회")
    @GetMapping("/{id}/subscription")
    public ResponseEntity<ApiResponse<AdminUserWalletResponse>> getSubscription(
            @Parameter(description = "회원 ID") @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(adminCustomerService.getSubscription(id)));
    }
}
