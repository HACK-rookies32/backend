package com.example.kidhackbackend.global.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * presigned 업로드 URL 발급 결과.
 *
 * <p>클라이언트는 uploadUrl 로 PUT(헤더에 contentType) → filename 을 완료 API 로 확인 →
 * 완료 API 가 준 경로를 게시글/문의 작성 요청에 담는다.
 */
@Schema(description = "presigned 업로드 URL 발급 결과")
@Getter
@AllArgsConstructor
public class PresignResponse {

    @Schema(description = "브라우저가 PUT 할 S3 주소(서명 포함 — 노출·재사용 주의)")
    private String uploadUrl;

    @Schema(description = "업로드 완료 후 이 파일을 가리킬 공개 경로")
    private String path;

    @Schema(description = "서버가 정한 저장 파일명(완료 API 에 그대로 전달)")
    private String filename;

    @Schema(description = "PUT 요청에 그대로 실어야 하는 Content-Type", example = "image/jpeg")
    private String contentType;

    @Schema(description = "서명 유효 기간(초)", example = "300")
    private long expiresInSeconds;
}
