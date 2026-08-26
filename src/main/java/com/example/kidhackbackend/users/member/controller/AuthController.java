package com.example.kidhackbackend.users.member.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.dto.*;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.service.AccountRecoveryService;
import com.example.kidhackbackend.users.member.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "회원가입 / 로그인 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AccountRecoveryService accountRecoveryService; // 추가

    @Operation(summary = "회원가입", description = "회원 정보를 받아 가입 처리 후, 가입된 회원 정보를 반환한다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(@RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.refresh(request)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal Member member) {
        authService.logout(member);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/find-id/send-code")
    public ResponseEntity<ApiResponse<FindIdSendCodeResponse>> sendFindIdCode(@RequestBody FindIdSendCodeRequest request) {
        String code = accountRecoveryService.sendFindIdCode(request.getUsername(), request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(new FindIdSendCodeResponse("인증코드가 발송되었습니다.", code)));
    }

    @PostMapping("/find-id/verify")
    public ResponseEntity<ApiResponse<FindIdVerifyResponse>> verifyFindId(@RequestBody FindIdVerifyRequest request) {
        String loginId = accountRecoveryService.verifyFindId(request.getEmail(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success(new FindIdVerifyResponse(loginId)));
    }

    @PostMapping("/reset-password/send-code")
    public ResponseEntity<ApiResponse<ResetPasswordSendCodeResponse>> sendResetPasswordCode(@RequestBody ResetPasswordSendCodeRequest request) {
        String code = accountRecoveryService.sendResetPasswordCode(request.getLoginId(), request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(new ResetPasswordSendCodeResponse("인증코드가 발송되었습니다.", code)));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody ResetPasswordRequest request) {
        accountRecoveryService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}