package com.example.kidhackbackend.users.child.dto;

import com.example.kidhackbackend.users.member.entity.Member;

import java.time.LocalDate;

public record ChildDetailResponse(
        Long id, String loginId, String username, String nickname,
        String email, String phoneNumber, LocalDate birthDate, String address, String profileImageUrl,
        boolean paired
) {
    public static ChildDetailResponse from(Member child) {
        return new ChildDetailResponse(
                child.getId(), child.getLoginId(), child.getUsername(), child.getNickname(),
                child.getEmail(), child.getPhoneNumber(), child.getBirthDate(), child.getAddress(),
                child.getProfileImageUrl(), child.getParent() != null);
    }
}
