package com.example.kidhackbackend.users.screenshot.dto;

import com.example.kidhackbackend.users.screenshot.entity.CaptureTrigger;
import com.example.kidhackbackend.users.screenshot.entity.Screenshot;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 캡처 목록 항목 (명세서 4.6 GET).
 *
 * <p>명세가 목록 필드를 정하지 않아 여기서 정의한다. 부모가 언제·어떤 계기로 찍힌
 * 이미지인지 알고 열 수 있도록 촬영시각과 트리거를 함께 내린다.
 */
@Getter
@AllArgsConstructor
public class ScreenshotResponse {

    private Long screenshotId;
    private String imageUrl;
    private LocalDateTime capturedAt;
    private CaptureTrigger trigger;

    public static ScreenshotResponse from(Screenshot screenshot) {
        return new ScreenshotResponse(
                screenshot.getId(),
                screenshot.getImagePath(),
                screenshot.getCapturedAt(),
                screenshot.getTrigger());
    }
}
