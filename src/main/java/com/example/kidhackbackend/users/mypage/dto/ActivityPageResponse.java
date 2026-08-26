package com.example.kidhackbackend.users.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ActivityPageResponse {
    private List<ActivityResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}