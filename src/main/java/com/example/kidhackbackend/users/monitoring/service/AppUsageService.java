package com.example.kidhackbackend.users.monitoring.service;

import com.example.kidhackbackend.global.util.ClientTime;
import com.example.kidhackbackend.global.notification.entity.NotificationType;
import com.example.kidhackbackend.global.notification.service.NotificationPort;
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
    private final NotificationPort notificationPort;

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
                .map(u -> new AppUsage(request.childId(), u.appName(), u.packageName(),
                        u.durationSec(), ClientTime.toKst(u.recordedAt())))
                .toList();
        repository.saveAll(entities);
        return entities.size();
    }

    /**
     * 자녀 앱에 「사용량을 지금 올려라」를 보낸다.
     *
     * <p>자녀 앱은 1시간 주기로만 올린다. 부모가 사용 리포트를 새로고침해도 서버에 있는
     * 값(자녀가 마지막에 올린 값)만 다시 읽으므로, 방금까지의 사용시간을 보려면 자녀
     * 기기를 깨워야 한다.
     *
     * <p>푸시만 보내고 끝난다 — 실제 값은 자녀 앱이 곧이어 {@code POST /api/monitoring/usage}
     * 로 올려 완성된다. 그래서 컨트롤러가 202(Accepted)로 응답한다. 자녀 기기가 꺼져
     * 있거나 네트워크가 없으면 요청은 닿지 않고 마지막 값이 그대로 남는다.
     */
    // [VULN-MON-1] 이 경로도 childId 소유권을 검증하지 않는다 → 남의 자녀 기기에 보고 트리거 가능(IDOR).
    public void requestReport(Long childId) {
        notificationPort.publish(childId, NotificationType.USAGE_REPORT_REQUESTED,
                "부모가 사용량 확인을 요청했습니다.");
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

        // APP 집계는 패키지명으로 묶고 화면에는 앱 이름을 내보낸다. 부모 화면이 이 값을
        // 그대로 쓰므로 "com.android.camera" 가 보이면 안 된다.
        Map<String, String> labels = (groupBy == GroupBy.APP) ? appLabels(usages) : Map.of();

        List<UsageAggregateResponse.Bucket> items = summed.entrySet().stream()
                .map(e -> new UsageAggregateResponse.Bucket(
                        labels.getOrDefault(e.getKey(), e.getKey()), e.getValue()))
                .sorted(Comparator.comparing(UsageAggregateResponse.Bucket::key))
                .toList();

        return new UsageAggregateResponse(childId, groupBy, items);
    }

    /** groupBy 기준으로 집계 키를 만든다. */
    private Function<AppUsage, String> keyOf(GroupBy groupBy) {
        return switch (groupBy) {
            case HOUR -> u -> String.format(Locale.ROOT, "%02d", u.getRecordedAt().getHour()); // 00~23
            case APP -> AppUsage::groupingKey;
            case DAY -> u -> u.getRecordedAt().toLocalDate().toString(); // yyyy-MM-dd
        };
    }

    /**
     * 집계 키 → 화면에 보일 앱 이름.
     *
     * <p>한 패키지가 여러 이름으로 들어와 있을 수 있다(기기 언어가 다른 두 기기, 앱 이름
     * 변경). 그중 **가장 최근에 기록된 이름**을 쓴다 — 지금 자녀 기기에 보이는 이름일
     * 가능성이 가장 높다.
     */
    private Map<String, String> appLabels(List<AppUsage> usages) {
        return usages.stream()
                // 조회 순서는 보장되지 않는다. 시각순으로 세운 뒤 뒤엣것이 이기게 한다.
                .sorted(Comparator.comparing(AppUsage::getRecordedAt))
                .collect(Collectors.toMap(
                        AppUsage::groupingKey,
                        AppUsage::getAppName,
                        (older, newer) -> newer));
    }
}
