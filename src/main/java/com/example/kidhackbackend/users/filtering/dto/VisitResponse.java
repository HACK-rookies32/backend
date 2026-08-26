package com.example.kidhackbackend.users.filtering.dto;

/** 접속 감지 결과 (No.5). */
public record VisitResponse(
        Long childId,
        String target,
        boolean blocked
) {
}
