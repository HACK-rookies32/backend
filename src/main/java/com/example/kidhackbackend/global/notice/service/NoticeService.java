package com.example.kidhackbackend.global.notice.service;

import com.example.kidhackbackend.admin.notice.dto.AdminNoticeDetailResponse;
import com.example.kidhackbackend.admin.notice.dto.AdminNoticeListResponse;
import com.example.kidhackbackend.admin.notice.dto.AdminNoticePageResponse;
import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.global.notice.dto.NoticeListItemResponse;
import com.example.kidhackbackend.global.notice.dto.NoticePageResponse;
import com.example.kidhackbackend.global.notice.dto.NoticeRequest;
import com.example.kidhackbackend.global.notice.dto.NoticeResponse;
import com.example.kidhackbackend.global.notice.entity.Notice;
import com.example.kidhackbackend.global.notice.repository.NoticeRepository;
import com.example.kidhackbackend.global.storage.AttachmentValidator;
import com.example.kidhackbackend.global.storage.UploadCategory;
import com.example.kidhackbackend.users.member.entity.Member;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공지 CRUD.
 *
 * <p><b>알림을 보내지 않는다.</b> 예전에는 {@code createNotice()} 가 저장 직후 전 회원에게
 * 푸시를 보냈다. 그래서 공지를 하나 쓸 때마다 알림이 나갔고, 오타를 고치려고 지웠다 다시
 * 쓰면 또 나갔다. 관리자 웹은 「공지 작성」과 「전체 알림 발송」을 별도 버튼으로 두고 있어
 * 알림을 원하면 {@code POST /api/admin/notifications/broadcast} 를 따로 호출한다.
 *
 * <p>응답이 둘로 갈린다. 관리자 웹은 작성자 이름과 첨부·링크 배열이 필요하고, 모바일은
 * 목록에서 본문 없이 개수만, 상세에서 배열을 받는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final AttachmentValidator attachmentValidator;

    /* ---------------------------------------------------------------- 관리자 */

    public AdminNoticePageResponse getNoticesForAdmin(String keyword, Pageable pageable) {
        Page<AdminNoticeListResponse> page = search(keyword, pageable).map(AdminNoticeListResponse::from);
        return new AdminNoticePageResponse(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    public AdminNoticeDetailResponse getNoticeForAdmin(Long noticeId) {
        return AdminNoticeDetailResponse.from(findNotice(noticeId));
    }

    /** 공지를 저장한다. 알림은 나가지 않는다. */
    @Transactional
    public AdminNoticeDetailResponse createNotice(Member author, NoticeRequest request) {
        attachmentValidator.requireAllUploaded(request.getAttachments(), UploadCategory.NOTICE);
        Notice notice = new Notice(
                request.getTitle(),
                request.getContent(),
                request.getAttachments(),
                request.getLinks(),
                author
        );
        noticeRepository.save(notice);
        return AdminNoticeDetailResponse.from(notice);
    }

    @Transactional
    public AdminNoticeDetailResponse updateNotice(Long noticeId, NoticeRequest request) {
        attachmentValidator.requireAllUploaded(request.getAttachments(), UploadCategory.NOTICE);
        Notice notice = findNotice(noticeId);
        notice.update(request.getTitle(), request.getContent(),
                request.getAttachments(), request.getLinks());
        return AdminNoticeDetailResponse.from(notice);
    }

    @Transactional
    public void deleteNotice(Long noticeId) {
        noticeRepository.delete(findNotice(noticeId));
    }

    /* ---------------------------------------------------------------- 사용자 */

    /** 목록 — 본문 없이 개수만. keyword 가 있으면 제목에서 검색. 최신순. */
    public NoticePageResponse getNotices(String keyword, Pageable pageable) {
        Page<NoticeListItemResponse> page = search(keyword, pageable).map(NoticeListItemResponse::from);
        return new NoticePageResponse(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }

    public NoticeResponse getNotice(Long noticeId) {
        return NoticeResponse.from(findNotice(noticeId));
    }

    /* ------------------------------------------------------------------ 공통 */

    private Page<Notice> search(String keyword, Pageable pageable) {
        String kw = (keyword == null) ? "" : keyword.trim();
        return kw.isEmpty()
                ? noticeRepository.findAllByOrderByCreatedAtDesc(pageable)
                : noticeRepository.searchByTitle(kw, pageable);
    }

    private Notice findNotice(Long noticeId) {
        return noticeRepository.findById(noticeId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 공지입니다: " + noticeId));
    }
}
