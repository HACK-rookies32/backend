package com.example.kidhackbackend.users.mypage.service;

import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.mypage.dto.ProfileResponse;
import com.example.kidhackbackend.users.mypage.dto.ProfileUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final MemberRepository memberRepository;

    @Transactional
    public ProfileResponse updateProfile(Member member, ProfileUpdateRequest request) {
        if (request.getNickname() != null) member.setNickname(request.getNickname());
        if (request.getPhoneNumber() != null) member.setPhoneNumber(request.getPhoneNumber());
        if (request.getEmail() != null) member.setEmail(request.getEmail());
        if (request.getAddress() != null) member.setAddress(request.getAddress());
        if (request.getProfileImageUrl() != null) member.setProfileImageUrl(request.getProfileImageUrl());

        memberRepository.save(member);
        return toResponse(member);
    }

    @Transactional
    public void withdraw(Member member) {
        member.setDeleted(true);
        memberRepository.save(member);
    }

    private ProfileResponse toResponse(Member member) {
        return new ProfileResponse(
                member.getId(),
                member.getLoginId(),
                member.getUsername(),
                member.getNickname(),
                member.getEmail(),
                member.getPhoneNumber(),
                member.getAddress(),
                member.getProfileImageUrl()
        );
    }
}