package com.example.kidhackbackend.users.communityboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Schema(description = "좋아요 토글 결과")
@Getter
@AllArgsConstructor
public class LikeResponse {

    @Schema(description = "토글 후 내가 좋아요한 상태인지", example = "true")
    private boolean liked;

    @Schema(description = "토글 후 총 좋아요 수", example = "4")
    private long likeCount;
}
