package com.example.kidhackbackend.users.mypage.scheduler;

import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import com.example.kidhackbackend.users.member.service.MemberRemover;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 탈퇴(isDeleted=true) 후 10분 지난 계정을 완전히 삭제한다.
 *
 * <p><b>왜 건별로 지우는가.</b> 예전에는 {@code memberRepository.deleteAll(targets)} 를
 * 스케줄러 메서드 하나의 트랜잭션에서 실행했다. 그런데 회원에게는 필수 FK 자식이 6종 붙어 있어
 * 글을 하나라도 남긴 회원이 섞이면 FK 위반이 났고, <b>한 명이 걸리면 그 배치 전체가 롤백</b>됐다.
 * 실패한 회원은 계속 대상으로 남아 1분마다 같은 예외가 반복됐고, 그 뒤로는 <b>어떤 탈퇴 회원도
 * 정리되지 않았다.</b>
 *
 * <p>그래서 두 가지를 바꿨다.
 * <ul>
 *   <li>자식 정리를 {@link MemberRemover} 가 맡는다 — FK 위반 자체를 없앤다</li>
 *   <li>회원 <b>한 명당 트랜잭션</b>을 따로 연다 — 한 건이 실패해도 나머지는 정리된다</li>
 * </ul>
 *
 * <p>스케줄러 메서드에 {@code @Transactional} 을 두지 않는 것이 요점이다. 여기에 걸면
 * {@code MemberRemover.remove} 가 같은 트랜잭션에 참여해 건별로 나눈 의미가 사라진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawnMemberCleanupScheduler {

    private final MemberRepository memberRepository;
    private final MemberRemover memberRemover;

    @Scheduled(fixedRate = 60000)
    public void cleanupWithdrawnMembers() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
        List<Member> targets = memberRepository.findByIsDeletedTrueAndUpdatedAtBefore(cutoff);

        int removed = 0;
        int skipped = 0;
        int failed = 0;

        for (Member member : targets) {
            // 관리자 계정은 대상이 아니다 — Notice.author 가 필수 FK 라 공지 이력이 훼손된다.
            if (!memberRemover.isRemovable(member)) {
                skipped++;
                continue;
            }
            try {
                memberRemover.remove(member);
                removed++;
            } catch (Exception e) {
                // 한 건의 실패가 나머지를 막지 않게 삼킨다. 원인은 로그로 남긴다.
                failed++;
                log.warn("탈퇴 회원 정리 실패 — memberId={}, loginId={}",
                        member.getId(), member.getLoginId(), e);
            }
        }

        if (removed > 0 || failed > 0) {
            log.info("탈퇴 회원 정리 — 삭제 {}건, 실패 {}건, 관리자 제외 {}건", removed, failed, skipped);
        }
    }
}
