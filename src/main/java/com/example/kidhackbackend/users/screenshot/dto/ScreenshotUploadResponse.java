package com.example.kidhackbackend.users.screenshot.dto;

import com.example.kidhackbackend.users.screenshot.entity.Screenshot;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 캡처 업로드 결과 (명세서 4.6).
 *
 * <p>명세 예시는 {@code imageUrl} 이 S3 직링크지만, 버킷이 private 이라 서명 없이는
 * 열리지 않는다. 커뮤니티 첨부와 동일하게 우리 서버 경로를 돌려주고, 조회 시
 * FileController 가 presigned URL 로 리다이렉트한다.
 */
@Getter
@AllArgsConstructor
public class ScreenshotUploadResponse {

    private Long screenshotId;
    private String imageUrl;

    public static ScreenshotUploadResponse from(Screenshot screenshot) {
        return new ScreenshotUploadResponse(screenshot.getId(), screenshot.getImagePath());
    }
}
