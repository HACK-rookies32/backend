package com.example.kidhackbackend.location;

import com.example.kidhackbackend.users.location.entity.Place;
import com.example.kidhackbackend.users.location.entity.PlaceEvent;
import com.example.kidhackbackend.users.location.entity.PlaceEventType;
import com.example.kidhackbackend.users.location.entity.PlaceType;
import com.example.kidhackbackend.users.location.repository.PlaceRepository;
import com.example.kidhackbackend.users.location.service.GeofenceService;
import com.example.kidhackbackend.users.location.service.LogAlertAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({GeofenceService.class, LogAlertAdapter.class})
class GeofenceServiceTest {

    @Autowired
    GeofenceService geofenceService;

    @Autowired
    PlaceRepository placeRepository;

    // 서울시청 근방 좌표
    private static final double LAT_IN = 37.5665;
    private static final double LON_IN = 126.9780;
    // 약 3km 떨어진 좌표
    private static final double LAT_OUT = 37.5400;
    private static final double LON_OUT = 126.9500;

    @Test
    void 지오펜스_진입시_ENTER_이탈시_EXIT_이벤트가_발생한다() {
        placeRepository.save(new Place(1L, "학원", LAT_IN, LON_IN, 200, PlaceType.GEOFENCE));

        List<PlaceEvent> enter = geofenceService.evaluate(1L, LAT_IN, LON_IN, LocalDateTime.now());
        assertThat(enter).extracting(PlaceEvent::getEventType).containsExactly(PlaceEventType.ENTER);

        // 같은 상태(내부) 재호출 → 이벤트 없음
        List<PlaceEvent> stay = geofenceService.evaluate(1L, LAT_IN, LON_IN, LocalDateTime.now());
        assertThat(stay).isEmpty();

        // 이탈 → EXIT
        List<PlaceEvent> exit = geofenceService.evaluate(1L, LAT_OUT, LON_OUT, LocalDateTime.now());
        assertThat(exit).extracting(PlaceEvent::getEventType).containsExactly(PlaceEventType.EXIT);
    }

    @Test
    void 목적지_도착은_최초_1회만_ARRIVED를_발생한다() {
        placeRepository.save(new Place(2L, "집", LAT_IN, LON_IN, 150, PlaceType.DESTINATION));

        List<PlaceEvent> first = geofenceService.evaluate(2L, LAT_IN, LON_IN, LocalDateTime.now());
        assertThat(first).extracting(PlaceEvent::getEventType).containsExactly(PlaceEventType.ARRIVED);

        // 재진입해도 도착은 다시 발생하지 않음
        List<PlaceEvent> again = geofenceService.evaluate(2L, LAT_IN, LON_IN, LocalDateTime.now());
        assertThat(again).isEmpty();
    }

    @Test
    void 반경_밖에서는_아무_이벤트도_없다() {
        placeRepository.save(new Place(3L, "학원", LAT_IN, LON_IN, 200, PlaceType.GEOFENCE));

        List<PlaceEvent> events = geofenceService.evaluate(3L, LAT_OUT, LON_OUT, LocalDateTime.now());
        assertThat(events).isEmpty();
    }
}
