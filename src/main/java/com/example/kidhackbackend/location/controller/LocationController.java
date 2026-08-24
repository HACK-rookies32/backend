package com.example.kidhackbackend.location.controller;

import com.example.kidhackbackend.location.dto.PlaceEventResponse;
import com.example.kidhackbackend.location.dto.PlaceRequest;
import com.example.kidhackbackend.location.dto.PlaceResponse;
import com.example.kidhackbackend.location.dto.TrackRequest;
import com.example.kidhackbackend.location.dto.TrackSegmentResponse;
import com.example.kidhackbackend.location.service.PlaceService;
import com.example.kidhackbackend.location.service.TrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 위치 추적 API — 역할표 No.1(지오펜스)·No.2(동선 저장)·No.3(도착).
 */
@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final TrackingService trackingService;
    private final PlaceService placeService;

    /** 자녀 앱이 현재 좌표를 업로드 → 5분 세그먼트에 반영 + 지오펜스/도착 판정 (No.2). */
    @PostMapping("/track")
    public ResponseEntity<TrackSegmentResponse> track(@Valid @RequestBody TrackRequest request) {
        return ResponseEntity.ok(TrackSegmentResponse.from(trackingService.record(request)));
    }

    /** 자녀의 저장된 동선 조회(최신순). */
    @GetMapping("/track/{childId}")
    public ResponseEntity<List<TrackSegmentResponse>> track(@PathVariable Long childId) {
        return ResponseEntity.ok(trackingService.findTrack(childId).stream()
                .map(TrackSegmentResponse::from)
                .toList());
    }

    /** 지오펜스/목적지 등록 (No.1·No.3). */
    @PostMapping("/places")
    public ResponseEntity<PlaceResponse> registerPlace(@Valid @RequestBody PlaceRequest request) {
        return ResponseEntity.ok(PlaceResponse.from(placeService.register(request)));
    }

    /** 자녀의 등록 공간 목록. */
    @GetMapping("/places/{childId}")
    public ResponseEntity<List<PlaceResponse>> places(@PathVariable Long childId) {
        return ResponseEntity.ok(placeService.findPlaces(childId).stream()
                .map(PlaceResponse::from)
                .toList());
    }

    /** 진입/이탈/도착 이벤트 이력 (No.1·No.3). */
    @GetMapping("/events/{childId}")
    public ResponseEntity<List<PlaceEventResponse>> events(@PathVariable Long childId) {
        return ResponseEntity.ok(placeService.findEvents(childId).stream()
                .map(PlaceEventResponse::from)
                .toList());
    }
}
