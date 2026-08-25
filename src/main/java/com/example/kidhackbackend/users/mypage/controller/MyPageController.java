package com.example.kidhackbackend.users.mypage.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.mypage.dto.ProfileResponse;
import com.example.kidhackbackend.users.mypage.dto.ProfileUpdateRequest;
import com.example.kidhackbackend.users.mypage.service.MyPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "MyPage", description = "마이페이지 API")
@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    private void requireLogin(Member member) {
        if (member == null) {
            throw new IllegalArgumentException("로그인이 필요합니다.");
        }
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
}