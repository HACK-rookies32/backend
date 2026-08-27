package com.example.kidhackbackend.global.notice.service;

import com.example.kidhackbackend.admin.notification.service.BroadcastNotificationService;
import com.example.kidhackbackend.global.notice.dto.NoticeRequest;
import com.example.kidhackbackend.global.notice.dto.NoticeResponse;
import com.example.kidhackbackend.global.notice.entity.Notice;
import com.example.kidhackbackend.global.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final BroadcastNotificationService broadcastNotificationService;

    public Page<NoticeResponse> getNotices(Pageable pageable) {
        return noticeRepository.findAllByOrderByCreatedAtDesc(pageable).map(NoticeResponse::from);
    }

    public NoticeResponse getNotice(Long noticeId) {
        return NoticeResponse.from(findNotice(noticeId));
    }

    @Transactional
    public NoticeResponse createNotice(Long authorId, NoticeRequest request) {
        Notice notice = new Notice(
                request.getTitle(),
                request.getContent(),
                request.getAttachmentUrl(),
                request.getLinkUrl(),
                authorId
        );
        noticeRepository.save(notice);

        broadcastNotificationService.broadcast("새 공지가 등록되었습니다: " + notice.getTitle());

        return NoticeResponse.from(notice);
    }

    @Transactional
    public NoticeResponse updateNotice(Long noticeId, NoticeRequest request) {
        Notice notice = findNotice(noticeId);
        notice.update(request.getTitle(), request.getContent(), request.getAttachmentUrl(), request.getLinkUrl());
        return NoticeResponse.from(notice);
    }

    @Transactional
    public void deleteNotice(Long noticeId) {
        noticeRepository.delete(findNotice(noticeId));
    }

    private Notice findNotice(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지입니다: " + noticeId));
    }
}