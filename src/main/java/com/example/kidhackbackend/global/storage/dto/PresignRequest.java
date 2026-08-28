package com.example.kidhackbackend.global.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/**
 * presigned 업로드 URL 발급 요청. 파일 본문은 오지 않고 메타데이터만 온다.
 */
@Schema(description = "presigned 업로드 URL 발급 요청")
@Getter
public class PresignRequest {

    @Schema(description = "사용자가 고른 원본 파일명(저장 파일명은 서버가 다시 생성)", example = "photo.jpg")
    @NotBlank
    private String filename;

    @Schema(description = "업로드할 파일의 MIME 타입. 비우면 application/octet-stream. PUT 시 같은 값을 헤더로 보내야 함",
            example = "image/jpeg")
    private String contentType;
}
