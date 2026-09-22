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
        // [VULN-9] 상세 조회에 작성자 검증이 없다(IDOR). findInquiry 는 id 로 찾아오기만 할 뿐
        //          그 문의를 쓴 사람이 요청자인지 보지 않는다. 애초에 이 메서드는 요청자가 누구인지
        //          받지도 않는다(파라미터가 inquiryId 하나뿐이다).
        //          → 문의 id 만 바꾸면 남의 문의를 본문·첨부·링크·관리자 답변까지 그대로 볼 수 있다.
        //            문의에는 개인정보나 결제 문제 같은 민감한 내용이 담기므로 피해가 작지 않다.
        //          ※ 비교 기준은 이미 있다 — 목록 조회(getMyInquiries)는 findByMemberId 로 본인 것만
        //            걸러 내려준다. 즉 "누가 작성자인지" 판단하는 방법이 코드에 존재하는데
        //            상세·수정·삭제에서만 그것을 쓰지 않는다.
        //          방어: member 를 파라미터로 받아 inquiry.getMember() 와 대조한다.
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
        // [VULN-9] 수정도 작성자 검증이 없다 — inquiry.getMember() 가 이 문의를 실제로 쓴 사람인데,
        //          그 사람과 요청자(member)가 같은지 비교하지 않는다. 그래서 로그인만 되어 있으면
        //          문의 id 만 바꿔 남의 문의 제목·본문·첨부·링크를 마음대로 고칠 수 있다(IDOR).
        //          바로 위 isAnswered() 검사는 통과하므로, 아직 답변이 안 달린 남의 문의가 표적이 된다.
        //          방어: if (!inquiry.getMember().getId().equals(member.getId())) throw new AccessDenied...;
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
        // [VULN-9] 삭제도 작성자 검증이 없다 — inquiry.getMember()(실제 작성자)와 요청자(member)를
        //          비교하는 코드가 없어, 문의 id 만 알면 남의 문의를 지울 수 있다.
        //
        //   이 삭제가 특히 무거운 이유는 DB 행에서 끝나지 않기 때문이다. 바로 아래에서
        //     · 관리자 답변을 지우고(deleteByInquiryId)
        //     · 첨부 파일의 S3 실물까지 지운 뒤(inquiryFileStorageService::delete)
        //     · 마지막에 문의 행을 지운다.
        //   즉 피해가 DB 한 줄이 아니라 저장소의 실제 파일로 번진다.
        //
        //   복구 가능성(2026-09-07 인프라 설정 확인 결과, 낙관하면 안 된다):
        //     · S3 버킷은 버저닝이 켜져 있고(infra/storage.tf) 코드는 DeleteObject 만 호출하므로
        //       이전 버전 자체는 남는다. 다만 자동 복구는 없고 운영자가 손으로 되살려야 한다.
        //     · 노후 버전 만료 정책이 30일이라, 그 안에 알아채지 못하면 영구히 사라진다.
        //     · DB 백업 보존은 7일이라 기간이 서로 다르다. 문의 행과 첨부를 "같은 시점으로"
        //       한 번에 되돌리는 것은 불가능하고, 손으로 짜맞춰야 한다.
        //   방어: if (!inquiry.getMember().getId().equals(member.getId())) throw new AccessDenied...;
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
