package com.example.kidhackbackend.users.efin.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.efin.dto.IdentityVerifyRequest;
import com.example.kidhackbackend.users.efin.dto.IdentityVerifyResponse;
import com.example.kidhackbackend.users.efin.service.IdentityVerificationService;
import com.example.kidhackbackend.users.member.entity.Member;
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
 * 접근매체 발급 시 실명확인 — 신분증 진위확인(mock)으로 본인 명의를 확인한다.
 */
@Tag(name = "22. 전자금융 - 실명확인", description = "접근매체(카드 등) 발급 전 신분증 진위확인(실명확인).")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/efin/identity")
@RequiredArgsConstructor
public class EfinIdentityController {

    private final IdentityVerificationService identityVerificationService;

    @Operation(summary = "실명확인(신분증 진위확인)",
            description = """
                    이름·생년월일·신분증 종류·번호·발급일자를 외부 진위확인 기관(mock)에 대조해 실명을 확인한다.

                    ⚠️ [VULN-EFIN-7] 실제 대조 없이 제출 값의 형식·자기일치만으로 통과시키고 결과를 응답에 노출한다 → 조건에 맞는 임의 값으로 우회 가능.""")
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<IdentityVerifyResponse>> verify(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody IdentityVerifyRequest request) {
        if (member == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
        return ResponseEntity.ok(ApiResponse.success(
                identityVerificationService.verify(member.getId(), request)));
    }
}