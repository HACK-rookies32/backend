package com.example.kidhackbackend.users.mypage.service;

import com.example.kidhackbackend.users.communityboard.repository.CommunityBoardCommentRepository;
import com.example.kidhackbackend.users.communityboard.repository.CommunityBoardLikeRepository;
import com.example.kidhackbackend.users.communityboard.repository.CommunityBoardRepository;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.mypage.dto.ActivityPageResponse;
import com.example.kidhackbackend.users.mypage.dto.ActivityResponse;
import com.example.kidhackbackend.users.mypage.dto.ProfileResponse;
import com.example.kidhackbackend.users.mypage.dto.ProfileUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final MemberRepository memberRepository;
    private final CommunityBoardRepository communityBoardRepository;
    private final CommunityBoardCommentRepository communityBoardCommentRepository;
    private final CommunityBoardLikeRepository communityBoardLikeRepository;

    public ProfileResponse getProfile(Member member) {
        return toResponse(member);
    }

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

    public ActivityPageResponse getActivities(Long memberId, String type, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<ActivityResponse> result = switch (type.toUpperCase()) {
            case "POST" -> communityBoardRepository.findByMemberId(memberId, pageable)
                    .map(post -> new ActivityResponse(
                            post.getId(), "POST", post.getId(), post.getTitle(), post.getCreatedAt()));
            case "COMMENT" -> communityBoardCommentRepository.findByMemberId(memberId, pageable)
                    .map(comment -> new ActivityResponse(
                            comment.getId(), "COMMENT", comment.getBoard().getId(), comment.getContent(), comment.getCreatedAt()));
            case "LIKE" -> communityBoardLikeRepository.findByMemberId(memberId, pageable)
                    .map(like -> new ActivityResponse(
                            like.getId(), "LIKE", like.getBoard().getId(), null, like.getCreatedAt()));
            default -> throw new IllegalArgumentException("지원하지 않는 활동 유형입니다: " + type);
        };

        return new ActivityPageResponse(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
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