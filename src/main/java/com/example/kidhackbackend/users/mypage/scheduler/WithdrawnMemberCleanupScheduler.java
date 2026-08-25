package com.example.kidhackbackend.users.mypage.scheduler;

import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WithdrawnMemberCleanupScheduler {

    private final MemberRepository memberRepository;

    // 1분마다 확인해서, 탈퇴(isDeleted=true) 후 10분 지난 계정을 완전히 삭제
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cleanupWithdrawnMembers() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
        List<Member> targets = memberRepository.findByIsDeletedTrueAndUpdatedAtBefore(cutoff);

        if (!targets.isEmpty()) {
            memberRepository.deleteAll(targets);
        }
    }
}