package com.example.kidhackbackend.location;

import com.example.kidhackbackend.location.dto.TrackRequest;
import com.example.kidhackbackend.location.entity.TrackSegment;
import com.example.kidhackbackend.location.repository.TrackSegmentRepository;
import com.example.kidhackbackend.location.service.GeofenceService;
import com.example.kidhackbackend.location.service.LogAlertAdapter;
import com.example.kidhackbackend.location.service.TrackingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({TrackingService.class, GeofenceService.class, LogAlertAdapter.class})
class TrackingServiceTest {

    @Autowired
    TrackingService trackingService;

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
    void 보관기간_경과_세그먼트는_롤링_삭제된다() {
        LocalDateTime old = LocalDateTime.now().minusDays(5);
        repository.save(new TrackSegment(1L, "37.5", "127.0", old, old.plusMinutes(5), old));

        int deleted = repository.deleteByCreatedAtBefore(LocalDateTime.now().minusDays(3));
        assertThat(deleted).isEqualTo(1);
    }
}
