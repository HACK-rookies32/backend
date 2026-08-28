package com.example.kidhackbackend.admin.community.dto;

import com.example.kidhackbackend.admin.community.entity.MemberBan;
import com.example.kidhackbackend.users.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "커뮤니티 사용자 밴 상태")
@Getter
@RequiredArgsConstructor
public class MemberBanResponse {

    @Schema(description = "대상 회원 ID", example = "1")
    private final Long memberId;

    @Schema(description = "대상 회원 로그인 아이디", example = "parent01")
    private final String loginId;

    @Schema(description = "밴 상태 여부", example = "true")
    private final boolean banned;

    @Schema(description = "사유")
    private final String reason;

    @Schema(description = "밴 해제 예정 시각(해제 상태면 null)")
    private final LocalDateTime bannedUntil;

    public static MemberBanResponse from(Member member, MemberBan ban) {
        return new MemberBanResponse(
                member.getId(),
                member.getLoginId(),
                ban.isBanned(),
                ban.getReason(),
                ban.getBannedUntil()
        );
    }
}
