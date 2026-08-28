package com.example.kidhackbackend.users.location.service;

import com.example.kidhackbackend.users.location.dto.TrackRequest;
import com.example.kidhackbackend.users.location.entity.TrackSegment;
import com.example.kidhackbackend.users.location.repository.TrackSegmentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 실시간 동선 트래킹 저장 로직 (역할표 No.2).
 *
 * <p>5분 단위 세그먼트로 좌표를 적재한다. 같은 5분 창 안에서 들어온 좌표는 기존 세그먼트를
 * 최신 좌표로 갱신하고, 새 창이면 새 세그먼트를 생성한다. 팀 공통 기반 로직이며
 * No.1(지오펜스)·No.3(도착) 판정의 입력이 된다.
 */
@Service
public class TrackingService {

    private final TrackSegmentRepository repository;
    private final GeofenceService geofenceService;
    private final TrackingSettingService trackingSettingService;
    private final int segmentMinutes;

    public TrackingService(TrackSegmentRepository repository,
                           GeofenceService geofenceService,
                           TrackingSettingService trackingSettingService,
                           @Value("${kidhack.location.segment-minutes:5}") int segmentMinutes) {
        this.repository = repository;
        this.geofenceService = geofenceService;
        this.trackingSettingService = trackingSettingService;
        this.segmentMinutes = segmentMinutes;
    }

    /**
     * 자녀 앱이 업로드한 현재 좌표를 저장하고, 지오펜스/도착 판정을 트리거한다.
     *
     * <p>자녀의 실시간 추적 토글에 따라 저장 방식이 달라진다.
     * <ul>
     *   <li>OFF(기본): 5분 단위 세그먼트로 묶어 저장(같은 창이면 최신 좌표로 갱신)</li>
     *   <li>ON: 업로드된 좌표를 매번 실시간 포인트로 저장(5분 묶음 없음)</li>
     * </ul>
     *
     * @return 갱신되거나 새로 생성된 세그먼트
     */
    @Transactional
    public TrackSegment record(TrackRequest request) {
        double lat = parseCoordinate(request.latitude(), "위도", -90.0, 90.0);
        double lng = parseCoordinate(request.longitude(), "경도", -180.0, 180.0);

        LocalDateTime now = LocalDateTime.now();

        TrackSegment segment = trackingSettingService.isRealtimeEnabled(request.childId())
                ? recordRealtime(request, now)
                : recordSegment(request, now);

        // No.1(지오펜스 진입/이탈)·No.3(도착) 판정을 새 좌표 기준으로 트리거.
        geofenceService.evaluate(request.childId(), lat, lng, now);

        return segment;
    }

    /** 실시간 ON: 좌표를 매번 포인트 세그먼트(시작=종료=now)로 새로 저장. */
    // [VULN-LOC-4] 업로드 횟수 제한(rate limit)이 없어, 실시간 ON 상태에서 /track을 대량 호출하면
    //              레코드가 무제한 생성되어 저장소 고갈/성능 저하(DoS)로 이어질 수 있다.
    //              (VULN-LOC-1 IDOR로 남의 자녀를 강제 ON 시킨 뒤 flood하면 피해가 커진다.)
    private TrackSegment recordRealtime(TrackRequest request, LocalDateTime now) {
        return repository.save(new TrackSegment(
                request.childId(),
                request.latitude(),
                request.longitude(),
                now,
                now,
                now));
    }

    /** 실시간 OFF(기본): 5분 창에 묶어 저장(같은 창이면 최신 좌표로 갱신). */
    private TrackSegment recordSegment(TrackRequest request, LocalDateTime now) {
        LocalDateTime windowStart = floorToWindow(now);
        LocalDateTime windowEnd = windowStart.plusMinutes(segmentMinutes);

        return repository.findByChildIdAndSegmentStart(request.childId(), windowStart)
                .map(existing -> {
                    existing.updatePosition(request.latitude(), request.longitude(), now);
                    return existing;
                })
                .orElseGet(() -> repository.save(new TrackSegment(
                        request.childId(),
                        request.latitude(),
                        request.longitude(),
                        windowStart,
                        windowEnd,
                        now
                )));
    }

    /** 자녀의 동선 조회(최신순) — 대시보드 등 "최근 1건" 용도. */
    @Transactional(readOnly = true)
    public List<TrackSegment> findTrack(Long childId) {
        return repository.findByChildIdOrderBySegmentStartDesc(childId);
    }

    /**
     * 동선 그리기용 조회 — 시간 오름차순.
     *
     * @param from 시작(포함). null 이면 하한 없음
     * @param to   종료(제외). null 이면 상한 없음
     */
    @Transactional(readOnly = true)
    public List<TrackSegment> getTrack(Long childId, LocalDateTime from, LocalDateTime to) {
        if (from != null && to != null) {
            return repository.findTrackInRange(childId, from, to);
        }
        return repository.findByChildIdOrderBySegmentStartAsc(childId);
    }

    /** 현재 시각을 5분 경계로 내림 정렬한다. (예: 12:07 → 12:05) */
    private LocalDateTime floorToWindow(LocalDateTime now) {
        int flooredMinute = (now.getMinute() / segmentMinutes) * segmentMinutes;
        return now.withMinute(flooredMinute).withSecond(0).withNano(0);
    }

    /** 좌표는 스펙상 문자열로 저장하지만, 파싱 가능한 값인지와 범위는 검증한다. */
    private double parseCoordinate(String value, String field, double min, double max) {
        double parsed;
        try {
            parsed = Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(field + "가 올바른 숫자 형식이 아닙니다: " + value);
        }
        if (parsed < min || parsed > max) {
            throw new IllegalArgumentException(field + " 범위는 " + min + "~" + max + " 입니다: " + parsed);
        }
        return parsed;
    }
}
