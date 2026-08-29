package com.example.kidhackbackend.notice;

import com.example.kidhackbackend.admin.notice.dto.AdminNoticeDetailResponse;
import com.example.kidhackbackend.global.config.JpaAuditingConfig;
import com.example.kidhackbackend.global.notice.dto.NoticeRequest;
import com.example.kidhackbackend.global.notice.dto.NoticeResponse;
import com.example.kidhackbackend.global.notice.service.NoticeService;
import com.example.kidhackbackend.global.notification.FakePushSender;
import com.example.kidhackbackend.global.notification.push.PushDispatcher;
import com.example.kidhackbackend.global.notification.service.NotificationService;
import com.example.kidhackbackend.global.notification.service.NotificationSettingService;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 공지 CRUD 슬라이스 테스트.
 *
 * <p>가장 중요한 것은 <b>공지를 써도 알림이 나가지 않는다</b>는 것이다. 예전에는
 * createNotice() 가 저장 직후 전 회원에게 푸시를 보내서, 공지를 하나 쓸 때마다 알림이
 * 나갔고 오타를 고치려고 다시 쓰면 또 나갔다. 관리자 웹은 「공지 작성」과 「전체 알림
 * 발송」을 별도 버튼으로 두고 있다.
 *
 * <p>NotificationService 와 FakePushSender 를 굳이 함께 올리는 이유가 그것이다.
 * 공지 쪽이 알림 쪽을 다시 호출하기 시작하면 이 테스트가 깨진다.
 */
@DataJpaTest
@Import({NoticeService.class, NotificationService.class, NotificationSettingService.class,
        PushDispatcher.class, FakePushSender.class, JpaAuditingConfig.class})
class NoticeServiceTest {

    @Autowired NoticeService noticeService;
    @Autowired MemberRepository memberRepository;
    @Autowired FakePushSender pushSender;

    private Member admin;

    @BeforeEach
    void setUp() {
        pushSender.reset();
        admin = memberRepository.save(admin("super01", "김슈퍼"));
    }

    @Test
    void 공지를_작성해도_알림은_나가지_않는다() {
        // 회원이 더 있어도(=발송 대상이 있어도) 마찬가지다.
        memberRepository.save(admin("parent01", "홍길동"));

        noticeService.createNotice(admin, request("점검 안내", "8/29 새벽 점검", List.of(), List.of()));

        assertThat(pushSender.sent()).isEmpty();
    }

    @Test
    void 관리자_응답은_작성자_이름과_첨부_링크를_담는다() {
        AdminNoticeDetailResponse created = noticeService.createNotice(admin,
                request("공지 제목", "본문",
                        List.of("/api/files/notice/a.png", "/api/files/notice/b.png"),
                        List.of("https://example.com")));

        assertThat(created.id()).isNotNull();
        assertThat(created.authorName()).isEqualTo("김슈퍼");
        assertThat(created.attachments()).hasSize(2);
        assertThat(created.links()).containsExactly("https://example.com");
        assertThat(created.attachmentCount()).isEqualTo(2);
        assertThat(created.linkCount()).isEqualTo(1);
    }

    @Test
    void 목록은_본문없이_개수만_내려준다() {
        noticeService.createNotice(admin,
                request("공지", "본문", List.of("/api/files/notice/a.png"), List.of()));

        assertThat(noticeService.getNoticesForAdmin(null, PageRequest.of(0, 10)).content())
                .singleElement()
                .satisfies(row -> {
                    assertThat(row.authorName()).isEqualTo("김슈퍼");
                    assertThat(row.attachmentCount()).isEqualTo(1);
                    assertThat(row.linkCount()).isZero();
                });
    }

    @Test
    void 수정에서_null_은_유지하고_빈_배열은_비운다() {
        AdminNoticeDetailResponse created = noticeService.createNotice(admin,
                request("원래 제목", "원래 본문",
                        List.of("/api/files/notice/a.png"), List.of("https://example.com")));

        // title 만 바꾸고 나머지는 null → 그대로
        AdminNoticeDetailResponse kept = noticeService.updateNotice(created.id(),
                request("새 제목", null, null, null));
        assertThat(kept.title()).isEqualTo("새 제목");
        assertThat(kept.content()).isEqualTo("원래 본문");
        assertThat(kept.attachments()).hasSize(1);

        // 빈 배열 → 비운다
        AdminNoticeDetailResponse cleared = noticeService.updateNotice(created.id(),
                request(null, null, List.of(), List.of()));
        assertThat(cleared.attachments()).isEmpty();
        assertThat(cleared.links()).isEmpty();
    }

    @Test
    void 사용자용_상세는_첨부_링크를_배열로_내려준다() {
        AdminNoticeDetailResponse created = noticeService.createNotice(admin,
                request("공지", "본문",
                        List.of("/api/files/notice/a.png", "/api/files/notice/b.png"),
                        List.of("https://example.com")));

        NoticeResponse forUser = noticeService.getNotice(created.id());

        assertThat(forUser.id()).isEqualTo(created.id());
        assertThat(forUser.authorId()).isEqualTo(admin.getId());
        assertThat(forUser.content()).isEqualTo("본문");
        assertThat(forUser.attachments())
                .containsExactly("/api/files/notice/a.png", "/api/files/notice/b.png");
        assertThat(forUser.links()).containsExactly("https://example.com");
    }

    @Test
    void 첨부가_없으면_사용자용_상세의_배열은_비어_있다() {
        AdminNoticeDetailResponse created = noticeService.createNotice(admin,
                request("공지", "본문", List.of(), List.of()));

        NoticeResponse forUser = noticeService.getNotice(created.id());

        assertThat(forUser.attachments()).isEmpty();
        assertThat(forUser.links()).isEmpty();
    }

    @Test
    void 사용자_목록은_최신순이고_제목검색이_된다() {
        noticeService.createNotice(admin,
                request("점검 안내", "본문A", List.of("/api/files/notice/a.png"), List.of()));
        noticeService.createNotice(admin,
                request("이벤트 공지", "본문B", List.of(), List.of()));

        var all = noticeService.getNotices(null, PageRequest.of(0, 10));
        assertThat(all.content()).hasSize(2);
        assertThat(all.content().get(0).title()).isEqualTo("이벤트 공지"); // 최신순
        assertThat(all.totalElements()).isEqualTo(2);

        var searched = noticeService.getNotices("점검", PageRequest.of(0, 10));
        assertThat(searched.content()).singleElement()
                .satisfies(row -> {
                    assertThat(row.title()).isEqualTo("점검 안내");
                    assertThat(row.attachmentCount()).isEqualTo(1);
                });
    }

    private NoticeRequest request(String title, String content,
                                  List<String> attachments, List<String> links) {
        NoticeRequest request = new NoticeRequest();
        ReflectionTestUtils.setField(request, "title", title);
        ReflectionTestUtils.setField(request, "content", content);
        ReflectionTestUtils.setField(request, "attachments", attachments);
        ReflectionTestUtils.setField(request, "links", links);
        return request;
    }

    private Member admin(String loginId, String username) {
        Member member = new Member();
        ReflectionTestUtils.setField(member, "loginId", loginId);
        ReflectionTestUtils.setField(member, "username", username);
        ReflectionTestUtils.setField(member, "password", "pw");
        ReflectionTestUtils.setField(member, "phoneNumber", "010-0000-0000");
        ReflectionTestUtils.setField(member, "birthDate", LocalDate.of(1990, 1, 1));
        ReflectionTestUtils.setField(member, "role", Role.SUPER_ADMIN);
        return member;
    }
}
