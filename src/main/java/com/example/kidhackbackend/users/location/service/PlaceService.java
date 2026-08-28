package com.example.kidhackbackend.users.location.service;

import com.example.kidhackbackend.global.exception.AdminAccessDeniedException;
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

    /**
     * 자녀의 이벤트 이력 조회(최신순).
     *
     * @param from 시작(포함). null 이면 하한 없음
     * @param to   종료(제외). null 이면 상한 없음
     */
    @Transactional(readOnly = true)
    public List<PlaceEvent> findEvents(Long childId, java.time.LocalDateTime from, java.time.LocalDateTime to) {
        if (from != null && to != null) {
            return eventRepository.findEventsInRange(childId, from, to);
        }
        return eventRepository.findByChildIdOrderByOccurredAtDesc(childId);
    }

    @Transactional
    public void delete(Long placeId, Member requester) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 장소입니다."));

        Member child = memberRepository.findById(place.getChildId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 자녀입니다."));

        if (child.getParent() == null || !child.getParent().getId().equals(requester.getId())) {
            // 권한 문제이므로 403. (다른 도메인과 동일하게 표준 봉투로 나간다)
            throw new AdminAccessDeniedException("본인 자녀의 장소만 삭제할 수 있습니다.");
        }

        placeRepository.delete(place);
    }
}
