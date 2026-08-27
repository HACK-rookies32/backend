package com.example.kidhackbackend.screenshot;

import com.example.kidhackbackend.global.config.JpaAuditingConfig;
import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.service.NotificationPort;
import com.example.kidhackbackend.global.storage.ObjectStorage;
import com.example.kidhackbackend.global.storage.StoredObject;
import com.example.kidhackbackend.global.storage.UploadCategory;
import com.example.kidhackbackend.users.screenshot.dto.ScreenshotPageResponse;
import com.example.kidhackbackend.users.screenshot.dto.ScreenshotUploadResponse;
import com.example.kidhackbackend.users.screenshot.entity.CaptureTrigger;
import com.example.kidhackbackend.users.screenshot.repository.ScreenshotRepository;
import com.example.kidhackbackend.users.screenshot.service.ScreenshotService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 자녀 화면 캡처 서비스 (명세서 4.6).
 *
 * <p>저장소와 알림은 이 테스트가 검증할 대상이 아니라 "무엇을 넘겼는가" 만 보면 되므로
 * 기록만 하는 가짜 구현을 끼운다. DB 는 다른 서비스 테스트와 같이 실제 JPA 로 돈다.
 */
@DataJpaTest
@Import({ScreenshotService.class, JpaAuditingConfig.class, ScreenshotServiceTest.Fakes.class})
class ScreenshotServiceTest {

    @Autowired
    ScreenshotService service;

    @Autowired
    ScreenshotRepository repository;

    @Autowired
    RecordingStorage storage;

    @Autowired
    RecordingNotifications notifications;

    @Test
    void 캡처를_screenshot_분류로_저장하고_메타데이터를_남긴다() {
        LocalDateTime capturedAt = LocalDateTime.now().minusMinutes(3);

        ScreenshotUploadResponse res = service.upload(
                1L, image(), capturedAt, CaptureTrigger.SCHEDULED);

        // 이미지 실물은 S3(또는 로컬)로 가고, 분류가 screenshot 이어야 uploads/screenshot 아래에 쌓인다.
        assertThat(storage.lastCategory).isEqualTo(UploadCategory.SCREENSHOT);

        // 응답의 imageUrl 은 만료되는 presigned URL 이 아니라 우리 서버 경로다.
        // 이 값이 그대로 DB 에 남고, 열 때 FileController 가 presigned 로 리다이렉트한다.
        assertThat(res.getImageUrl()).startsWith("/api/files/screenshot/");
        assertThat(StoredObject.parse(res.getImageUrl())).isPresent();

        assertThat(repository.findById(res.getScreenshotId())).hasValueSatisfying(saved -> {
            assertThat(saved.getChildId()).isEqualTo(1L);
            assertThat(saved.getTrigger()).isEqualTo(CaptureTrigger.SCHEDULED);
            // 서버 수신 시각이 아니라 자녀 앱이 찍은 시각이 남아야 한다.
            assertThat(saved.getCapturedAt()).isEqualTo(capturedAt);
        });
    }

    @Test
    void 저장_파일명은_서버가_만든다() {
        service.upload(1L, image(), LocalDateTime.now(), CaptureTrigger.SCHEDULED);

        // 클라이언트가 준 이름을 그대로 키에 쓰면 같은 이름끼리 덮어쓴다.
        assertThat(storage.lastFilename).isNotEqualTo("capture.jpg").endsWith("_capture.jpg");
    }

    @Test
    void 목록은_최신_촬영순이고_기간으로_걸러진다() {
        LocalDateTime now = LocalDateTime.now();
        service.upload(1L, image(), now.minusDays(5), CaptureTrigger.SCHEDULED);
        service.upload(1L, image(), now.minusHours(2), CaptureTrigger.MANUAL);
        service.upload(2L, image(), now.minusHours(1), CaptureTrigger.SCHEDULED); // 다른 자녀

        ScreenshotPageResponse all = service.list(1L, null, null, 0, 20);
        assertThat(all.getTotalElements()).isEqualTo(2);
        assertThat(all.getContent().get(0).getTrigger()).isEqualTo(CaptureTrigger.MANUAL);

        ScreenshotPageResponse recent = service.list(1L, now.minusDays(1), now, 0, 20);
        assertThat(recent.getTotalElements()).isEqualTo(1);
    }

    @Test
    void 추가_촬영_요청은_자녀_기기로_간다() {
        service.requestCapture(42L);

        // 수신자가 부모가 아니라 자녀여야 자녀 앱이 촬영 트리거를 받는다.
        // (자녀도 role=CHILD 인 Member 라 childId 가 곧 수신자 memberId 다)
        assertThat(notifications.recipients).containsExactly(42L);
        assertThat(notifications.types).containsExactly(NotificationType.SCREENSHOT_REQUESTED);
    }

    private MultipartFile image() {
        return new MockMultipartFile("file", "capture.jpg", "image/jpeg", "fake-image".getBytes());
    }

    @TestConfiguration
    static class Fakes {

        @Bean
        RecordingStorage recordingStorage() {
            return new RecordingStorage();
        }

        @Bean
        RecordingNotifications recordingNotifications() {
            return new RecordingNotifications();
        }
    }

    /** 넘어온 분류와 파일명만 기록하고, 실제 저장소처럼 공개 경로를 돌려준다. */
    static class RecordingStorage implements ObjectStorage {

        UploadCategory lastCategory;
        String lastFilename;

        @Override
        public String store(UploadCategory category, String filename, MultipartFile file) {
            lastCategory = category;
            lastFilename = filename;
            return new StoredObject(category, filename).publicPath();
        }

        @Override
        public Optional<URI> uploadUrl(UploadCategory category, String filename, String contentType) {
            return Optional.empty();
        }

        @Override
        public boolean exists(UploadCategory category, String filename) {
            return true;
        }

        @Override
        public void delete(UploadCategory category, String filename) {
        }

        @Override
        public Optional<URI> downloadUrl(UploadCategory category, String filename) {
            return Optional.empty();
        }

        @Override
        public Resource loadAsResource(UploadCategory category, String filename) {
            return null;
        }
    }

    /** 발송하지 않고 수신자와 유형만 모은다. */
    static class RecordingNotifications implements NotificationPort {

        final List<Long> recipients = new ArrayList<>();
        final List<NotificationType> types = new ArrayList<>();

        @Override
        public void publish(Long recipientId, NotificationType type, String message) {
            recipients.add(recipientId);
            types.add(type);
        }
    }
}
