package com.example.kidhackbackend.global.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * presigned 업로드 URL 발급 결과.
 *
 * <p>클라이언트는 이 순서로 쓴다.
 * <ol>
 *   <li>{@link #uploadUrl} 로 {@code PUT} — 헤더에 {@link #contentType} 을 그대로 넣는다.</li>
 *   <li>{@link #filename} 을 완료 API 에 보내 서버가 존재를 확인하게 한다.</li>
 *   <li>완료 API 가 돌려준 경로를 게시글/문의 작성 요청에 담는다.</li>
 * </ol>
 *
 * <p>{@link #path} 를 미리 알려주는 것은 미리보기 등에 쓰라는 편의일 뿐이다. 업로드가
 * 끝나기 전에는 그 경로로 받을 수 없으므로 DB 에 넣을 값은 완료 API 를 거쳐야 한다.
 */
@Getter
@AllArgsConstructor
public class PresignResponse {

    /** 브라우저가 PUT 할 S3 주소. 서명이 들어 있으므로 노출·재사용에 주의한다. */
    private String uploadUrl;

    /** 업로드가 끝난 뒤 이 파일을 가리키게 될 공개 경로. */
    private String path;

    /** 서버가 정한 저장 파일명. 완료 API 에 그대로 돌려줘야 한다. */
    private String filename;

    /** PUT 요청에 그대로 실어야 하는 Content-Type. 다르면 S3 가 403 을 준다. */
    private String contentType;

    /** 서명 유효 기간(초). 지나면 다시 발급받아야 한다. */
    private long expiresInSeconds;
}
