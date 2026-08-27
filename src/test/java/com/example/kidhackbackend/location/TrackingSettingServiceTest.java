package com.example.kidhackbackend.location;

import com.example.kidhackbackend.users.location.repository.TrackingSettingRepository;
import com.example.kidhackbackend.users.location.service.TrackingSettingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실시간 추적 토글 조회(GET) 슬라이스 테스트.
 *
 * <p>설정 레코드가 없을 때의 기본값(false)과, 토글 후 조회 결과가 일치하는지 확인한다.
 */
@DataJpaTest
@Import(TrackingSettingService.class)
class TrackingSettingServiceTest {

    @Autowired
    TrackingSettingService trackingSettingService;

    @Autowired
    TrackingSettingRepository trackingSettingRepository;

    @Test
    void 설정이_없으면_기본값_false를_반환한다() {
        assertThat(trackingSettingService.isRealtimeEnabled(999L)).isFalse();
        assertThat(trackingSettingRepository.findByChildId(999L)).isEmpty();
    }

    @Test
    void 토글_ON_후_조회하면_true다() {
        trackingSettingService.setRealtime(1L, true);

        assertThat(trackingSettingService.isRealtimeEnabled(1L)).isTrue();
    }

    @Test
    void 토글_ON_후_OFF하면_조회값도_false로_돌아온다() {
        trackingSettingService.setRealtime(1L, true);
        trackingSettingService.setRealtime(1L, false);

        assertThat(trackingSettingService.isRealtimeEnabled(1L)).isFalse();
        // 설정은 자녀당 1개(upsert)라 레코드가 늘지 않는다
        assertThat(trackingSettingRepository.count()).isEqualTo(1);
    }

    @Test
    void 자녀별로_토글_상태가_독립적이다() {
        trackingSettingService.setRealtime(1L, true);
        trackingSettingService.setRealtime(2L, false);

        assertThat(trackingSettingService.isRealtimeEnabled(1L)).isTrue();
        assertThat(trackingSettingService.isRealtimeEnabled(2L)).isFalse();
    }
}
