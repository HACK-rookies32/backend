package com.example.kidhackbackend.users.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "로그인 요청")
@Getter @Setter
public class LoginRequest {

    @Schema(description = "로그인 아이디", example = "parent01")
    private String loginId;

    @Schema(description = "비밀번호", example = "1234")
    private String password;
}
