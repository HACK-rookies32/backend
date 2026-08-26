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
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "MyPage", description = "마이페이지 API")
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

    @Operation(summary = "내 정보 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(@AuthenticationPrincipal Member member) {
        requireLogin(member);
        return ResponseEntity.ok(ApiResponse.success(myPageService.getProfile(member)));
    }

    @Operation(summary = "프로필 수정")
    @PatchMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal Member member,
            @RequestBody ProfileUpdateRequest request) {
        requireLogin(member);
        return ResponseEntity.ok(ApiResponse.success(myPageService.updateProfile(member, request)));
    }

    @Operation(summary = "회원 탈퇴")
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> withdraw(@AuthenticationPrincipal Member member) {
        requireLogin(member);
        myPageService.withdraw(member);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "알림 설정 조회")
    @GetMapping("/notification-settings")
    public ResponseEntity<ApiResponse<Map<NotificationType, Boolean>>> getNotificationSettings(
            @AuthenticationPrincipal Member member) {
        requireLogin(member);
        return ResponseEntity.ok(ApiResponse.success(notificationSettingService.getSettings(member.getId())));
    }

    @Operation(summary = "알림 설정 변경(토글)")
    @PatchMapping("/notification-settings")
    public ResponseEntity<ApiResponse<Map<NotificationType, Boolean>>> updateNotificationSettings(
            @AuthenticationPrincipal Member member,
            @RequestBody Map<NotificationType, Boolean> request) {
        requireLogin(member);
        return ResponseEntity.ok(ApiResponse.success(notificationSettingService.updateSettings(member.getId(), request)));
    }
    @Operation(summary = "활동 내역 조회")
    @GetMapping("/activities")
    public ResponseEntity<ApiResponse<ActivityPageResponse>> getActivities(
            @AuthenticationPrincipal Member member,
            @RequestParam String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        requireLogin(member);
        return ResponseEntity.ok(ApiResponse.success(myPageService.getActivities(member.getId(), type, page, size)));
    }
}