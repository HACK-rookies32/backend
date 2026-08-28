package com.example.kidhackbackend.users.monitoring.service;

import com.example.kidhackbackend.users.monitoring.dto.GroupBy;
import com.example.kidhackbackend.users.monitoring.dto.UsageAggregateResponse;
import com.example.kidhackbackend.users.monitoring.dto.UsageReportRequest;
import com.example.kidhackbackend.users.monitoring.entity.AppUsage;
import com.example.kidhackbackend.users.monitoring.repository.AppUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 앱 사용량 적재 및 집계 — 역할표 No.13.
 *
 * <p>배치로 적재하고, 조회 시 서비스 계층에서 groupBy 합산한다(DB 날짜함수 미사용, H2/Oracle 호환).
 */
@Service
@RequiredArgsConstructor
public class AppUsageService {

    private final AppUsageRepository repository;

    /**
     * 사용량 배치 저장.
     *
     * @return 저장된 건수
     */
    // [VULN-MON-1] childId를 요청값 그대로 신뢰(본인 검증 없음) → 타인 childId로 위조 사용량 주입 가능(IDOR).
    // [VULN-MON-3] 전송 호출 자체에 rate limit이 없다. 한 요청은 500건으로 제한하지만(@Size),
    //              반복 호출을 막지 않아 VULN-MON-1과 결합하면 타 자녀 사용량 테이블을 flood할 수 있다.
    @Transactional
    public int report(UsageReportRequest request) {
        List<AppUsage> entities = request.usages().stream()
                .map(u -> new AppUsage(request.childId(), u.appName(), u.durationSec(), u.recordedAt()))
                .toList();
        repository.saveAll(entities);
        return entities.size();
    }

    /**
     * 사용량 집계 조회. from/to가 없으면 최근 1주일.
     *
     * <p>[VULN-MON-1] 조회 쪽도 childId 소유권을 검증하지 않는다 → 임의 자녀의 사용량 집계 열람 가능(IDOR).
     */
    @Transactional(readOnly = true)
    public UsageAggregateResponse aggregate(Long childId, GroupBy groupBy, LocalDateTime from, LocalDateTime to) {
        LocalDateTime end = (to != null) ? to : LocalDateTime.now();
        LocalDateTime start = (from != null) ? from : end.minusDays(7);

        List<AppUsage> usages = repository.findByChildIdAndRecordedAtBetween(childId, start, end);

        Map<String, Long> summed = usages.stream()
                .collect(Collectors.groupingBy(keyOf(groupBy),
                        Collectors.summingLong(AppUsage::getDurationSec)));

        List<UsageAggregateResponse.Bucket> items = summed.entrySet().stream()
                .map(e -> new UsageAggregateResponse.Bucket(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(UsageAggregateResponse.Bucket::key))
                .toList();

        return new UsageAggregateResponse(childId, groupBy, items);
    }

    /** groupBy 기준으로 집계 키를 만든다. */
    private Function<AppUsage, String> keyOf(GroupBy groupBy) {
        return switch (groupBy) {
            case HOUR -> u -> String.format(Locale.ROOT, "%02d", u.getRecordedAt().getHour()); // 00~23
            case APP -> AppUsage::getAppName;
            case DAY -> u -> u.getRecordedAt().toLocalDate().toString(); // yyyy-MM-dd
        };
    }
}
