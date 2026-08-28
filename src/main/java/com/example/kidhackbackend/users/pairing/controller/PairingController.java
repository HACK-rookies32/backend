package com.example.kidhackbackend.users.pairing.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.pairing.dto.PairingCodeResponse;
import com.example.kidhackbackend.users.pairing.dto.PairingConnectRequest;
import com.example.kidhackbackend.users.pairing.service.PairingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "03. 페어링", description = "부모-자녀 페어링 코드 발급/조회(PARENT) · 연결(CHILD)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/pairing")
@RequiredArgsConstructor
public class PairingController {

    private final PairingService pairingService;

    @Operation(summary = "페어링 코드 발급 (PARENT)",
            description = """
                    자녀 연결용 6자리 코드를 발급한다. 유효/보관 시간 5분.

                    - 재발급해도 기존 코드는 무효화되지 않아 여러 개가 동시에 유효할 수 있다.
                    - DB에 이미 있는 코드와 겹치면 자동 재생성한다.
                    - 미사용 코드는 만료 후 스케줄러가 1분 주기로 삭제한다.
                    """)
    @PostMapping("/code")
    public ResponseEntity<ApiResponse<PairingCodeResponse>> issueCode(@AuthenticationPrincipal Member member) {
        return ResponseEntity.ok(ApiResponse.success(pairingService.issueCode(member.getId())));
    }

    @Operation(summary = "발급한 페어링 코드 조회 (PARENT)",
            description = "현재 로그인한 부모가 발급한 코드 중 아직 만료되지 않은 것을 만료 시각 내림차순으로 반환한다. 없으면 빈 배열.")
    @GetMapping("/code")
    public ResponseEntity<ApiResponse<List<PairingCodeResponse>>> activeCodes(@AuthenticationPrincipal Member member) {
        return ResponseEntity.ok(ApiResponse.success(pairingService.getActiveCodes(member.getId())));
    }

    @Operation(summary = "페어링 연결 (CHILD)",
            description = """
                    자녀가 부모에게 받은 코드를 입력해 부모 계정과 연결한다. 성공 시 자녀의 `parent` 가 해당 부모로 설정되고 코드는 삭제된다.

                    | 필드 | 필수 | 설명 |
                    |---|:---:|---|
                    | `code` | ✅ | 부모가 발급받은 6자리 페어링 코드 |

                    - 존재하지 않는 코드: `400 INVALID_PARAMETER` "유효하지 않은 페어링 코드입니다."
                    - 만료된 코드: `400 INVALID_PARAMETER` "만료된 페어링 코드입니다."

                    ⚠️ 코드 조회가 파라미터 바인딩 없이 문자열 결합으로 구현되어 SQL Injection 에 취약(의도). 이미 다른 부모와 연결된 자녀도 재연결(덮어쓰기) 가능.
                    """)
    @PostMapping("/connect")
    public ResponseEntity<ApiResponse<Void>> connect(@AuthenticationPrincipal Member member,
                                                     @RequestBody PairingConnectRequest request) {
        pairingService.connect(member.getId(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
