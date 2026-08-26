package com.example.kidhackbackend.admin.community.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class MemberBanRequest {

    @NotBlank
    private String loginId;

    @NotBlank
    private String reason;

    // 0 이면 밴 해제
    @NotNull
    @Min(0)
    private Integer durationDays;
}
