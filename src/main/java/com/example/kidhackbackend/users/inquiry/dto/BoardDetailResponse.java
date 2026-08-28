package com.example.kidhackbackend.users.inquiry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Schema(description = "1:1 문의 상세 (답변 포함)")
@Getter
@AllArgsConstructor
public class BoardDetailResponse {

    @Schema(description = "문의 ID", example = "3")
    private Long id;

    @Schema(description = "작성자 회원 ID", example = "1")
    private Long memberId;

    @Schema(description = "작성자 닉네임", example = "보라")
    private String memberNickname;

    @Schema(description = "제목")
    private String title;

    @Schema(description = "본문")
    private String content;

    @Schema(description = "첨부파일 URL (없으면 null)")
    private String attachmentUrl;

    @Schema(description = "관련 링크 URL (없으면 null)")
    private String linkUrl;

    @Schema(description = "답변 완료 여부", example = "true")
    private boolean answered;

    @Schema(description = "작성 시각")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시각")
    private LocalDateTime updatedAt;

    @Schema(description = "관리자 답변 (없으면 null)")
    private InquiryAnswerResponse answer;
}
