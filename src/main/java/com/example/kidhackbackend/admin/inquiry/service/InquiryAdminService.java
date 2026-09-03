package com.example.kidhackbackend.admin.inquiry.service;

import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.service.NotificationPort;
import com.example.kidhackbackend.users.inquiry.dto.InquiryDetailResponse;
import com.example.kidhackbackend.users.inquiry.dto.InquiryResponse;
import com.example.kidhackbackend.users.inquiry.dto.InquiryAnswerRequest;
import com.example.kidhackbackend.users.inquiry.dto.InquiryAnswerResponse;
import com.example.kidhackbackend.users.inquiry.dto.InquiryPageResponse;
import com.example.kidhackbackend.users.inquiry.entity.Inquiry;
import com.example.kidhackbackend.users.inquiry.entity.InquiryAnswer;
import com.example.kidhackbackend.users.inquiry.repository.InquiryAnswerRepository;
import com.example.kidhackbackend.users.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryAdminService {

    private final InquiryRepository inquiryRepository;
    private final InquiryAnswerRepository inquiryAnswerRepository;
    private final NotificationPort notificationPort;

    /**
     * 문의 목록.
     *
     * <p>{@code memberId} 를 주면 그 회원이 쓴 것만 본다. 관리자 웹의 회원 상세에 있는
     * "문의 기록" 이 이 필터를 쓴다 — 예전에는 파라미터가 없어 전체 회원의 문의가
     * 내려갔다(모르는 쿼리 파라미터는 조용히 무시된다).
     */
    public InquiryPageResponse getAllInquiries(Long memberId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Inquiry> result = (memberId == null)
                ? inquiryRepository.findAllByOrderByCreatedAtDesc(pageable)
                : inquiryRepository.findByMemberIdOrderByCreatedAtDesc(memberId, pageable);

        List<InquiryResponse> content = result.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());

        return new InquiryPageResponse(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages()
        );
    }

    public InquiryDetailResponse getInquiryDetail(Long inquiryId) {
        Inquiry inquiry = findInquiry(inquiryId);
        return toDetailResponse(inquiry);
    }

    @Transactional
    public InquiryAnswerResponse createAnswer(Long inquiryId, InquiryAnswerRequest request) {
        Inquiry inquiry = findInquiry(inquiryId);
        if (inquiryAnswerRepository.findByInquiryId(inquiryId).isPresent()) {
            throw new IllegalArgumentException("이미 답변이 등록된 문의입니다.");
        }

        InquiryAnswer answer = new InquiryAnswer();
        answer.setInquiry(inquiry);
        answer.setContent(request.getContent());
        inquiryAnswerRepository.save(answer);

        inquiry.setAnswered(true);

        // 문의한 회원(부모)에게 답변 등록을 알린다. 설정에서 끈 회원은 NotificationService 가 걸러낸다.
        notificationPort.publish(inquiry.getMember().getId(), NotificationType.INQUIRY_ANSWERED,
                "문의하신 '" + inquiry.getTitle() + "' 에 답변이 등록되었습니다.");

        return toAnswerResponse(answer);
    }

    @Transactional
    public InquiryAnswerResponse updateAnswer(Long inquiryId, InquiryAnswerRequest request) {
        InquiryAnswer answer = findAnswer(inquiryId);
        answer.setContent(request.getContent());
        return toAnswerResponse(answer);
    }

    @Transactional
    public void deleteAnswer(Long inquiryId) {
        InquiryAnswer answer = findAnswer(inquiryId);
        inquiryAnswerRepository.delete(answer);
        findInquiry(inquiryId).setAnswered(false);
    }

    private Inquiry findInquiry(Long inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 문의입니다."));
    }

    private InquiryAnswer findAnswer(Long inquiryId) {
        return inquiryAnswerRepository.findByInquiryId(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("등록된 답변이 없습니다."));
    }

    private InquiryResponse toResponse(Inquiry inquiry) {
        return new InquiryResponse(
                inquiry.getId(),
                inquiry.getMember().getId(),
                inquiry.getMember().getNickname(),
                inquiry.getTitle(),
                inquiry.isAnswered(),
                inquiry.getCreatedAt()
        );
    }

    private InquiryDetailResponse toDetailResponse(Inquiry inquiry) {
        InquiryAnswerResponse answerResponse = inquiryAnswerRepository.findByInquiryId(inquiry.getId())
                .map(this::toAnswerResponse)
                .orElse(null);

        return new InquiryDetailResponse(
                inquiry.getId(),
                inquiry.getMember().getId(),
                inquiry.getMember().getNickname(),
                inquiry.getTitle(),
                inquiry.getContent(),
                new ArrayList<>(inquiry.getAttachments()),
                inquiry.getLinkUrl(),
                inquiry.isAnswered(),
                inquiry.getCreatedAt(),
                inquiry.getUpdatedAt(),
                answerResponse
        );
    }

    private InquiryAnswerResponse toAnswerResponse(InquiryAnswer answer) {
        return new InquiryAnswerResponse(
                answer.getId(),
                answer.getContent(),
                answer.getCreatedAt(),
                answer.getUpdatedAt()
        );
    }
}
