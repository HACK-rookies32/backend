package com.example.kidhackbackend.global.notification.repository;

import com.example.kidhackbackend.global.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** 수신자의 알림 목록(최신순). */
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);
}
