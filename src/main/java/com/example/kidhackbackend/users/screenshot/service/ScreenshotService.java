package com.example.kidhackbackend.users.screenshot.service;

import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.service.NotificationPort;
import com.example.kidhackbackend.global.storage.ObjectStorage;
import com.example.kidhackbackend.global.storage.UploadCategory;
import com.example.kidhackbackend.users.screenshot.dto.ScreenshotPageResponse;
import com.example.kidhackbackend.users.screenshot.dto.ScreenshotResponse;
import com.example.kidhackbackend.users.screenshot.dto.ScreenshotUploadResponse;
import com.example.kidhackbackend.users.screenshot.entity.CaptureTrigger;
import com.example.kidhackbackend.users.screenshot.entity.Screenshot;
import com.example.kidhackbackend.users.screenshot.repository.ScreenshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 자녀 화면 캡처 (명세서 4.6).
 *
 * <p>업로드는 명세대로 multipart 다. 이미지는 {@link ObjectStorage} 로 S3(운영) 또는
 * 로컬 디스크(개발)에 저장하고, 촬영 맥락만 {@link Screenshot} 로 DB 에 남긴다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScreenshotService {

    private final ObjectStorage storage;
    private final ScreenshotRepository screenshotRepository;
    private final NotificationPort notificationPort;

    /**
     * 자녀 앱이 캡처 이미지를 올린다.
     *
     * <p>1시간 주기 자동 촬영({@code SCHEDULED})이든 부모 요청 추가 촬영({@code MANUAL})
     * 이든 같은 경로로 들어오며, 계기는 {@code trigger} 로만 구분한다.
     */
    @Transactional
    public ScreenshotUploadResponse upload(Long childId, MultipartFile file,
                                           LocalDateTime capturedAt, CaptureTrigger trigger) {
        // [VULN-10] 확장자 화이트리스트 검증이 없다. 커뮤니티/문의 업로드와 같은 수준으로 맞춘다.
        String savedFilename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String imagePath = storage.store(UploadCategory.SCREENSHOT, savedFilename, file);

        Screenshot screenshot = screenshotRepository.save(
                new Screenshot(childId, imagePath, capturedAt, trigger));
        return ScreenshotUploadResponse.from(screenshot);
    }

    /**
     * 부모가 캡처 목록을 조회한다. from/to 가 없으면 전체 기간이다.
     */
    public ScreenshotPageResponse list(Long childId, LocalDateTime from, LocalDateTime to,
                                       int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Screenshot> result = (from != null && to != null)
                ? screenshotRepository.findByChildIdAndCapturedAtBetweenOrderByCapturedAtDesc(
                        childId, from, to, pageable)
                : screenshotRepository.findByChildIdOrderByCapturedAtDesc(childId, pageable);

        return new ScreenshotPageResponse(
                result.getContent().stream().map(ScreenshotResponse::from).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    /**
     * 부모가 즉시 촬영을 요청한다. 자녀 기기로 푸시만 보내고, 실제 이미지는 자녀 앱이
     * {@link #upload} 로 {@code MANUAL} 캡처를 올려 완성된다.
     */
    public void requestCapture(Long childId) {
        notificationPort.publish(childId, NotificationType.SCREENSHOT_REQUESTED,
                "부모가 화면 캡처를 요청했습니다.");
    }
}
