package com.example.kidhackbackend.location.service;

import com.example.kidhackbackend.location.entity.Place;
import com.example.kidhackbackend.location.entity.PlaceEvent;
import com.example.kidhackbackend.location.entity.PlaceEventType;
import com.example.kidhackbackend.location.entity.PlaceType;
import com.example.kidhackbackend.location.repository.PlaceEventRepository;
import com.example.kidhackbackend.location.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 지오펜스 진입/이탈 판정(No.1)과 목적지 도착 판정(No.3).
 *
 * <p>새 좌표가 들어올 때마다 자녀의 등록 공간을 순회하며 상태 전환을 감지하고,
 * 전환 시점에 {@link AlertPort}로 알림을 넘긴다.
 */
@Service
@RequiredArgsConstructor
public class GeofenceService {

    private final PlaceRepository placeRepository;
    private final PlaceEventRepository eventRepository;
    private final AlertPort alertPort;

    /**
     * 현재 좌표를 자녀의 모든 등록 공간과 대조하여 이벤트를 발생시킨다.
     *
     * @return 이번 좌표로 새로 발생한 이벤트 목록
     */
    @Transactional
    public List<PlaceEvent> evaluate(Long childId, double latitude, double longitude, LocalDateTime now) {
        return placeRepository.findByChildId(childId).stream()
                .map(place -> evaluatePlace(place, childId, latitude, longitude, now))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private Optional<PlaceEvent> evaluatePlace(Place place, Long childId,
                                               double latitude, double longitude, LocalDateTime now) {
        double distance = GeoUtils.distanceMeters(
                latitude, longitude, place.getLatitude(), place.getLongitude());
        boolean inside = distance <= place.getRadiusMeters();

        if (place.getType() == PlaceType.DESTINATION) {
            return evaluateDestination(place, childId, inside, now);
        }
        return evaluateGeofence(place, childId, inside, now);
    }

    /** 지오펜스: 직전 상태와 비교해 진입/이탈 전환 시에만 이벤트 발생 (No.1). */
    private Optional<PlaceEvent> evaluateGeofence(Place place, Long childId,
                                                  boolean inside, LocalDateTime now) {
        Optional<PlaceEvent> last =
                eventRepository.findFirstByChildIdAndPlaceIdOrderByOccurredAtDesc(childId, place.getId());
        boolean wasInside = last.map(e -> e.getEventType() == PlaceEventType.ENTER).orElse(false);

        if (inside && !wasInside) {
            return Optional.of(record(childId, place, PlaceEventType.ENTER, now,
                    "'" + place.getName() + "'에 진입했습니다."));
        }
        if (!inside && wasInside) {
            return Optional.of(record(childId, place, PlaceEventType.EXIT, now,
                    "'" + place.getName() + "'에서 이탈했습니다."));
        }
        return Optional.empty();
    }

    /** 목적지: 최초 진입 시 ARRIVED 1회만 발송 (No.3, 중복 방지). */
    private Optional<PlaceEvent> evaluateDestination(Place place, Long childId,
                                                     boolean inside, LocalDateTime now) {
        if (!inside) {
            return Optional.empty();
        }
        boolean alreadyArrived = eventRepository.existsByChildIdAndPlaceIdAndEventType(
                childId, place.getId(), PlaceEventType.ARRIVED);
        if (alreadyArrived) {
            return Optional.empty();
        }
        return Optional.of(record(childId, place, PlaceEventType.ARRIVED, now,
                "목적지 '" + place.getName() + "'에 도착했습니다."));
    }

    private PlaceEvent record(Long childId, Place place, PlaceEventType type,
                              LocalDateTime now, String message) {
        PlaceEvent event = eventRepository.save(new PlaceEvent(childId, place.getId(), type, now));
        alertPort.sendLocationAlert(childId, type.name(), message);
        return event;
    }
}
