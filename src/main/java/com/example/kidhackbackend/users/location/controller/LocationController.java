package com.example.kidhackbackend.users.location.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.location.dto.PlaceEventResponse;
import com.example.kidhackbackend.users.location.dto.PlaceRequest;
import com.example.kidhackbackend.users.location.dto.PlaceResponse;
import com.example.kidhackbackend.users.location.dto.TrackRequest;
import com.example.kidhackbackend.users.location.dto.TrackSegmentResponse;
import com.example.kidhackbackend.users.location.dto.TrackingSettingResponse;
import com.example.kidhackbackend.users.location.service.PlaceService;
import com.example.kidhackbackend.users.location.service.TrackingService;
import com.example.kidhackbackend.users.location.service.TrackingSettingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.example.kidhackbackend.users.member.entity.Member;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import java.util.List;

/**
 * 위치 추적 API — 역할표 No.1(지오펜스)·No.2(동선 저장)·No.3(도착).
 *
 * <p>모든 엔드포인트는 JWT 인증이 필요하다(위치 업로드=CHILD, 조회/등록=PARENT).
 *
 * <p>[VULN-LOC-1] IDOR (권한 상승/타인 데이터 접근): 모든 엔드포인트가 대상 자녀를
 * {@code childId}(요청 본문/경로)로만 지정하고, <b>인증된 사용자가 그 자녀 본인인지 /
 * 그 자녀의 부모인지 전혀 검증하지 않는다.</b> 역할(CHILD/PARENT)만 맞으면 통과하므로:
 * <ul>
 *   <li>아무 부모나 다른 집 자녀의 childId로 동선·이벤트·공간을 조회/등록 가능</li>
 *   <li>아무 자녀나 다른 자녀의 childId로 위치를 업로드(위치 위조) 가능</li>
 * </ul>
 * 근본 해결: 토큰의 사용자 → Member(자녀/부모) 매핑 후 소유권을 검증해야 한다(2단계 예정).
 */
@Tag(name = "Location", description = "위치 추적 API (동선/지오펜스/도착) — JWT 인증 필요")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final TrackingService trackingService;
    private final TrackingSettingService trackingSettingService;
    private final PlaceService placeService;

    /** 자녀 앱이 현재 좌표를 업로드 → 5분 세그먼트에 반영 + 지오펜스/도착 판정 (No.2). */
    // [VULN-LOC-1] childId를 요청 본문에서 그대로 받는다. 인증된 자녀 본인의 id인지 확인하지 않아
    //              다른 자녀의 위치를 위조 업로드할 수 있다. (토큰 subject로 childId를 도출해야 함)
    @PostMapping("/track")
    public ResponseEntity<ApiResponse<TrackSegmentResponse>> track(@Valid @RequestBody TrackRequest request) {
        return ResponseEntity.ok(ApiResponse.success(TrackSegmentResponse.from(trackingService.record(request))));
    }

    /** 실시간 위치 추적 on/off 토글(부모). OFF=5분 세그먼트, ON=실시간 포인트 저장. */
    // [VULN-LOC-1] childId 소유권 미검증 → 남의 자녀의 실시간 추적을 임의로 켜고 끌 수 있다.
    @PatchMapping("/tracking/{childId}/realtime")
    public ResponseEntity<ApiResponse<TrackingSettingResponse>> setRealtime(@PathVariable Long childId,
                                                                            @RequestParam boolean enabled) {
        return ResponseEntity.ok(ApiResponse.success(
                TrackingSettingResponse.from(trackingSettingService.setRealtime(childId, enabled))));
    }

    /** 실시간 위치 추적 on/off 현재 상태 조회(부모). 설정이 없으면 기본 false. */
    // [VULN-LOC-1] childId 소유권 미검증 → 남의 자녀의 실시간 추적 활성화 여부를 열람할 수 있다(감시 상태 노출).
    //              VULN-LOC-3(토글 시 childId 미검증)·VULN-LOC-5(감사 로그 부재)와 결합하면
    //              남의 자녀를 몰래 ON 시킨 뒤 유지 여부까지 확인 가능하다(스토킹 시나리오 완결).
    @GetMapping("/tracking/{childId}/realtime")
    public ResponseEntity<ApiResponse<TrackingSettingResponse>> getRealtime(@PathVariable Long childId) {
        return ResponseEntity.ok(ApiResponse.success(
                new TrackingSettingResponse(childId, trackingSettingService.isRealtimeEnabled(childId))));
    }

    /** 자녀의 저장된 동선 조회(최신순). */
    // [VULN-LOC-1] 경로의 childId가 인증된 부모의 '자기 자녀'인지 검증하지 않는다.
    //              → 아무 부모나 임의의 childId로 남의 자녀 동선을 조회할 수 있다(IDOR).
    @GetMapping("/track/{childId}")
    public ResponseEntity<ApiResponse<List<TrackSegmentResponse>>> track(@PathVariable Long childId) {
        return ResponseEntity.ok(ApiResponse.success(trackingService.findTrack(childId).stream()
                .map(TrackSegmentResponse::from)
                .toList()));
    }

    /** 지오펜스/목적지 등록 (No.1·No.3). */
    // [VULN-LOC-1] 본문의 childId 소유권 미검증 → 남의 자녀에게 임의 공간(지오펜스)을 등록 가능.
    @PostMapping("/places")
    public ResponseEntity<ApiResponse<PlaceResponse>> registerPlace(@Valid @RequestBody PlaceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(PlaceResponse.from(placeService.register(request))));
    }

    /** 자녀의 등록 공간 목록. */
    // [VULN-LOC-1] childId 소유권 미검증 → 남의 자녀 등록 공간(집/학원 위치)까지 열람 가능.
    @GetMapping("/places/{childId}")
    public ResponseEntity<ApiResponse<List<PlaceResponse>>> places(@PathVariable Long childId) {
        return ResponseEntity.ok(ApiResponse.success(placeService.findPlaces(childId).stream()
                .map(PlaceResponse::from)
                .toList()));
    }

    /** 진입/이탈/도착 이벤트 이력 (No.1·No.3). */
    // [VULN-LOC-1] childId 소유권 미검증 → 남의 자녀 이동 이벤트 이력까지 열람 가능(IDOR).
    @GetMapping("/events/{childId}")
    public ResponseEntity<ApiResponse<List<PlaceEventResponse>>> events(@PathVariable Long childId) {
        return ResponseEntity.ok(ApiResponse.success(placeService.findEvents(childId).stream()
                .map(PlaceEventResponse::from)
                .toList()));
    }
    /** 지오펜스/목적지 삭제. 본인 자녀 소유의 장소인지 검증한다. */
    @DeleteMapping("/places/{placeId}")
    public ResponseEntity<ApiResponse<Void>> deletePlace(
            @AuthenticationPrincipal Member member,
            @PathVariable Long placeId) {
        placeService.delete(placeId, member);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
