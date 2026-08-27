package com.example.kidhackbackend.global.notice.dto;

import lombok.Getter;

import java.util.List;

/**
 * 공지 작성·수정 요청. 관리자 웹의 작성/수정 모달이 같은 본문을 쓴다.
 *
 * <p>수정(PATCH)에서 {@code null} 은 "그대로 두기" 다. 첨부·링크는 <b>빈 배열</b>을
 * 보내면 비운다 — null 과 구분해야 전부 지우는 조작이 가능하다.
 */
@Getter
public class NoticeRequest {

    private String title;
    private String content;

    /** 첨부파일 URL 목록. 업로드는 {@code /api/files/{category}/presign} 이 따로 담당한다. */
    private List<String> attachments;

    /** 관련 링크 목록. */
    private List<String> links;
}
