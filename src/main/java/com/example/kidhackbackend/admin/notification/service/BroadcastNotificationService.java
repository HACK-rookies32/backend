package com.example.kidhackbackend.admin.notification.service;

import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.service.NotificationService;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BroadcastNotificationService {

    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    public void broadcast(String message) {
        memberRepository.findByIsDeletedFalse()
                .forEach(member -> notificationService.publish(member.getId(), NotificationType.NOTICE, message));
    }
}
