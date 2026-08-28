package com.example.kidhackbackend.admin.member.dto;

import com.example.kidhackbackend.users.member.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Schema(description = "관리자 역할 변경 요청")
@Getter
public class AdminRoleUpdateRequest {

    @Schema(description = "새 역할", example = "COMMUNITY_ADMIN")
    @NotNull
    private Role role;
}
