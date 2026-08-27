package com.example.kidhackbackend.users.screenshot.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * 캡처 목록 페이징 응답. 공통 페이징 규약(3.5)을 따른다.
 */
@Getter
@AllArgsConstructor
public class ScreenshotPageResponse {

    private List<ScreenshotResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}
