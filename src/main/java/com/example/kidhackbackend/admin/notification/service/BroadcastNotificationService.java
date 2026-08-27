package com.example.kidhackbackend.admin.notification.service;

import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.service.NotificationService;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BroadcastNotificationService {

    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    /**
     * 탈퇴하지 않은 전체 회원에게 알림을 보낸다.
     *
     * <p>회원별로 publish 를 돌리면 FCM 을 인원수만큼 호출한다(1,000 명이면 1,000 회).
     * 수신자 목록을 통째로 넘겨 멀티캐스트로 보낸다.
     *
     * <p>이 메서드는 <b>공지를 저장하지 않는다.</b> 공지 작성과 알림 발송은 별개의
     * API 이고, 관리자 웹도 두 버튼으로 나눠 두었다.
     */
    public void broadcast(NotificationType type, String title, String body) {
        List<Long> recipientIds = memberRepository.findByIsDeletedFalse().stream()
                .map(Member::getId)
                .toList();
        notificationService.publishAll(recipientIds, type, title, body);
    }
}