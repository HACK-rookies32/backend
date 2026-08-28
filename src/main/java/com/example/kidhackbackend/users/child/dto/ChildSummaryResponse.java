package com.example.kidhackbackend.users.child.dto;

import com.example.kidhackbackend.users.member.entity.Member;

public record ChildSummaryResponse(
        Long id,
        String username,
        String nickname,
        String profileImageUrl,
        boolean realtimeEnabled
) {
    public static ChildSummaryResponse from(Member child, boolean realtimeEnabled) {
        return new ChildSummaryResponse(
                child.getId(), child.getUsername(), child.getNickname(), child.getProfileImageUrl(),
                realtimeEnabled);
    }
}
