package com.example.kidhackbackend.users.member.service;

import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 부모가 없어질 때 그 자녀 계정도 함께 정리한다(QA #26).
 *
 * <p>부모가 탈퇴(본인)하거나 관리자가 삭제해도 자녀는 {@code isDeleted} 가 안 찍혀 계속 로그인됐다.
 * 자녀는 본질적으로 부모가 관리하는 미성년 계정이라, 부모가 없어지면 자녀도 접근을 막아야 한다.
 *
 * <p>여기서는 자녀를 <b>소프트 삭제({@code isDeleted=true})</b> 만 한다. 그 순간부터
 * {@code AuthService.login} 이 "탈퇴한 계정" 으로 막고(즉시 차단), 물리 삭제는 기존 정리배치
 * ({@code WithdrawnMemberCleanupScheduler} → {@code MemberRemover})가 부모와 동일하게 처리한다.
 *
 * <p>부모가 없어지는 경로가 둘(본인 탈퇴 · 관리자 삭제)이라, 규칙이 경로마다 어긋나지 않게
 * 이 한 곳에 모아 두 경로가 함께 호출한다({@code MemberRemover} 가 정리 순서를 한 곳에 모은 것과 같은 이유).
 */
@Component
@RequiredArgsConstructor
public class ChildAccountCascade {

    private final MemberRepository memberRepository;

    /**
     * 주어진 회원의 모든 자녀를 소프트 삭제한다. 자녀가 없으면(CHILD·관리자 등) 아무 일도 하지 않는다.
     *
     * @param parentId 없어지는 부모 회원 id
     */
    @Transactional
    public void deactivateChildrenOf(Long parentId) {
        List<Member> children = memberRepository.findByParentId(parentId);
        for (Member child : children) {
            child.setDeleted(true);
        }
        memberRepository.saveAll(children);
    }
}
