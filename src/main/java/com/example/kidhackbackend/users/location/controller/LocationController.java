package com.example.kidhackbackend.users.location.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.location.dto.PlaceEventResponse;
import com.example.kidhackbackend.users.location.dto.PlaceRequest;
import com.example.kidhackbackend.users.location.dto.PlaceResponse;
import com.example.kidhackbackend.users.location.dto.TrackRequest;
import com.example.kidhackbackend.users.location.dto.TrackResponse;
import com.example.kidhackbackend.users.location.dto.TrackSegmentResponse;
import com.example.kidhackbackend.users.location.dto.TrackingSettingResponse;
import com.example.kidhackbackend.users.location.service.PlaceService;
import com.example.kidhackbackend.users.location.service.TrackingService;
import com.example.kidhackbackend.users.location.service.TrackingSettingService;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.member.entity.Role;
import com.example.kidhackbackend.users.member.repository.MemberRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 위치 추적 API — 역할표 No.1(지오펜스)·No.2(동선 저장)·No.3(도착).
 *
 * <p>[VULN-LOC-1] IDOR: 모든 엔드포인트가 대상 자녀를 childId(요청 본문/경로)로만 지정하고
 * 인증 사용자가 그 자녀 본인/부모인지 검증하지 않는다. (childId 가 실재하는 CHILD 인지만 검증한다)
 */
