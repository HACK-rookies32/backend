package com.example.kidhackbackend.admin.audit.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 관리자 활동이 건드린 대상 리소스의 종류.
 *
 * <p>명세서에 정의된 4종(MEMBER·WALLET·POST·PAYMENT) 에 더해, admin 패키지의 나머지
 * 변경(비-GET) 엔드포인트가 다루는 리소스까지 포함한다.
 */
@Getter
@RequiredArgsConstructor
public enum AuditTargetType {

    MEMBER("회원"),
    WALLET("지갑"),
    POST("게시글"),
    PAYMENT("매장 결제"),
    INQUIRY("1:1 문의"),
    NOTICE("공지사항"),
    NOTIFICATION("알림"),
    HARMFUL_KEYWORD("유해 키워드");

    private final String label;
}
