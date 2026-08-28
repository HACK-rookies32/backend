package com.example.kidhackbackend.users.child.dto;

import com.example.kidhackbackend.users.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "자녀 목록 항목")
public record ChildSummaryResponse(
        @Schema(description = "자녀 회원 ID", example = "7") Long id,
        @Schema(description = "자녀 실명", example = "김하늘") String username,
        @Schema(description = "자녀 닉네임", example = "하늘이") String nickname,
        @Schema(description = "프로필 이미지 URL") String profileImageUrl,
        @Schema(description = "실시간 위치 추적 on/off(설정한 적 없으면 false)", example = "false") boolean realtimeEnabled
) {
    public static ChildSummaryResponse from(Member child, boolean realtimeEnabled) {
        return new ChildSummaryResponse(
                child.getId(), child.getUsername(), child.getNickname(), child.getProfileImageUrl(),
                realtimeEnabled);
    }
}
