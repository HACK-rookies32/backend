package com.example.kidhackbackend.global.storage.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/**
 * presigned 업로드 완료 통보.
 *
 * <p>presigned PUT 은 WAS 를 거치지 않으므로 서버는 업로드가 끝났는지 알 수 없다.
 * 클라이언트가 이 API 로 알려 오면 서버가 S3 에 실제로 있는지 확인한 뒤 공개 경로를
 * 확정해 준다.
 */
@Getter
public class UploadCompleteRequest {

    /** 발급 API 가 돌려준 저장 파일명. 원본 파일명이 아니다. */
    @NotBlank
    private String filename;
}
