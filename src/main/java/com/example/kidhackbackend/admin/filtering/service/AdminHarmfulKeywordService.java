package com.example.kidhackbackend.admin.filtering.service;

import com.example.kidhackbackend.admin.filtering.dto.HarmfulKeywordRequest;
import com.example.kidhackbackend.admin.filtering.dto.HarmfulKeywordResponse;
import com.example.kidhackbackend.global.exception.DuplicateResourceException;
import com.example.kidhackbackend.global.exception.ResourceNotFoundException;
import com.example.kidhackbackend.users.filtering.entity.HarmfulKeyword;
import com.example.kidhackbackend.users.filtering.repository.HarmfulKeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 전역 차단 키워드 관리(관리자) — 등록한 키워드는 전체 사용자의 차단 판정에 적용된다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminHarmfulKeywordService {

    private final HarmfulKeywordRepository harmfulKeywordRepository;

    public List<HarmfulKeywordResponse> getKeywords() {
        return harmfulKeywordRepository.findAllByOrderByIdDesc().stream()
                .map(HarmfulKeywordResponse::from)
                .toList();
    }

    @Transactional
    public HarmfulKeywordResponse addKeyword(HarmfulKeywordRequest request) {
        String keyword = request.getKeyword().trim();

        if (harmfulKeywordRepository.existsByKeyword(keyword)) {
            throw new DuplicateResourceException("이미 등록된 키워드입니다.");
        }

        HarmfulKeyword saved = harmfulKeywordRepository.save(new HarmfulKeyword(keyword));
        return HarmfulKeywordResponse.from(saved);
    }

    @Transactional
    public void deleteKeyword(Long id) {
        HarmfulKeyword keyword = harmfulKeywordRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 키워드입니다."));

        harmfulKeywordRepository.delete(keyword);
    }
}
