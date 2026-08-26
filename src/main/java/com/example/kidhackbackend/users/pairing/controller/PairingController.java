package com.example.kidhackbackend.users.pairing.controller;

import com.example.kidhackbackend.global.response.ApiResponse;
import com.example.kidhackbackend.users.member.entity.Member;
import com.example.kidhackbackend.users.pairing.dto.PairingCodeResponse;
import com.example.kidhackbackend.users.pairing.dto.PairingConnectRequest;
import com.example.kidhackbackend.users.pairing.service.PairingService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Pairing", description = "부모-자녀 페어링 API — JWT 인증 필요")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/pairing")
@RequiredArgsConstructor
public class PairingController {

    private final PairingService pairingService;

    @PostMapping("/code")
    public ResponseEntity<ApiResponse<PairingCodeResponse>> issueCode(@AuthenticationPrincipal Member member) {
        return ResponseEntity.ok(ApiResponse.success(pairingService.issueCode(member.getId())));
    }

    @PostMapping("/connect")
    public ResponseEntity<ApiResponse<Void>> connect(@AuthenticationPrincipal Member member,
                                                     @RequestBody PairingConnectRequest request) {
        pairingService.connect(member.getId(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}