package com.example.kidhackbackend.users.child.dto;

import com.example.kidhackbackend.users.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "자녀 상세 프로필")
public record ChildDetailResponse(
        @Schema(description = "자녀 회원 ID", example = "7") Long id,
        @Schema(description = "자녀 로그인 아이디", example = "child01") String loginId,
        @Schema(description = "자녀 실명", example = "김하늘") String username,
        @Schema(description = "자녀 닉네임", example = "하늘이") String nickname,
        @Schema(description = "자녀 이메일(null 가능)") String email,
        @Schema(description = "자녀 전화번호", example = "010-1234-5678") String phoneNumber,
        @Schema(description = "자녀 생년월일", example = "2015-03-21") LocalDate birthDate,
        @Schema(description = "자녀 주소(null 가능)") String address,
        @Schema(description = "프로필 이미지 URL") String profileImageUrl,
        @Schema(description = "부모 계정 연결 여부(parent 존재 시 true)", example = "true") boolean paired
) {
    public static ChildDetailResponse from(Member child) {
        return new ChildDetailResponse(
                child.getId(), child.getLoginId(), child.getUsername(), child.getNickname(),
                child.getEmail(), child.getPhoneNumber(), child.getBirthDate(), child.getAddress(),
                child.getProfileImageUrl(), child.getParent() != null);
    }
}
