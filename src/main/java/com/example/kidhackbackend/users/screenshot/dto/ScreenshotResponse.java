package com.example.kidhackbackend.users.screenshot.dto;

import com.example.kidhackbackend.users.screenshot.entity.CaptureTrigger;
import com.example.kidhackbackend.users.screenshot.entity.Screenshot;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 캡처 목록 항목 (명세서 4.6 GET).
 */
@Schema(description = "캡처 목록 항목")
@Getter
@AllArgsConstructor
public class ScreenshotResponse {

    @Schema(description = "캡처 ID", example = "1")
    private Long screenshotId;

    @Schema(description = "이미지 조회 경로(/api/files/... — 조회 시 presigned URL 로 302 리다이렉트)")
    private String imageUrl;

    @Schema(description = "촬영 시각")
    private LocalDateTime capturedAt;

    @Schema(description = "촬영 계기: SCHEDULED(1시간 주기 자동) / MANUAL(부모 요청)", example = "SCHEDULED")
    private CaptureTrigger trigger;

    public static ScreenshotResponse from(Screenshot screenshot) {
        return new ScreenshotResponse(
                screenshot.getId(),
                screenshot.getImagePath(),
                screenshot.getCapturedAt(),
                screenshot.getTrigger());
    }
}
