package com.example.kidhackbackend.global.storage.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/**
 * presigned 업로드 완료 통보. 서버가 S3 에 실제로 있는지 확인한 뒤 공개 경로를 확정한다.
 */
@Schema(description = "presigned 업로드 완료 통보")
@Getter
public class UploadCompleteRequest {

    @Schema(description = "발급 API 가 돌려준 저장 파일명(원본 파일명 아님)")
    @NotBlank
    private String filename;
}
