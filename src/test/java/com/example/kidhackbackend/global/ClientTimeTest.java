package com.example.kidhackbackend.global;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.kidhackbackend.global.util.ClientTime;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

/** #QA-13 — 앱이 UTC(Z)로 보낸 시각을 KST 로 변환하는지. */
class ClientTimeTest {

    @Test
    void UTC_Z_는_9시간_더해_KST로_바뀐다() {
        // 앱이 보내는 형태: Instant.now().toString() = ...Z
        assertThat(ClientTime.toKst("2026-09-02T01:34:00Z"))
                .isEqualTo(LocalDateTime.of(2026, 9, 2, 10, 34, 0)); // 01:34 UTC → 10:34 KST
    }

    @Test
    void 밀리초_포함_UTC도_변환된다() {
        assertThat(ClientTime.toKst("2026-09-02T01:00:00.123Z"))
                .isEqualTo(LocalDateTime.of(2026, 9, 2, 10, 0, 0, 123_000_000));
    }

    @Test
    void 명시적_오프셋도_KST로_맞춘다() {
        // +09:00 이면 이미 KST 순간 → 그대로 10:00
        assertThat(ClientTime.toKst("2026-09-02T10:00:00+09:00"))
                .isEqualTo(LocalDateTime.of(2026, 9, 2, 10, 0, 0));
    }

    @Test
    void 오프셋_없으면_이미_KST_벽시계로_보고_그대로_쓴다() {
        // 하위호환: 예전 무-오프셋 호출은 변환 없이 그대로
        assertThat(ClientTime.toKst("2026-09-02T10:00:00"))
                .isEqualTo(LocalDateTime.of(2026, 9, 2, 10, 0, 0));
    }

    @Test
    void 시각이_아니면_IllegalArgument로_400_을_유도한다() {
        assertThatThrownBy(() -> ClientTime.toKst("어제쯤"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
