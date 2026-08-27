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
     * 전체 회원에게 공지 알림.
     *
     * <p>회원별로 publish 를 돌리면 FCM 을 인원수만큼 호출한다(1,000 명이면 1,000 회).
     * 수신자 목록을 통째로 넘겨 멀티캐스트로 보낸다.
     */
    public void broadcast(String message) {
        List<Long> recipientIds = memberRepository.findByIsDeletedFalse().stream()
                .map(Member::getId)
                .toList();
        notificationService.publishAll(recipientIds, NotificationType.NOTICE, message);
    }
}