package com.example.kidhackbackend.users.child.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Schema(description = "자녀 프로필 수정 요청 — 모든 필드 선택, 포함된(non-null) 필드만 반영")
@Getter
public class ChildUpdateRequest {

    @Schema(description = "(선택) 변경할 닉네임", example = "우리하늘이")
    private String nickname;

    @Schema(description = "(선택) 변경할 프로필 이미지 URL")
    private String profileImageUrl;
}
