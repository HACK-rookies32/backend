package com.example.kidhackbackend.users.child.dto;

import com.example.kidhackbackend.users.member.entity.Member;

public record ChildSummaryResponse(Long id, String username, String nickname, String profileImageUrl) {
    public static ChildSummaryResponse from(Member child) {
        return new ChildSummaryResponse(child.getId(), child.getUsername(), child.getNickname(), child.getProfileImageUrl());
    }
}