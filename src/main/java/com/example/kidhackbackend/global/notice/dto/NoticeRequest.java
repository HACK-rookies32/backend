package com.example.kidhackbackend.global.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.util.List;

/**
 * 공지 작성·수정 요청. 관리자 웹의 작성/수정 모달이 같은 본문을 쓴다.
 *
 * <p>수정(PATCH)에서 {@code null} 은 "그대로 두기" 다. 첨부·링크는 <b>빈 배열</b>을
 * 보내면 비운다 — null 과 구분해야 전부 지우는 조작이 가능하다.
 */
@Schema(description = "공지 작성/수정 요청 — 수정 시 null 은 미변경, 빈 배열은 비우기")
@Getter
public class NoticeRequest {

    @Schema(description = "제목", example = "8월 정기 점검 안내")
    private String title;

    @Schema(description = "본문")
    private String content;

    @Schema(description = "첨부파일 URL 목록 (업로드는 /api/files/{category}/presign 이 담당)")
    private List<String> attachments;

    @Schema(description = "관련 링크 목록")
    private List<String> links;
}
