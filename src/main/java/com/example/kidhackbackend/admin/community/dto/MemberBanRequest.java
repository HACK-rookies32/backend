package com.example.kidhackbackend.admin.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Schema(description = "커뮤니티 사용자 밴/해제 요청")
@Getter
public class MemberBanRequest {

    @Schema(description = "대상 회원 로그인 아이디", example = "parent01")
    @NotBlank
    private String loginId;

    @Schema(description = "사유")
    @NotBlank
    private String reason;

    @Schema(description = "밴 기간(일). 0 이면 밴 해제", example = "7")
    @NotNull
    @Min(0)
    private Integer durationDays;
}
