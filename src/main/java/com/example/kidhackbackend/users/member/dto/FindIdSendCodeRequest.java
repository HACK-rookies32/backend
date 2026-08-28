package com.example.kidhackbackend.users.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "아이디 찾기 - 인증코드 발송 요청")
@Getter
public class FindIdSendCodeRequest {

    @Schema(description = "찾으려는 계정의 실명(자녀 계정이면 자녀 실명)", example = "홍길동")
    private String username;

    @Schema(description = "본인 이메일 또는 부모의 이메일", example = "parent01@test.com")
    private String email;
}
