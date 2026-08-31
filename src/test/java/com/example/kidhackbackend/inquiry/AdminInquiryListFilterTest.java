package com.example.kidhackbackend.inquiry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.kidhackbackend.admin.inquiry.service.InquiryAdminService;
import com.example.kidhackbackend.global.notification.service.NotificationPort;
import com.example.kidhackbackend.users.inquiry.dto.InquiryPageResponse;
import com.example.kidhackbackend.users.inquiry.entity.Inquiry;
import com.example.kidhackbackend.users.inquiry.repository.InquiryAnswerRepository;
import com.example.kidhackbackend.users.inquiry.repository.InquiryRepository;
import com.example.kidhackbackend.users.member.entity.Member;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 관리자 문의 목록의 {@code memberId} 필터.
 *
 * <p>관리자 웹의 회원 상세에 있는 "문의 기록" 이 {@code ?memberId=} 로 부르는데 서버에
 * 그 파라미터가 없었다. 모르는 쿼리 파라미터는 조용히 무시되므로 <b>전체 회원의 문의</b>가
 * 내려갔다 — 오류가 아니라 잘못된 데이터가 나가는 쪽이라 눈에 잘 띄지 않는다.
 */
class AdminInquiryListFilterTest {

    private InquiryRepository inquiryRepository;
    private InquiryAdminService service;

    @BeforeEach
    void setUp() {
        inquiryRepository = mock(InquiryRepository.class);
        service = new InquiryAdminService(
                inquiryRepository,
                mock(InquiryAnswerRepository.class),
                mock(NotificationPort.class));
    }

    private Inquiry inquiry(Long id, Long memberId, String title) {
        Member author = new Member();
        ReflectionTestUtils.setField(author, "id", memberId);
        author.setNickname("작성자" + memberId);

        Inquiry inquiry = new Inquiry();
        ReflectionTestUtils.setField(inquiry, "id", id);
        ReflectionTestUtils.setField(inquiry, "member", author);
        ReflectionTestUtils.setField(inquiry, "title", title);
        return inquiry;
    }

    private Page<Inquiry> page(List<Inquiry> rows) {
        return new PageImpl<>(rows, PageRequest.of(0, 10), rows.size());
    }

    @Test
    void memberId_를_주면_그_회원_문의만_조회한다() {
        when(inquiryRepository.findByMemberIdOrderByCreatedAtDesc(eq(7L), any(Pageable.class)))
                .thenReturn(page(List.of(inquiry(1L, 7L, "내 문의"))));

        InquiryPageResponse response = service.getAllInquiries(7L, 0, 10);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getMemberId()).isEqualTo(7L);
        // 전체 조회로 새지 않아야 한다.
        verify(inquiryRepository, never()).findAllByOrderByCreatedAtDesc(any(Pageable.class));
    }

    @Test
    void memberId_가_없으면_전체를_조회한다() {
        when(inquiryRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .thenReturn(page(List.of(inquiry(1L, 7L, "가"), inquiry(2L, 9L, "나"))));

        InquiryPageResponse response = service.getAllInquiries(null, 0, 10);

        assertThat(response.getContent()).hasSize(2);
        verify(inquiryRepository, never())
                .findByMemberIdOrderByCreatedAtDesc(any(), any(Pageable.class));
    }

    @Test
    void 페이지_정보는_그대로_실린다() {
        when(inquiryRepository.findByMemberIdOrderByCreatedAtDesc(eq(7L), any(Pageable.class)))
                .thenReturn(page(List.of(inquiry(1L, 7L, "내 문의"))));

        InquiryPageResponse response = service.getAllInquiries(7L, 0, 10);

        assertThat(response.getPage()).isZero();
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
    }
}
