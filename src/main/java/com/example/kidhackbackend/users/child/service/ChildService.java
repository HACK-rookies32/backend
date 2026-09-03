package com.example.kidhackbackend.users.child.service;

import com.example.kidhackbackend.global.exception.DuplicateResourceException;
import com.example.kidhackbackend.users.child.dto.ChildDetailResponse;
import com.example.kidhackbackend.users.child.dto.ChildSummaryResponse;
import com.example.kidhackbackend.users.child.dto.ChildUpdateRequest;
import com.example.kidhackbackend.users.location.service.TrackingSettingService;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.member.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChildService {

    private final MemberRepository memberRepository;
    private final RefreshTokenService refreshTokenService; // 생성자 주입 추가
    private final TrackingSettingService trackingSettingService;

    public List<ChildSummaryResponse> getChildren(Long parentId) {
        return memberRepository.findByParentId(parentId).stream()
                .filter(child -> !child.isDeleted()) // C3: 소프트 삭제된 자녀(본인 탈퇴 등)는 제외
                .map(child -> ChildSummaryResponse.from(
                        child, trackingSettingService.isRealtimeEnabled(child.getId())))
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

        // 포함된(non-null) 필드만 반영한다(PATCH 부분수정). 회원 본인 수정(MyPageService.updateProfile)과
        // 같은 관례·취약점 유지(QA #27): password 평문[VULN-1]·형식검증 없음[VULN-6]·미새니타이즈[VULN-CHILD-5].
        if (request.getUsername() != null) {
            child.setUsername(request.getUsername());
        }
        // [VULN-CHILD-5] nickname을 이스케이프/새니타이징 없이 그대로 저장.
        //   프론트(관리자 페이지 등)에서 이스케이프 없이 렌더링하면 Stored XSS로 이어질 수 있음.
        if (request.getNickname() != null) {
            child.setNickname(request.getNickname());
        }
        // email 은 형식검증은 없지만([VULN-6]) 중복만은 막는다 — 같은 email 이 둘이면 로그인·복구가 꼬인다.
        // 마이페이지 자기수정과 동일하게 처리해 경로 간 email 규칙을 일관시킨다.
        if (request.getEmail() != null && !request.getEmail().equals(child.getEmail())) {
            memberRepository.findByEmail(request.getEmail())
                    .filter(existing -> !existing.getId().equals(child.getId()))
                    .ifPresent(existing -> {
                        throw new DuplicateResourceException("이미 사용 중인 이메일입니다.");
                    });
            child.setEmail(request.getEmail());
        }
        if (request.getPhoneNumber() != null) {
            child.setPhoneNumber(request.getPhoneNumber());
        }
        if (request.getBirthDate() != null) {
            child.setBirthDate(request.getBirthDate());
        }
        if (request.getAddress() != null) {
            child.setAddress(request.getAddress());
        }
        // [VULN-1] 비밀번호 평문 저장 — 회원가입·마이페이지와 동일. 현재 비번 확인 절차는 이 앱에 없다.
        if (request.getPassword() != null) {
            child.setPassword(request.getPassword());
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

    // [VULN-CHILD-7] childId 소유권 검증 없이 강제 로그아웃 — 임의 childId로 다른 부모가 자녀 세션을 끊을 수 있음(IDOR)
    @Transactional
    public void logoutChild(Long childId) {
        Member child = findChild(childId);
        refreshTokenService.revokeAll(child.getId());
    }

    private Member findChild(Long childId) {
        Member child = memberRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("자녀를 찾을 수 없습니다."));
        // C4: 소프트 삭제됐거나 CHILD 역할이 아니면(=부모/관리자 계정) children 리소스로 취급하지 않는다.
        //     부모↔임의 자녀 IDOR(VULN-CHILD-*)은 유지하되, 대상 역할을 벗어난 조작은 차단한다.
        if (child.isDeleted() || child.getRole() != Role.CHILD) {
            throw new IllegalArgumentException("자녀를 찾을 수 없습니다.");
        }
        return child;
    }
}