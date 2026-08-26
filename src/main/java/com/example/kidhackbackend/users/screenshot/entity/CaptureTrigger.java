package com.example.kidhackbackend.users.screenshot.entity;

/**
 * 캡처가 일어난 계기.
 *
 * <p>{@code SCHEDULED} 는 자녀 앱의 1시간 주기 자동 촬영, {@code MANUAL} 은 부모가
 * {@code POST /screenshots/request} 로 요청해 이뤄진 추가 촬영이다.
 */
public enum CaptureTrigger {
    SCHEDULED,
    MANUAL
}
