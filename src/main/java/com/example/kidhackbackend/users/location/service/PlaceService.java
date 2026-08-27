package com.example.kidhackbackend.users.location.service;

import com.example.kidhackbackend.users.location.dto.PlaceRequest;
import com.example.kidhackbackend.users.location.entity.Place;
import com.example.kidhackbackend.users.location.entity.PlaceEvent;
import com.example.kidhackbackend.users.location.repository.PlaceEventRepository;
import com.example.kidhackbackend.users.location.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.repository.MemberRepository;

import java.util.List;

/** 등록 공간(지오펜스/목적지) 관리 및 이벤트 조회 — No.1·No.3. */
@Service
@RequiredArgsConstructor
public class PlaceService {
    private final MemberRepository memberRepository;
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

    @Transactional
    public void delete(Long placeId, Member requester) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장소입니다."));

        Member child = memberRepository.findById(place.getChildId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 자녀입니다."));

        if (child.getParent() == null || !child.getParent().getId().equals(requester.getId())) {
            throw new IllegalArgumentException("본인 자녀의 장소만 삭제할 수 있습니다.");
        }

        placeRepository.delete(place);
    }
}
