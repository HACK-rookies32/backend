package com.example.kidhackbackend.global.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * 클라이언트가 보낸 시각 문자열을 <b>KST(Asia/Seoul) 벽시계 {@link LocalDateTime}</b> 로 바꾼다(#QA-13).
 *
 * <p><b>왜 필요한가.</b> 모바일 앱은 사용량·스크린샷 시각을 {@code Instant.now().toString()} 으로 만들어
 * {@code 2026-09-02T01:34:00Z} 처럼 <b>UTC</b> 로 보낸다. 그런데 수신 타입이 {@code LocalDateTime} 이면
 * 끝의 {@code Z}(=UTC 표시)가 버려지고 {@code 01:34} 가 그대로 저장돼, 화면에서 KST 보다 9시간 밀린다
 * (10시 사용 → 01시 표기). 서버가 스스로 만드는 시각({@code createdAt} 등)은 운영 컨테이너
 * TZ 가 Asia/Seoul 이라 이미 KST 다 — 그래서 결제·거래 시각은 정상이고 <b>클라이언트 전송 시각만</b> 어긋난다.
 *
 * <p><b>동작.</b>
 * <ul>
 *   <li>{@code Z}·오프셋이 있으면(예: {@code ...Z}, {@code ...+09:00}) 그 순간을 Asia/Seoul 로 변환한다.</li>
 *   <li>오프셋이 없으면(예: {@code 2026-09-02T10:00:00}) 이미 KST 벽시계로 보고 그대로 쓴다 —
 *       기존 무-오프셋 호출(Postman·스웨거 예시)과의 하위호환을 위해서다.</li>
 * </ul>
 * 저장 컬럼은 시간대 없는 {@code TIMESTAMP} 이고 서버 규약이 "KST 벽시계" 이므로, 여기서 KST 로 맞춘 뒤
 * 넘긴다. <b>과거에 UTC 로 쌓인 데이터는 건드리지 않는다</b> — 이 변환은 신규 저장분에만 적용된다.
 */
public final class ClientTime {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private ClientTime() {
    }

    /**
     * 오프셋이 있으면 KST 로 변환, 없으면 그대로 KST 로 해석한다.
     *
     * @param raw ISO-8601 시각 문자열
     * @return KST 벽시계 {@link LocalDateTime}
     * @throws DateTimeParseException 파싱 불가한 형식
     */
    public static LocalDateTime toKst(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("시각이 비어 있습니다.");
        }
        String s = raw.trim();
        try {
            // 오프셋/Z 가 있으면 여기서 파싱된다 → 그 순간을 KST 로 옮긴다.
            return OffsetDateTime.parse(s).atZoneSameInstant(SEOUL).toLocalDateTime();
        } catch (DateTimeParseException withOffsetFailed) {
            try {
                // 오프셋이 없다 — 이미 KST 벽시계로 보고 그대로 쓴다(하위호환).
                return LocalDateTime.parse(s);
            } catch (DateTimeParseException notADateTime) {
                // 둘 다 실패 = 시각 형식이 아니다. 400 으로 나가게 IllegalArgumentException 으로 바꾼다
                // (예전 @DateTimeFormat/LocalDateTime 바인딩이 내던 400 과 동일한 취급).
                throw new IllegalArgumentException("시각 형식이 올바르지 않습니다: " + raw);
            }
        }
    }

    /** {@link Instant}(항상 UTC 순간)를 KST 벽시계로. */
    public static LocalDateTime toKst(Instant instant) {
        return instant.atZone(SEOUL).toLocalDateTime();
    }
}
