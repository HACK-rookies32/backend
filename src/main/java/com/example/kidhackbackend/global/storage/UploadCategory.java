package com.example.kidhackbackend.global.storage;

import java.util.Arrays;
import java.util.Optional;

/**
 * 첨부파일 분류. 저장 경로와 공개 URL 의 한 조각이 된다.
 *
 * <p>다운로드 컨트롤러가 경로 변수를 그대로 파일 시스템 경로나 S3 키에 넣지
 * 않고 반드시 이 enum 을 거치게 하려고 둔다. 문자열을 그대로 썼다면
 * {@code /api/files/../../etc/passwd} 같은 요청이 경로를 빠져나갈 수 있다.
 */
public enum UploadCategory {

    COMMUNITY("community"),
    INQUIRY("inquiry"),
    /** 관리자 공지 첨부. 관리자 웹이 /api/files/notice/presign → /complete 로 올린 뒤 URL 을 공지에 담는다. */
    NOTICE("notice"),
    /** 프로필 이미지. 회원가입·프로필 수정에서 profileImageUrl 로 저장된다. */
    PROFILE("profile"),
    /** 자녀 화면 캡처. 1시간 주기 자동 또는 부모 요청분. */
    SCREENSHOT("screenshot");

    private final String segment;

    UploadCategory(String segment) {
        this.segment = segment;
    }

    public String segment() {
        return segment;
    }

    /** 알 수 없는 값이면 비어 있는 Optional. 컨트롤러가 404 로 처리한다. */
    public static Optional<UploadCategory> from(String segment) {
        return Arrays.stream(values())
                .filter(c -> c.segment.equals(segment))
                .findFirst();
    }
}