@Tag(name = "05. 위치 추적", description = "동선 업로드(CHILD) · 동선/이벤트 조회 · 지오펜스 등록 · 실시간 추적 토글(PARENT)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final TrackingService trackingService;
    private final TrackingSettingService trackingSettingService;
    private final PlaceService placeService;
    private final MemberRepository memberRepository;

    /** childId 가 실재하는(삭제되지 않은) CHILD 계정인지 확인한다. 소유권(IDOR)은 검증하지 않는다. */
    private void requireExistingChild(Long childId) {
        Member child = memberRepository.findById(childId)
                .orElseThrow(() -> new IllegalArgumentException("자녀를 찾을 수 없습니다."));
        if (child.isDeleted() || child.getRole() != Role.CHILD) {
            throw new IllegalArgumentException("자녀를 찾을 수 없습니다.");
        }
    }

    @Operation(summary = "위치 업로드 (CHILD)",
            description = "자녀 앱이 현재 좌표를 업로드한다. 5분 세그먼트에 반영하고 지오펜스 진입/이탈·도착을 판정한다. "
                    + "⚠️ 본문 childId 소유권 미검증 → 다른 자녀 위치 위조 가능(IDOR).")
    @PostMapping("/track")
    public ResponseEntity<ApiResponse<TrackSegmentResponse>> track(@Valid @RequestBody TrackRequest request) {
        return ResponseEntity.ok(ApiResponse.success(TrackSegmentResponse.from(trackingService.record(request))));
    }

    @Operation(summary = "실시간 추적 on/off 설정 (PARENT)",
            description = "OFF=5분 세그먼트, ON=실시간 포인트 저장. childId 가 실재 CHILD 인지 검증. "
                    + "⚠️ 부모-자녀 소유권은 미검증(IDOR).")
    @PatchMapping("/tracking/{childId}/realtime")
    public ResponseEntity<ApiResponse<TrackingSettingResponse>> setRealtime(
            @Parameter(description = "자녀 회원 ID") @PathVariable Long childId,
            @Parameter(description = "실시간 추적 활성화 여부") @RequestParam boolean enabled) {
        requireExistingChild(childId);
        return ResponseEntity.ok(ApiResponse.success(
                TrackingSettingResponse.from(trackingSettingService.setRealtime(childId, enabled))));
    }

    @Operation(summary = "실시간 추적 상태 조회 (PARENT)",
            description = "현재 on/off 상태. 설정이 없으면 기본 false. ⚠️ childId 소유권 미검증(IDOR).")
    @GetMapping("/tracking/{childId}/realtime")
    public ResponseEntity<ApiResponse<TrackingSettingResponse>> getRealtime(
            @Parameter(description = "자녀 회원 ID") @PathVariable Long childId) {
        return ResponseEntity.ok(ApiResponse.success(
                new TrackingSettingResponse(childId, trackingSettingService.isRealtimeEnabled(childId))));
    }

    @Operation(summary = "동선 조회 (PARENT)",
            description = """
                    자녀의 동선을 위·경도 포인트 리스트로 반환한다(시간 오름차순). 프론트는 points 를 순서대로 이어 동선을 그린다.

                    - `date` (yyyy-MM-dd): 해당 하루치만. `from`/`to` 보다 우선.
                    - `from`/`to` (ISO-8601 date-time): 구간 조회. 둘 다 있어야 적용.
                    - 아무것도 없으면 보관 기간(기본 3일) 전체.

                    ⚠️ childId 소유권 미검증(IDOR).
                    """)
    @GetMapping("/track/{childId}")
    public ResponseEntity<ApiResponse<TrackResponse>> track(
            @Parameter(description = "자녀 회원 ID") @PathVariable Long childId,
            @Parameter(description = "조회할 하루(yyyy-MM-dd). from/to 보다 우선")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "구간 시작(포함, ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "구간 종료(제외, ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        LocalDateTime[] window = resolveWindow(date, from, to);
        return ResponseEntity.ok(ApiResponse.success(
                TrackResponse.of(trackingService.getTrack(childId, window[0], window[1]))));
    }

    @Operation(summary = "지오펜스/목적지 등록 (PARENT)",
            description = "진입/이탈 알림 대상(GEOFENCE) 또는 도착 판정 대상(DESTINATION) 공간을 등록한다. "
                    + "childId 가 실재 CHILD 인지 검증. ⚠️ 부모-자녀 소유권은 미검증(IDOR).")
    @PostMapping("/places")
    public ResponseEntity<ApiResponse<PlaceResponse>> registerPlace(@Valid @RequestBody PlaceRequest request) {
        requireExistingChild(request.childId());
        return ResponseEntity.ok(ApiResponse.success(PlaceResponse.from(placeService.register(request))));
    }

    @Operation(summary = "등록 공간 목록 조회 (PARENT)",
            description = "자녀의 등록 공간(집/학원 등) 목록. ⚠️ childId 소유권 미검증(IDOR).")
    @GetMapping("/places/{childId}")
    public ResponseEntity<ApiResponse<List<PlaceResponse>>> places(
            @Parameter(description = "자녀 회원 ID") @PathVariable Long childId) {
        return ResponseEntity.ok(ApiResponse.success(placeService.findPlaces(childId).stream()
                .map(PlaceResponse::from)
                .toList()));
    }

    @Operation(summary = "이동 이벤트 이력 조회 (PARENT)",
            description = """
                    진입/이탈/도착 이벤트 이력(최신순).

                    - `date` (yyyy-MM-dd): 해당 하루치만. `from`/`to` 보다 우선.
                    - `from`/`to` (ISO-8601 date-time): 구간 조회. 둘 다 있어야 적용.
                    - 아무것도 없으면 전체.

                    ⚠️ childId 소유권 미검증(IDOR).
                    """)
    @GetMapping("/events/{childId}")
    public ResponseEntity<ApiResponse<List<PlaceEventResponse>>> events(
            @Parameter(description = "자녀 회원 ID") @PathVariable Long childId,
            @Parameter(description = "조회할 하루(yyyy-MM-dd). from/to 보다 우선")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "구간 시작(포함, ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "구간 종료(제외, ISO-8601)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        LocalDateTime[] window = resolveWindow(date, from, to);
        return ResponseEntity.ok(ApiResponse.success(placeService.findEvents(childId, window[0], window[1]).stream()
                .map(PlaceEventResponse::from)
                .toList()));
    }

    @Operation(summary = "지오펜스/목적지 삭제 (PARENT)",
            description = "본인 자녀 소유의 장소인지 검증 후 삭제한다(비소유자 → 403).")
    @DeleteMapping("/places/{placeId}")
    public ResponseEntity<ApiResponse<Void>> deletePlace(
            @AuthenticationPrincipal Member member,
            @Parameter(description = "장소 ID") @PathVariable Long placeId) {
        placeService.delete(placeId, member);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** date 가 있으면 그 하루 [00:00, 다음날 00:00), 없으면 from/to 그대로(null 허용). */
    private LocalDateTime[] resolveWindow(LocalDate date, LocalDateTime from, LocalDateTime to) {
        if (date != null) {
            return new LocalDateTime[]{date.atStartOfDay(), date.plusDays(1).atStartOfDay()};
        }
        return new LocalDateTime[]{from, to};
    }
}
