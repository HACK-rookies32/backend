package com.example.kidhackbackend.location.service;

import com.example.kidhackbackend.location.dto.TrackRequest;
import com.example.kidhackbackend.location.entity.TrackSegment;
import com.example.kidhackbackend.location.repository.TrackSegmentRepository;
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
    private final int segmentMinutes;

    public TrackingService(TrackSegmentRepository repository,
                           GeofenceService geofenceService,
                           @Value("${kidhack.location.segment-minutes:5}") int segmentMinutes) {
        this.repository = repository;
        this.geofenceService = geofenceService;
        this.segmentMinutes = segmentMinutes;
    }

    /**
     * 자녀 앱이 업로드한 현재 좌표를 5분 세그먼트에 반영하고, 지오펜스/도착 판정을 트리거한다.
     *
     * @return 갱신되거나 새로 생성된 세그먼트
     */
    @Transactional
    public TrackSegment record(TrackRequest request) {
        double lat = parseCoordinate(request.latitude(), "위도", -90.0, 90.0);
        double lng = parseCoordinate(request.longitude(), "경도", -180.0, 180.0);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowStart = floorToWindow(now);
        LocalDateTime windowEnd = windowStart.plusMinutes(segmentMinutes);

        TrackSegment segment = repository.findByChildIdAndSegmentStart(request.childId(), windowStart)
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

        // No.1(지오펜스 진입/이탈)·No.3(도착) 판정을 새 좌표 기준으로 트리거.
        geofenceService.evaluate(request.childId(), lat, lng, now);

        return segment;
    }

    /** 자녀의 동선 조회(최신순). */
    @Transactional(readOnly = true)
    public List<TrackSegment> findTrack(Long childId) {
        return repository.findByChildIdOrderBySegmentStartDesc(childId);
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
