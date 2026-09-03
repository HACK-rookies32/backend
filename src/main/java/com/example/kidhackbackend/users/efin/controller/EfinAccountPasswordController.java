package com.example.kidhackbackend.users.efin.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.efin.dto.AccountPasswordChangeRequest;
import com.example.kidhackbackend.users.efin.dto.AccountPasswordRequest;
import com.example.kidhackbackend.users.efin.service.AccountPasswordService;
import com.example.kidhackbackend.users.member.entity.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 전자금융 별도 비밀번호(계좌비밀번호) 설정·변경 — 비밀번호 변경 시 본인확인 + 이전 비밀번호 재사용 방지.
 */
@Tag(name = "21. 전자금융 - 계좌비밀번호", description = "거래용 별도 비밀번호 설정·변경. 변경 시 기존 비번 본인확인 + 과거 재사용 차단.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/efin/account-password")
@RequiredArgsConstructor
public class EfinAccountPasswordController {

    private final AccountPasswordService accountPasswordService;

    @Operation(summary = "계좌비밀번호 설정",
            description = """
                    거래용 별도 비밀번호(6자리 숫자)를 최초 설정한다. 로그인 비밀번호와 분리된 체계다.

                    ⚠️ [VULN-EFIN-5] salt 없는 MD5 로 저장(6자리라 즉시 역산). [VULN-EFIN-6] 재사용 방지용 이력을 평문으로 쌓는다.""")
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> set(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody AccountPasswordRequest request) {
        requireLogin(member);
        accountPasswordService.set(member.getId(), request.password());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "계좌비밀번호 변경",
            description = """
                    기존 계좌비밀번호로 본인확인 후 새 비밀번호로 변경한다. 과거에 쓴 비밀번호로는 바꿀 수 없다.

                    ⚠️ [VULN-EFIN-6] 재사용 차단을 위해 과거 비밀번호를 평문으로 대조·보관한다 → DB 탈취 시 전량 노출.""")
    @PutMapping
    public ResponseEntity<ApiResponse<Void>> change(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody AccountPasswordChangeRequest request) {
        requireLogin(member);
        accountPasswordService.change(member.getId(), request.currentPassword(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private void requireLogin(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
    }
}