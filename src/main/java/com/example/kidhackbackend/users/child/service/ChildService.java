package com.example.kidhackbackend.users.child.service;

import com.example.kidhackbackend.users.child.dto.ChildDetailResponse;
import com.example.kidhackbackend.users.child.dto.ChildSummaryResponse;
import com.example.kidhackbackend.users.child.dto.ChildUpdateRequest;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChildService {

    private final MemberRepository memberRepository;

    public List<ChildSummaryResponse> getChildren(Long parentId) {
        return memberRepository.findByParentId(parentId).stream()
                .map(ChildSummaryResponse::from)
                .toList();
    }

    // [VULN-CHILD-3] childId 소유권(부모-자녀 관계) 검증 없이 조회 — 임의 childId로 다른 부모의 자녀 상세 열람 가능(IDOR).
    //   VULN-CHILD-1(대시보드)과 동일 계열.
    public ChildDetailResponse getChild(Long childId) {
        Member child = findChild(childId);
        return ChildDetailResponse.from(child);
    }

    // [VULN-CHILD-4] childId 소유권 검증 없이 수정 — 임의 childId로 다른 부모의 자녀 프로필 변경 가능(IDOR).
    @Transactional
    public ChildDetailResponse updateChild(Long childId, ChildUpdateRequest request) {
        Member child = findChild(childId);

        // [VULN-CHILD-5] nickname을 이스케이프/새니타이징 없이 그대로 저장.
        //   프론트(관리자 페이지 등)에서 이스케이프 없이 렌더링하면 Stored XSS로 이어질 수 있음.
        if (request.getNickname() != null) {
            child.setNickname(request.getNickname());
        }
        if (request.getProfileImageUrl() != null) {
            child.setProfileImageUrl(request.getProfileImageUrl());
        }
        memberRepository.save(child);
        return ChildDetailResponse.from(child);
    }

    // [VULN-CHILD-6] childId 소유권 검증 없이 페어링 해제 — 임의 childId로 다른 부모가 페어링을 끊고 계정을 삭제시킬 수 있음(IDOR).
    @Transactional
    public void unpairChild(Long childId) {
        Member child = findChild(childId);
        child.setParent(null);
        child.setDeleted(true);
        memberRepository.save(child);
    }

    private Member findChild(Long childId) {
        Member child = memberRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("자녀를 찾을 수 없습니다."));
        if (child.isDeleted()) {
            throw new IllegalArgumentException("자녀를 찾을 수 없습니다.");
        }
        return child;
    }
}