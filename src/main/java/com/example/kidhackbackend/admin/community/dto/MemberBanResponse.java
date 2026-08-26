package com.example.kidhackbackend.admin.community.dto;

import com.example.kidhackbackend.admin.community.entity.MemberBan;
import com.example.kidhackbackend.users.member.entity.Member;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class MemberBanResponse {

    private final Long memberId;
    private final String loginId;
    private final boolean banned;
    private final String reason;
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
