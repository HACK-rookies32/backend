package com.example.kidhackbackend.users.child.dto;

import com.example.kidhackbackend.users.filtering.dto.BlockRuleResponse;
import com.example.kidhackbackend.global.notification.dto.NotificationResponse;
import com.example.kidhackbackend.users.location.dto.PlaceResponse;
import com.example.kidhackbackend.users.location.dto.TrackSegmentResponse;

import java.util.List;

/**
 * 자녀 관리 페이지 통합 조회 응답 — 역할표 No.18.
 *
 * <p>위치·알림·차단 도메인의 기존 응답 DTO를 조합한다. (새 데이터 저장 없음)
 */
public record ChildDashboardResponse(
        Long childId,
        /** 현재 위치(동선 최신 1건). 기록이 없으면 null. */
        TrackSegmentResponse currentLocation,
        /** 실시간 추적 on/off. */
        boolean realtimeEnabled,
        /** 자녀가 수신자인 최근 알림 3건(최신순). */
        List<NotificationResponse> childNotifications,
        /** 요청한 부모가 수신자인 최근 알림 3건(최신순) — 결제 승인/취소, 트래킹 불가 등. */
        List<NotificationResponse> parentNotifications,
        /** 유해 차단 목록(블랙리스트). */
        List<BlockRuleResponse> blockRules,
        /** 등록 공간(지오펜스/목적지). */
        List<PlaceResponse> places
) {
}
