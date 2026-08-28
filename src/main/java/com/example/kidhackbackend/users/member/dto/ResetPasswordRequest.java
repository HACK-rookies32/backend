package com.example.kidhackbackend.users.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "비밀번호 재설정 요청")
@Getter
public class ResetPasswordRequest {

    @Schema(description = "인증코드를 받은 이메일", example = "parent01@test.com")
    private String email;

    @Schema(description = "발송받은 인증코드", example = "269507")
    private String code;

    @Schema(description = "새로 설정할 비밀번호", example = "newpassword123")
    private String newPassword;
}
