package com.example.kidhackbackend.users.pairing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "페어링 연결 요청")
@Getter
public class PairingConnectRequest {

    @Schema(description = "부모가 발급받은 6자리 페어링 코드", example = "840975")
    private String code;
}
