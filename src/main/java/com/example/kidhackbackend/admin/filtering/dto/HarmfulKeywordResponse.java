package com.example.kidhackbackend.admin.filtering.dto;

import com.example.kidhackbackend.users.filtering.entity.HarmfulKeyword;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class HarmfulKeywordResponse {

    private final Long id;
    private final String keyword;
    private final LocalDateTime createdAt;

    public static HarmfulKeywordResponse from(HarmfulKeyword k) {
        return new HarmfulKeywordResponse(k.getId(), k.getKeyword(), k.getCreatedAt());
    }
}
