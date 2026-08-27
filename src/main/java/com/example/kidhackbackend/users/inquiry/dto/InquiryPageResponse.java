package com.example.kidhackbackend.users.inquiry.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class InquiryPageResponse {
    private List<BoardResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}