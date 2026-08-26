package com.example.kidhackbackend.location;

import com.example.kidhackbackend.users.location.dto.TrackRequest;
import com.example.kidhackbackend.users.location.entity.TrackSegment;
import com.example.kidhackbackend.users.location.repository.TrackSegmentRepository;
import com.example.kidhackbackend.users.location.service.GeofenceService;
import com.example.kidhackbackend.users.location.service.LogAlertAdapter;
import com.example.kidhackbackend.users.location.service.TrackingService;
import com.example.kidhackbackend.users.location.service.TrackingSettingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({TrackingService.class, GeofenceService.class, LogAlertAdapter.class, TrackingSettingService.class})
class TrackingServiceTest {

    @Autowired
    TrackingService trackingService;

    @Autowired
    TrackingSettingService trackingSettingService;

    @Autowired
    TrackSegmentRepository repository;

    @Test
    void 같은_5분창의_좌표는_기존_세그먼트를_갱신한다() {
        trackingService.record(new TrackRequest(1L, "37.5000", "127.0000"));
        trackingService.record(new TrackRequest(1L, "37.5010", "127.0010"));

        List<TrackSegment> track = trackingService.findTrack(1L);
        assertThat(track).hasSize(1);
        assertThat(track.get(0).getLatitude()).isEqualTo("37.5010");
    }

    @Test
    void 서로_다른_자녀는_별도_세그먼트를_가진다() {
        trackingService.record(new TrackRequest(1L, "37.5", "127.0"));
        trackingService.record(new TrackRequest(2L, "35.1", "129.0"));

        assertThat(trackingService.findTrack(1L)).hasSize(1);
        assertThat(trackingService.findTrack(2L)).hasSize(1);
    }

    @Test
    void 잘못된_좌표는_거부한다() {
        assertThatThrownBy(() -> trackingService.record(new TrackRequest(1L, "abc", "127.0")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> trackingService.record(new TrackRequest(1L, "200", "127.0")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 실시간_추적_ON이면_좌표마다_새_포인트가_저장된다() {
        trackingSettingService.setRealtime(1L, true);

        trackingService.record(new TrackRequest(1L, "37.5000", "127.0000"));
        trackingService.record(new TrackRequest(1L, "37.5010", "127.0010"));
        trackingService.record(new TrackRequest(1L, "37.5020", "127.0020"));

        // 5분 묶음 없이 3건 모두 개별 저장
        assertThat(trackingService.findTrack(1L)).hasSize(3);
    }

    @Test
    void 토글을_다시_끄면_5분_세그먼트로_돌아간다() {
        trackingSettingService.setRealtime(1L, true);
        trackingService.record(new TrackRequest(1L, "37.5", "127.0"));

        trackingSettingService.setRealtime(1L, false);
        trackingService.record(new TrackRequest(1L, "37.6", "127.1"));
        trackingService.record(new TrackRequest(1L, "37.7", "127.2"));

        // 실시간 1건 + 같은 5분창으로 묶인 1건 = 2건
        assertThat(trackingService.findTrack(1L)).hasSize(2);
    }

    @Test
    void 보관기간_경과_세그먼트는_롤링_삭제된다() {
        LocalDateTime old = LocalDateTime.now().minusDays(5);
        repository.save(new TrackSegment(1L, "37.5", "127.0", old, old.plusMinutes(5), old));

        int deleted = repository.deleteByCreatedAtBefore(LocalDateTime.now().minusDays(3));
        assertThat(deleted).isEqualTo(1);
    }
}
