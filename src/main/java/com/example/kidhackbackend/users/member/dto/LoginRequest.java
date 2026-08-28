package com.example.kidhackbackend.users.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "로그인 요청")
@Getter @Setter
public class LoginRequest {

    @Schema(description = "(필수) 로그인 아이디", example = "parent01", requiredMode = RequiredMode.REQUIRED)
    private String loginId;

    @Schema(description = "(필수) 비밀번호", example = "1234", requiredMode = RequiredMode.REQUIRED)
    private String password;
}
