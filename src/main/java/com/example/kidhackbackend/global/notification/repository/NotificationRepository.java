package com.example.kidhackbackend.global.notification.repository;

import com.example.kidhackbackend.global.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** 수신자의 알림 목록(최신순) — 페이징. */
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    /** 수신자의 알림 목록(최신순) — 전체. 테스트/내부용. */
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long recipientId);

    long countByRecipientIdAndReadFalse(Long recipientId);

    /** 수신자의 안 읽은 알림을 한 번에 읽음 처리. 반환값은 갱신된 행 수. */
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.read = true where n.recipientId = :recipientId and n.read = false")
    int markAllRead(@Param("recipientId") Long recipientId);
}
