package com.example.kidhackbackend.global.storage.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/**
 * presigned 업로드 URL 발급 요청.
 *
 * <p>파일 본문은 오지 않는다. 서버는 이 메타데이터만 보고 서명을 만든다.
 */
@Getter
public class PresignRequest {

    /** 사용자가 고른 원본 파일명. 저장 파일명은 서버가 다시 만든다. */
    @NotBlank
    private String filename;

    /**
     * 업로드할 파일의 MIME 타입.
     *
     * <p>이 값이 서명에 포함되므로 클라이언트는 PUT 할 때 같은 값을 헤더로 보내야 한다.
     * 비어 있으면 서버가 {@code application/octet-stream} 으로 채운다.
     */
    private String contentType;
}
