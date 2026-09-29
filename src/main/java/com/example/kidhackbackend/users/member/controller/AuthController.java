package com.example.kidhackbackend.users.member.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.dto.*;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.service.AccountRecoveryService;
import com.example.kidhackbackend.users.member.service.AuthService;
import com.example.kidhackbackend.users.member.service.PhoneVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "01. 인증/계정", description = "회원가입 · 로그인 · 토큰 갱신 · 아이디 찾기 · 비밀번호 재설정")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AccountRecoveryService accountRecoveryService;
    private final PhoneVerificationService phoneVerificationService;

    @Operation(summary = "아이디·이메일 중복 확인",
            description = """
                    가입 폼에서 「다음」을 누를 때 미리 확인하는 용도다. 로그인 없이 부를 수 있다.

                    | 파라미터 | 필수 | 설명 |
                    |---|:---:|---|
                    | `loginId` | — | 확인할 아이디 |
                    | `email` | — | 확인할 이메일 |

                    준 것만 확인하고 **안 준 항목은 `null`** 로 답한다(자녀는 이메일 없이 가입한다).

                    ⚠️ 이 확인이 가입의 중복 검사를 **대체하지 않는다.** 확인과 가입 사이에 다른 사람이
                    같은 값으로 가입할 수 있으므로 `POST /api/auth/signup` 은 여전히 409 를 줄 수 있다.
                    """)
    @GetMapping("/availability")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> checkAvailability(
            @RequestParam(required = false) String loginId,
            @RequestParam(required = false) String email) {
        return ResponseEntity.ok(ApiResponse.success(authService.checkAvailability(loginId, email)));
    }

    @Operation(summary = "회원가입",
            description = """
                    회원 정보를 받아 가입 처리 후 가입된 회원 정보를 반환한다.

                    **요청 필드**

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `loginId` | ✅ | 로그인 아이디 |
                    | `username` | ✅ | 실명 |
                    | `password` | ✅ | 비밀번호(평문 저장 — 의도된 취약점) |
                    | `nickname` | ✅ | 닉네임 |
                    | `phoneNumber` | ✅ | 휴대폰 번호 |
                    | `birthDate` | ✅ | 생년월일 `yyyy-MM-dd` |
                    | `role` | ✅ | `PARENT` / `CHILD` |
                    | `email` | ⚠️ 조건부 | `role` 이 `CHILD` 가 아니면 필수 |
                    | `address` | — | 주소 |
                    | `profileImageUrl` | — | 프로필 이미지 URL |
                    | `parentId` | — | `role=CHILD` 일 때 연결할 부모 id |

                    - 필수 필드 누락 시 `400 INVALID_PARAMETER`, 이미 존재하는 `loginId` 는 `409 CONFLICT`.
                    """)
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "회원가입 휴대폰 인증 - 인증코드 발송",
            description = """
                    가입 폼에서 휴대폰 번호로 6자리 인증코드를 발송한다. 로그인 없이 부를 수 있다.

                    **요청 필드**

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `phoneNumber` | ✅ | 인증코드를 받을 휴대폰 번호 |

                    - 정상이라면 코드는 그 번호의 소유자에게 SMS 로만 가야 한다.
                    - ⚠️ 발송은 mock 이며 인증코드가 응답 `data.code` 에도 그대로 노출된다(화면에는 노출 금지).
                    - ⚠️ 서버는 코드를 저장·검증하지 않는다. 가입 통과는 `POST /api/auth/signup` 의
                      `phoneVerified` 불리언으로만 판정된다(의도된 취약점 — 클라 전용 검증).
                    """)
    @PostMapping("/phone/send-code")
    public ResponseEntity<ApiResponse<PhoneVerifySendCodeResponse>> sendPhoneVerifyCode(
            @RequestBody PhoneVerifySendCodeRequest request) {
        String code = phoneVerificationService.sendCode(request.getPhoneNumber());
        return ResponseEntity.ok(ApiResponse.success(
                new PhoneVerifySendCodeResponse("인증번호가 발송되었습니다.", code)));
    }

    @Operation(summary = "회원가입 휴대폰 인증 - 코드 검증",
            description = """
                    발송받은 6자리 인증코드가 유효한지 검증한다. 로그인 없이 부를 수 있다.

                    **요청 필드**

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `phoneNumber` | ✅ | 인증코드를 받은 휴대폰 번호 |
                    | `code` | ✅ | 발송받은 6자리 인증코드 |

                    - 응답 `data.valid` 로 성공 여부를 돌려준다(TTL 180초, 시도 제한 없음).
                    - ⚠️ 클라이언트가 이 결과를 신뢰해 다음 단계로 넘어가며, 서버는 이를 가입과 묶어
                      강제하지 않는다 → 응답을 `valid:true` 로 변조하면 틀린 코드로도 통과한다(의도된 취약점).
                    """)
    @PostMapping("/phone/verify")
    public ResponseEntity<ApiResponse<PhoneVerifyResponse>> verifyPhoneCode(
            @RequestBody PhoneVerifyRequest request) {
        boolean valid = phoneVerificationService.verify(request.getPhoneNumber(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success(new PhoneVerifyResponse(valid)));
    }

    @Operation(summary = "로그인",
            description = """
                    `loginId` · `password` 로 로그인하고 access token / refresh token 을 발급한다.

                    **요청 필드**

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `loginId` | ✅ | 로그인 아이디 |
                    | `password` | ✅ | 비밀번호 |

                    - 아이디 없음·비밀번호 불일치·탈퇴한 계정: `401 UNAUTHORIZED`.
                    - 응답의 `refreshToken` 을 저장해 두었다가 `/api/auth/token/refresh` 에 사용한다.
                    """)
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "토큰 갱신",
            description = """
                    refresh token 으로 새 access token 을 발급한다. refresh token 자체는 재사용되며 회전(rotation)되지 않는다.

                    **요청 필드**

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `refreshToken` | ✅ | 로그인 시 발급받은 refresh token |

                    - 존재하지 않는·만료된 refresh token, 탈퇴한 계정: `400 INVALID_PARAMETER`.
                    """)
    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<TokenRefreshResponse>> refresh(@RequestBody TokenRefreshRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.refresh(request)));
    }

    @Operation(summary = "로그아웃 (PARENT 전용)",
            description = """
                    로그인한 본인의 refresh token 을 전부 무효화한다. **PARENT 만 호출 가능**하며 CHILD 는 직접 로그아웃할 수 없다
                    (부모가 `POST /api/children/{childId}/logout` 으로 강제 로그아웃).
                    """,
            security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal Member member) {
        authService.logout(member);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "아이디 찾기 - 인증코드 발송",
            description = """
                    실명(`username`) + 이메일(`email`) 로 계정을 조회하고 6자리 인증코드를 발송한다(유효 5분).
                    본인 이메일(PARENT/ADMIN) 또는 부모 이메일(CHILD 계정 조회) 로 찾을 수 있다.

                    **요청 필드**

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `username` | ✅ | 찾으려는 계정의 실명(자녀 계정이면 자녀 실명) |
                    | `email` | ✅ | 본인 이메일 또는 부모의 이메일 |

                    ⚠️ 메일 발송은 mock 이며 인증코드가 응답 `data.code` 에도 그대로 노출된다(화면에는 노출 금지).
                    """)
    @PostMapping("/find-id/send-code")
    public ResponseEntity<ApiResponse<FindIdSendCodeResponse>> sendFindIdCode(@RequestBody FindIdSendCodeRequest request) {
        String code = accountRecoveryService.sendFindIdCode(request.getUsername(), request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(new FindIdSendCodeResponse("인증코드가 발송되었습니다.", code)));
    }

    @Operation(summary = "아이디 찾기 - 코드 검증",
            description = """
                    발송받은 인증코드를 검증하고 통과하면 해당 계정의 `loginId` 를 반환한다.

                    **요청 필드**

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `email` | ✅ | 인증코드를 받은 이메일 |
                    | `code` | ✅ | 발송받은 6자리 인증코드 |
                    """)
    @PostMapping("/find-id/verify")
    public ResponseEntity<ApiResponse<FindIdVerifyResponse>> verifyFindId(@RequestBody FindIdVerifyRequest request) {
        String loginId = accountRecoveryService.verifyFindId(request.getEmail(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success(new FindIdVerifyResponse(loginId)));
    }

    @Operation(summary = "비밀번호 재설정 - 인증코드 발송",
            description = """
                    `loginId` + 이메일로 계정을 조회하고 6자리 인증코드를 발송한다(유효 5분).
                    본인 이메일 또는 부모 이메일(CHILD 계정) 로 요청할 수 있다.

                    **요청 필드**

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `loginId` | ✅ | 비밀번호를 재설정할 계정의 로그인 아이디 |
                    | `email` | ✅ | 본인 이메일 또는 부모의 이메일 |

                    ⚠️ 메일 발송은 mock 이며 인증코드가 응답 `data.code` 에도 그대로 노출된다.
                    """)
    @PostMapping("/reset-password/send-code")
    public ResponseEntity<ApiResponse<ResetPasswordSendCodeResponse>> sendResetPasswordCode(@RequestBody ResetPasswordSendCodeRequest request) {
        String code = accountRecoveryService.sendResetPasswordCode(request.getLoginId(), request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(new ResetPasswordSendCodeResponse("인증코드가 발송되었습니다.", code)));
    }

    @Operation(summary = "비밀번호 재설정",
            description = """
                    인증코드를 검증하고 통과하면 `newPassword` 로 변경한다.

                    **요청 필드**

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `email` | ✅ | 인증코드를 받은 이메일 |
                    | `code` | ✅ | 발송받은 6자리 인증코드 |
                    | `newPassword` | ✅ | 새로 설정할 비밀번호 |
                    """)
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@RequestBody ResetPasswordRequest request) {
        accountRecoveryService.resetPassword(request.getEmail(), request.getCode(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
