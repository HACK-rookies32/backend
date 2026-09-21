package com.example.kidhackbackend.global.config;

/**
 * 단일 jar 를 관리자 웹 / 모바일 두 이미지로 나눠 배포하기 위한 실행 모드.
 *
 * <p>코드와 빈 그래프(서로 참조하는 서비스·엔티티)는 그대로 두고, 노출할 HTTP 엔드포인트만
 * 실행 시점에 걸러 낸다. 값은 {@code APP_MODE} 환경변수로 주입한다(relaxed binding: APP_MODE
 * → app.mode). 분할 기준은 요청 경로에 {@code /api/admin} 접두사가 있는지 하나뿐이라, 두
 * 이미지의 엔드포인트 집합은 배타적으로 나뉘어 절대 겹치지 않는다.
 *
 * @see AppModeWebConfig
 */
public enum AppMode {

    /** 관리자 웹 이미지: {@code /api/admin/**} 핸들러만 등록한다. */
    ADMIN,

    /** 모바일 이미지: {@code /api/admin/**} 을 제외한 나머지 핸들러만 등록한다. */
    MOBILE,

    /** 분리하지 않고 전부 등록한다. 로컬 개발·CI 테스트 기본값. */
    ALL;

    /**
     * {@code APP_MODE} 문자열을 모드로 변환한다. 비었거나 모르는 값이면 {@link #ALL}.
     * 오타로 조용히 절반이 사라지는 것보다, 못 알아들으면 전부 노출하는 편이 안전하다.
     */
    public static AppMode from(String raw) {
        if (raw == null || raw.isBlank()) {
            return ALL;
        }
        return switch (raw.trim().toLowerCase()) {
            case "admin" -> ADMIN;
            case "mobile" -> MOBILE;
            default -> ALL;
        };
    }
}
