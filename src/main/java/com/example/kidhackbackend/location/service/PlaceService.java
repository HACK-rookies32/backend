package com.example.kidhackbackend.location.service;

import com.example.kidhackbackend.location.dto.PlaceRequest;
import com.example.kidhackbackend.location.entity.Place;
import com.example.kidhackbackend.location.entity.PlaceEvent;
import com.example.kidhackbackend.location.repository.PlaceEventRepository;
import com.example.kidhackbackend.location.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 등록 공간(지오펜스/목적지) 관리 및 이벤트 조회 — No.1·No.3. */
@Service
@RequiredArgsConstructor
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final PlaceEventRepository eventRepository;

    @Transactional
    public Place register(PlaceRequest request) {
        return placeRepository.save(new Place(
                request.childId(),
                request.name().trim(),
                request.latitude(),
                request.longitude(),
                request.radiusMeters(),
                request.type()));
    }

    @Transactional(readOnly = true)
    public List<Place> findPlaces(Long childId) {
        return placeRepository.findByChildId(childId);
    }

    @Transactional(readOnly = true)
    public List<PlaceEvent> findEvents(Long childId) {
        return eventRepository.findByChildIdOrderByOccurredAtDesc(childId);
    }
}
