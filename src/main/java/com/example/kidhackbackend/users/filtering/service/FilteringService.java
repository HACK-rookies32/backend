package com.example.kidhackbackend.users.filtering.service;

import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.service.NotificationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 유해 사이트/앱 접속 감지 — 역할표 No.5.
 *
 * <p>자녀가 보고한 접속 대상을 부모 블랙리스트(No.6)로 매칭해, 차단 대상이면 알림을 발행한다.
 */
@Service
@RequiredArgsConstructor
public class FilteringService {

    private final BlockRuleService blockRuleService;
    private final NotificationPort notificationPort;

    /**
     * 자녀의 접속 대상을 검사하고, 차단 대상이면 알림을 생성한다.
     *
     * @return 차단 여부
     */
    // [VULN-FILT-2] target(URL/앱 텍스트)을 정규화·검증 없이 그대로 매칭에 사용한다.
    //   - 자녀 앱이 보고 값을 조작하면(예: 대소문자/유니코드/공백 삽입) 블랙리스트 매칭을 우회할 수 있다.
    //   - childId도 요청값을 그대로 신뢰(본인 검증 없음)해 타인 childId로 위조 보고가 가능하다(IDOR).
    @Transactional
    public boolean detect(Long childId, String target) {
        boolean blocked = blockRuleService.isBlocked(childId, target);
        if (blocked) {
            notificationPort.publish(childId, NotificationType.HARMFUL_SITE,
                    "유해 사이트/앱 접속이 감지되었습니다: " + target);
        }
        return blocked;
    }
}
