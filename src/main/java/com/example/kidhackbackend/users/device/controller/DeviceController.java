package com.example.kidhackbackend.users.device.controller;

import com.example.kidhackbackend.users.device.dto.DeviceEventRequest;
import com.example.kidhackbackend.users.device.service.DeviceEventService;
import com.example.kidhackbackend.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 기기 전원/GPS 감지 API — 역할표 No.4.
 */
@Tag(name = "09. 기기 전원/GPS 감지", description = "자녀 앱이 전원/GPS 꺼짐을 보고 → '트래킹 불가' 알림 발행")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceEventService deviceEventService;

    @Operation(summary = "기기 이벤트 보고 (자녀 앱)",
            description = """
                    전원 꺼짐/GPS 꺼짐을 보고하면 부모에게 '트래킹 불가' 알림을 발행한다. 모든 필드 필수.

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `childId` | ✅ | 자녀 회원 ID |
                    | `deviceId` | ✅ | 기기 식별자 |
                    | `type` | ✅ | `POWER_OFF` / `GPS_OFF` |

                    ⚠️ [VULN-DEV-1] 본문 childId 본인 검증 없음 → 타인 childId로 위조 이벤트를 보내 거짓 알림을 유발할 수 있다(IDOR / 알림 스팸). 호출 rate limit 도 없음.""")
    @PostMapping("/event")
    public ResponseEntity<ApiResponse<Void>> report(@Valid @RequestBody DeviceEventRequest request) {
        deviceEventService.report(request.childId(), request.deviceId(), request.type());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
