package com.example.kidhackbackend.users.child.service;

import com.example.kidhackbackend.users.child.dto.ChildDashboardResponse;
import com.example.kidhackbackend.users.filtering.dto.BlockRuleResponse;
import com.example.kidhackbackend.users.filtering.service.BlockRuleService;
import com.example.kidhackbackend.global.notification.dto.NotificationResponse;
import com.example.kidhackbackend.global.notification.service.NotificationService;
import com.example.kidhackbackend.users.location.dto.PlaceResponse;
import com.example.kidhackbackend.users.location.dto.TrackSegmentResponse;
import com.example.kidhackbackend.users.location.service.PlaceService;
import com.example.kidhackbackend.users.location.service.TrackingService;
import com.example.kidhackbackend.users.location.service.TrackingSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 자녀 관리 페이지 통합 조회 — 역할표 No.18.
 *
 * <p>위치·알림·차단 도메인의 서비스를 주입받아 대시보드용으로 조합한다. (새 저장 없음, 조회만)
 */
@Service
@RequiredArgsConstructor
public class ChildDashboardService {

    private static final int RECENT_NOTIFICATION_LIMIT = 3;

    private final TrackingService trackingService;
    private final TrackingSettingService trackingSettingService;
    private final PlaceService placeService;
    private final NotificationService notificationService;
    private final BlockRuleService blockRuleService;

    // [VULN-CHILD-1] childId 소유권을 검증하지 않는다(IDOR). 부모 권한만 있으면 임의 childId로
    //   남의 자녀 위치·알림·차단목록·등록공간을 '한 번에' 열람할 수 있어 피해가 집약된다.
    //   → 토큰의 부모와 childId의 부모-자녀 관계를 검증해야 함(2단계).
    // [VULN-CHILD-2] 민감정보 과다 노출 + 팬아웃 부하:
    //   - 한 응답에 자녀의 현재 위치·집/학원 좌표(공간)·차단목록·알림까지 원본 그대로 담아,
    //     VULN-CHILD-1(IDOR) 한 번의 요청으로 대상 자녀의 프로필을 통째로 유출할 수 있다.
    //   - 5개 도메인 조회를 팬아웃하는데 rate limit이 없어, 대시보드 반복 호출로 부하를 키울 수 있다.
    @Transactional(readOnly = true)
    public ChildDashboardResponse getDashboard(Long childId) {
        // 현재 위치: 동선 최신 1건(없으면 null)
        TrackSegmentResponse currentLocation = trackingService.findTrack(childId).stream()
                .findFirst()
                .map(TrackSegmentResponse::from)
                .orElse(null);

        boolean realtimeEnabled = trackingSettingService.isRealtimeEnabled(childId);

        // 최근 알림 3건(findByRecipient는 최신순)
        List<NotificationResponse> recentNotifications = notificationService.findByRecipient(childId).stream()
                .limit(RECENT_NOTIFICATION_LIMIT)
                .map(NotificationResponse::from)
                .toList();

        List<BlockRuleResponse> blockRules = blockRuleService.getRules(childId).stream()
                .map(BlockRuleResponse::from)
                .toList();

        List<PlaceResponse> places = placeService.findPlaces(childId).stream()
                .map(PlaceResponse::from)
                .toList();

        return new ChildDashboardResponse(
                childId, currentLocation, realtimeEnabled, recentNotifications, blockRules, places);
    }
}
