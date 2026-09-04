package com.example.kidhackbackend.users.efin.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.efin.dto.PaymentPasswordChangeRequest;
import com.example.kidhackbackend.users.efin.dto.PaymentPasswordSetupRequest;
import com.example.kidhackbackend.users.efin.dto.PaymentPasswordStatusResponse;
import com.example.kidhackbackend.users.efin.dto.PaymentPasswordVerifyRequest;
import com.example.kidhackbackend.users.efin.dto.PaymentPasswordVerifyResponse;
import com.example.kidhackbackend.users.efin.service.IdentityVerificationService;
import com.example.kidhackbackend.users.efin.service.PaymentPasswordService;
import com.example.kidhackbackend.users.member.entity.Member;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 결제 비밀번호 — 최초 설정(접근매체 발급 흐름)·변경·본인확인·상태 조회.
 *
 * <p>최초 설정은 카드 등록 위저드에서 실명확인 직후에 이뤄진다(로그인 비번 step-up 생략). 변경은 계정설정에서
 * 앱 로그인 비밀번호로 본인확인 후 한다.
 */
@Tag(name = "21. 전자금융 - 결제 비밀번호", description = "결제용 4자리 비밀번호 설정(카드 발급 시)·변경. 결제 승인 본인확인 수단.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/efin/payment-password")
@RequiredArgsConstructor
public class EfinPaymentPasswordController {

    private final PaymentPasswordService paymentPasswordService;
    private final IdentityVerificationService identityVerificationService;

    @Operation(summary = "결제 비밀번호 최초 설정 (카드 발급 흐름)",
            description = """
                    접근매체(카드) 발급 흐름에서 결제용 4자리 비밀번호를 최초 설정한다. 직전 실명확인이 본인확인을
                    대신하므로 로그인 비밀번호를 요구하지 않는다. 실명확인이 안 되어 있으면 400.

                    ⚠️ [VULN-EFIN-5] salt 없는 MD5 로 저장(4자리라 즉시 역산). [VULN-EFIN-6] 재사용 방지용 이력을 평문으로 쌓는다.""")
    @PostMapping("/setup")
    public ResponseEntity<ApiResponse<Void>> setup(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody PaymentPasswordSetupRequest request) {
        requireLogin(member);
        if (!identityVerificationService.isVerified(member.getId())) {
            throw new IllegalArgumentException("실명확인이 필요합니다.");
        }
        paymentPasswordService.setup(member.getId(), request.password());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "결제 비밀번호 변경",
            description = """
                    기존 결제 비밀번호 + 앱 로그인 비밀번호로 본인확인 후 새 비밀번호로 변경한다. 과거에 쓴 비밀번호로는 바꿀 수 없다.

                    ⚠️ [VULN-EFIN-6] 재사용 차단을 위해 과거 비밀번호를 평문으로 대조·보관한다 → DB 탈취 시 전량 노출.""")
    @PutMapping
    public ResponseEntity<ApiResponse<Void>> change(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody PaymentPasswordChangeRequest request) {
        requireLogin(member);
        paymentPasswordService.change(member.getId(), request.currentPassword(), request.newPassword(),
                request.loginPassword());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "결제 비밀번호 확인",
            description = "입력한 결제 비밀번호가 맞는지 확인한다(거래 전 UI 점검용, 소비 없음). 최종 검증은 거래 관문이 한다.")
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<PaymentPasswordVerifyResponse>> verify(
            @AuthenticationPrincipal Member member,
            @Valid @RequestBody PaymentPasswordVerifyRequest request) {
        requireLogin(member);
        boolean valid = paymentPasswordService.matches(member.getId(), request.password());
        return ResponseEntity.ok(ApiResponse.success(new PaymentPasswordVerifyResponse(valid)));
    }

    @Operation(summary = "결제 비밀번호 설정 상태",
            description = "결제 비밀번호가 설정되어 있는지 조회한다. 카드 등록 위저드가 설정 단계를 건너뛸지 판단한다.")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<PaymentPasswordStatusResponse>> status(
            @AuthenticationPrincipal Member member) {
        requireLogin(member);
        return ResponseEntity.ok(ApiResponse.success(
                new PaymentPasswordStatusResponse(paymentPasswordService.exists(member.getId()))));
    }

    private void requireLogin(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
    }
}
