package com.example.kidhackbackend.users.inquiry.service;

import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.global.storage.AttachmentValidator;
import com.example.kidhackbackend.global.storage.UploadCategory;
import com.example.kidhackbackend.users.inquiry.dto.*;
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

// ╔══════════════════════════════════════════════════════════════════════════════╗
// ║ [VULN-SSRF] 이 파일에 추가된 내용 요약 (PL 확인용) — 금융보안 진단 61번 SSRF 재현    ║
// ╠══════════════════════════════════════════════════════════════════════════════╣
// ║ 무엇을     : 문의 작성(createInquiry) 시, 사용자가 넣은 linkUrl 로 서버가 대신       ║
// ║             HTTP 요청을 보내게 하는 SSRF 취약점을 심었다.                          ║
// ║ 왜 취약     : linkUrl 에 스킴(http/https)·사설IP(127.0.0.1,10.x…)·allowlist 검증이  ║
// ║             전혀 없어, 공격자가 내부망/클라우드 메타데이터 주소를 서버에게 대신       ║
// ║             호출시키고 그 응답을 빼낼 수 있다.                                     ║
// ║ 바뀐 곳(3군데, 모두 아래에 [VULN-SSRF] 주석으로 표시):                             ║
// ║   (1) import 구역   : SSRF 요청에 쓰는 java.net.http 관련 import (주석)            ║
// ║   (2) 필드 구역     : ssrfHttpClient 필드 (주석)                                  ║
// ║   (3) createInquiry : inquiryRepository.save(inquiry) 직후 SSRF 호출 블록 (주석)   ║
// ║ 현재 상태  : 전부 주석 처리(비활성). 기존 코드는 한 줄도 삭제/변경하지 않음.          ║
// ║ 활성화 방법: 위 3군데의 // 를 모두 해제 → 서버 재시작.                              ║
// ║ 원복 방법  : 3군데의 [VULN-SSRF] 블록을 다시 주석 처리하거나 삭제(기존 로직 무영향). ║
// ║ 재현       : PARENT 로그인 → 문의 작성 시 linkUrl 에                               ║
// ║             http://127.0.0.1:8080/actuator/health 입력 → 콘솔에 서버 내부 응답 노출 ║
// ╚══════════════════════════════════════════════════════════════════════════════╝
// [VULN-SSRF] (1) 아래 import 는 문의 linkUrl SSRF 재현용. 주석 해제 시 함께 해제할 것.

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryAnswerRepository inquiryAnswerRepository;
    private final InquiryFileStorageService inquiryFileStorageService;
    private final AttachmentValidator attachmentValidator;

    // [VULN-SSRF] (2) 문의 linkUrl 을 서버가 대신 방문할 때 쓰는 HTTP 클라이언트.
    //             connectTimeout 은 무한 대기 방지용일 뿐, SSRF 를 막는 안전장치가 아니다.
     private final java.net.http.HttpClient ssrfHttpClient = java.net.http.HttpClient.newBuilder()
             .connectTimeout(java.time.Duration.ofSeconds(3))
             .build();

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

        // ┌───────────────────────────────────────────────────────────────────────┐
        // │ [VULN-SSRF-1] (3) 서버 사이드 요청 위조 — 금융보안 진단 61번 재현            │
        // │ 위치 : createInquiry(), 문의 저장(save) 바로 다음.                         │
        // │ 흐름 : 사용자가 넣은 linkUrl 을 "링크가 살아있는지 서버가 확인한다"는 명목으로 │
        // │        서버가 그 주소로 직접 GET 요청을 보낸다.                             │
        // │ 취약점:                                                                  │
        // │   · linkUrl 스킴 제한 없음 (http/https 외 file:// 등도 시도됨)             │
        // │   · 127.0.0.1 / localhost / 사설IP(10.x·172.16~31.x·192.168.x) 차단 없음   │
        // │   · 클라우드 메타데이터(169.254.169.254) 차단 없음                          │
        // │   · 허용 도메인 allowlist 없음                                            │
        // │ [VULN-SSRF-2] 서버가 받아온 내부 응답을 그대로 콘솔에 노출한다.              │
        // │ ※ 주석 해제 시 위 (1)import·(2)필드 도 함께 해제할 것.                      │
        // └───────────────────────────────────────────────────────────────────────┘
        String linkUrl = request.getLinkUrl();
         if (linkUrl != null && !linkUrl.isBlank()) {
             try {
                 // [VULN-SSRF-1] linkUrl 을 검증 없이 그대로 목적지로 사용한다.
                 java.net.http.HttpRequest ssrfReq = java.net.http.HttpRequest.newBuilder()
                         .uri(java.net.URI.create(linkUrl))
                         .timeout(java.time.Duration.ofSeconds(5))
                         .GET()
                         .build();
                 java.net.http.HttpResponse<String> ssrfRes =
                         ssrfHttpClient.send(ssrfReq, java.net.http.HttpResponse.BodyHandlers.ofString());
                 // [VULN-SSRF-2] 서버가 대신 받아온 내부 응답을 가공 없이 그대로 노출한다.
                 System.out.println("[VULN-SSRF] inquiryId=" + inquiry.getId()
                         + " linkUrl=" + linkUrl
                         + " status=" + ssrfRes.statusCode()
                         + " body=" + ssrfRes.body());
             } catch (Exception e) {
                 System.out.println("[VULN-SSRF] linkUrl 호출 실패: " + linkUrl + " / " + e.getMessage());
             }
         }
        // ── [VULN-SSRF] 블록 끝 ──

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