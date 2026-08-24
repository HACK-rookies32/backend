package com.example.kidhackbackend.location.service;

/**
 * 위치 도메인 → 알림 도메인 연동 지점(느슨한 결합).
 *
 * <p>알림 도메인(역할표 No.4~6)은 다른 담당자가 진행 중이라, 위치 도메인은 이 인터페이스에만
 * 의존한다. 알림 서비스가 완성되면 실제 구현체로 교체한다.
 */
public interface AlertPort {

    /**
     * @param childId 대상 자녀
     * @param type    이벤트 유형(ENTER/EXIT/ARRIVED 등)
     * @param message 부모에게 전달할 메시지
     */
    void sendLocationAlert(Long childId, String type, String message);
}
