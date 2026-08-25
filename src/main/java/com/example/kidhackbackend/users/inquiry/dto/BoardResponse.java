package com.example.kidhackbackend.users.inquiry.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BoardResponse {
    private Long id;
    private Long memberId;
    private String memberNickname;
    private String title;
    private boolean answered;
    private LocalDateTime createdAt;
}