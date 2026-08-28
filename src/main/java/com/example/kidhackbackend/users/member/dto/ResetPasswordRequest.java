package com.example.kidhackbackend.users.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.Getter;

@Schema(description = "비밀번호 재설정 요청")
@Getter
public class ResetPasswordRequest {

    @Schema(description = "(필수) 인증코드를 받은 이메일", example = "parent01@test.com",
            requiredMode = RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "(필수) 발송받은 인증코드", example = "269507", requiredMode = RequiredMode.REQUIRED)
    private String code;

    @Schema(description = "(필수) 새로 설정할 비밀번호", example = "newpassword123",
            requiredMode = RequiredMode.REQUIRED)
    private String newPassword;
}
