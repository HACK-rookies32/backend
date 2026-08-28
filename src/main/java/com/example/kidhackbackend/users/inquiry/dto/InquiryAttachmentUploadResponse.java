package com.example.kidhackbackend.users.inquiry.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 1:1 문의 첨부파일 업로드 결과.
 *
 * <p>{@code url} 은 {@code /api/files/inquiry/{filename}} 형태의 공개 경로다. 이 값을 문의 작성/수정 요청의
 * {@code attachments} 배열에 담는다. 조회 시 이 경로가 presigned S3 GET 으로 리다이렉트된다.
 */
@Schema(description = "1:1 문의 첨부파일 업로드 결과")
public record InquiryAttachmentUploadResponse(
        @Schema(description = "첨부 공개 경로(문의 attachments 에 넣을 값)",
                example = "/api/files/inquiry/9f8e-...jpg") String url) {
}
