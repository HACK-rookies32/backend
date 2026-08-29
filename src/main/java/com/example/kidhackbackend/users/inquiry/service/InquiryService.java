package com.example.kidhackbackend.users.inquiry.service;

import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.global.storage.AttachmentValidator;
import com.example.kidhackbackend.global.storage.UploadCategory;
import com.example.kidhackbackend.users.inquiry.dto.InquiryDetailResponse;
import com.example.kidhackbackend.users.inquiry.dto.InquiryRequest;
import com.example.kidhackbackend.users.inquiry.dto.InquiryResponse;
import com.example.kidhackbackend.users.inquiry.dto.InquiryAnswerResponse;
import com.example.kidhackbackend.users.inquiry.dto.InquiryPageResponse;
import com.example.kidhackbackend.users.inquiry.entity.Inquiry;
import com.example.kidhackbackend.users.inquiry.repository.InquiryAnswerRepository;
import com.example.kidhackbackend.users.inquiry.repository.InquiryRepository;
import com.example.kidhackbackend.users.member.entity.Member;
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
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryAnswerRepository inquiryAnswerRepository;
    private final InquiryFileStorageService inquiryFileStorageService;
    private final AttachmentValidator attachmentValidator;

    @Transactional
    public InquiryResponse createInquiry(Member member, InquiryRequest request) {
        attachmentValidator.requireAllUploaded(request.getAttachments(), UploadCategory.INQUIRY);
        Inquiry inquiry = new Inquiry();
        inquiry.setMember(member);
        inquiry.setTitle(request.getTitle());
        inquiry.setContent(request.getContent());
        if (request.getAttachments() != null) {
            inquiry.setAttachments(new ArrayList<>(request.getAttachments()));
        }
        inquiry.setLinkUrl(request.getLinkUrl());

        inquiryRepository.save(inquiry);
        return toResponse(inquiry);
    }

    /** 본인 문의만 최신순으로 페이징. */
    public InquiryPageResponse getMyInquiries(Member member, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Inquiry> result = inquiryRepository.findByMemberIdOrderByCreatedAtDesc(member.getId(), pageable);

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
        // [VULN-9] findInquiry 만 하고 작성자(member) 검증이 없다(IDOR) — id 만 알면 남의 문의를
        //          본문·첨부·링크·답변까지 그대로 열람할 수 있다. 목록(getMyInquiries)은 본인 것만
        //          나오지만 상세는 막지 않는다.
        return toDetailResponse(findInquiry(inquiryId));
    }

    @Transactional
    public InquiryResponse updateInquiry(Long inquiryId, Member member, InquiryRequest request) {
        Inquiry inquiry = findInquiry(inquiryId);
        // 답변이 등록되면 회원이 내용을 바꿀 수 없다(관리자 답변과 본문이 어긋나는 것을 막는다).
        if (inquiry.isAnswered()) {
            throw new IllegalArgumentException("답변이 완료된 문의는 수정할 수 없습니다.");
        }
        attachmentValidator.requireAllUploaded(request.getAttachments(), UploadCategory.INQUIRY);
        // [VULN-9] inquiry.getMember() 와 요청자(member) 비교가 없음(IDOR).
        inquiry.setTitle(request.getTitle());
        inquiry.setContent(request.getContent());
        if (request.getAttachments() != null) {
            inquiry.setAttachments(new ArrayList<>(request.getAttachments()));
        }
        inquiry.setLinkUrl(request.getLinkUrl());
        return toResponse(inquiry);
    }

    @Transactional
    public void deleteInquiry(Long inquiryId, Member member) {
        Inquiry inquiry = findInquiry(inquiryId);
        if (inquiry.isAnswered()) {
            throw new IllegalArgumentException("답변이 완료된 문의는 삭제할 수 없습니다.");
        }
        // [VULN-9] inquiry.getMember() 와 요청자(member) 비교가 없음(IDOR). 아래 첨부 삭제가
        //          피해 범위를 DB 행에서 실제 S3 객체로 넓힌다(버저닝이라 복구 여지는 있음).
        inquiryAnswerRepository.deleteByInquiryId(inquiry.getId());
        inquiry.getAttachments().forEach(inquiryFileStorageService::delete);
        inquiryRepository.delete(inquiry);
    }

    private Inquiry findInquiry(Long inquiryId) {
        return inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 문의입니다."));
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
                .map(a -> new InquiryAnswerResponse(a.getId(), a.getContent(), a.getCreatedAt(), a.getUpdatedAt()))
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
}
