package com.example.kidhackbackend.users.communityboard.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PostPageResponse {
    private List<PostResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
}