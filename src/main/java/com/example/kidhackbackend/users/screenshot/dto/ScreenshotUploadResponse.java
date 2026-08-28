package com.example.kidhackbackend.users.screenshot.dto;

import com.example.kidhackbackend.users.screenshot.entity.Screenshot;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 캡처 업로드 결과 (명세서 4.6).
 */
@Schema(description = "캡처 업로드 결과")
@Getter
@AllArgsConstructor
public class ScreenshotUploadResponse {

    @Schema(description = "캡처 ID", example = "1")
    private Long screenshotId;

    @Schema(description = "이미지 조회 경로(/api/files/...)")
    private String imageUrl;

    public static ScreenshotUploadResponse from(Screenshot screenshot) {
        return new ScreenshotUploadResponse(screenshot.getId(), screenshot.getImagePath());
    }
}
