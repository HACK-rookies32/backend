package com.example.kidhackbackend.users.mypage.controller;

import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.service.NotificationSettingService;
import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.mypage.dto.ActivityPageResponse;
import com.example.kidhackbackend.users.mypage.dto.ProfileResponse;
import com.example.kidhackbackend.users.mypage.dto.ProfileUpdateRequest;
import com.example.kidhackbackend.users.mypage.service.MyPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "02. 마이페이지", description = "내 정보 조회/수정 · 회원 탈퇴 · 알림 설정 · 활동 내역 — 로그인 필요")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;
    private final NotificationSettingService notificationSettingService;

    private void requireLogin(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
    }

    @Operation(summary = "내 정보 조회", description = "로그인한 본인의 프로필 정보를 조회한다.")
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(@AuthenticationPrincipal Member member) {
        requireLogin(member);
        return ResponseEntity.ok(ApiResponse.success(myPageService.getProfile(member)));
    }

    @Operation(summary = "프로필 수정",
            description = """
                    닉네임/전화번호/이메일/주소/프로필이미지를 부분 수정한다. 값이 있는 필드만 반영되고 `null` 은 미변경.

                    - 다른 회원이 사용 중인 이메일로 변경 시 `409 CONFLICT`.
                    """)
    @PatchMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal Member member,
            @RequestBody ProfileUpdateRequest request) {
        requireLogin(member);
        return ResponseEntity.ok(ApiResponse.success(myPageService.updateProfile(member, request)));
    }

    @Operation(summary = "회원 탈퇴",
            description = """
                    본인 계정을 소프트 삭제(`isDeleted=true`)한다. 스케줄러가 1분마다 확인해 탈퇴 후 10분 지난 계정을 완전 삭제한다.
                    """)
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> withdraw(@AuthenticationPrincipal Member member) {
        requireLogin(member);
        myPageService.withdraw(member);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "알림 설정 조회",
            description = """
                    알림 유형별 on/off 맵을 반환한다. 한 번도 변경한 적 없으면 전부 기본값 `true`.
                    노출 유형: `LOCATION_ENTER, LOCATION_EXIT, LOCATION_ARRIVED, DEVICE_POWER_OFF, HARMFUL_SITE, APP_USAGE, NOTICE, PAYMENT_APPROVED, PAYMENT_CANCELED, ETC`.
                    """)
    @GetMapping("/notification-settings")
    public ResponseEntity<ApiResponse<Map<NotificationType, Boolean>>> getNotificationSettings(
            @AuthenticationPrincipal Member member) {
        requireLogin(member);
        return ResponseEntity.ok(ApiResponse.success(notificationSettingService.getSettings(member.getId())));
    }

    @Operation(summary = "알림 설정 변경(부분)",
            description = """
                    변경할 유형만 `{ "유형": true/false }` 로 전달한다. 포함되지 않은 유형은 기존 값 유지, 변경 후 전체 설정을 반환한다.

                    - 정의되지 않은 key(오타 등) → `400 INVALID_PARAMETER` "요청 본문을 읽을 수 없습니다."
                    - 정의됐지만 설정 불가 유형(`SCREENSHOT_REQUESTED`) → 무시.
                    """)
    @PatchMapping("/notification-settings")
    public ResponseEntity<ApiResponse<Map<NotificationType, Boolean>>> updateNotificationSettings(
            @AuthenticationPrincipal Member member,
            @RequestBody Map<NotificationType, Boolean> request) {
        requireLogin(member);
        return ResponseEntity.ok(ApiResponse.success(notificationSettingService.updateSettings(member.getId(), request)));
    }

    @Operation(summary = "활동 내역 조회",
            description = """
                    커뮤니티에서 작성한 게시글/댓글/좋아요 내역을 종류별로 페이징 조회한다(작성일 내림차순).

                    - `type` 은 `POST` / `COMMENT` / `LIKE` (대소문자 무관).
                    - `type` 미전달 → `400 INVALID_PARAMETER` "필수 파라미터가 없습니다: type".
                    - 지원하지 않는 `type` → `400 INVALID_PARAMETER` "지원하지 않는 활동 유형입니다: {값}".
                    """)
    @GetMapping("/activities")
    public ResponseEntity<ApiResponse<ActivityPageResponse>> getActivities(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "활동 종류: POST / COMMENT / LIKE", required = true) @RequestParam String type,
            @Parameter(description = "페이지 번호(0부터)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지당 항목 수") @RequestParam(defaultValue = "10") int size) {
        requireLogin(member);
        return ResponseEntity.ok(ApiResponse.success(myPageService.getActivities(member.getId(), type, page, size)));
    }
}
