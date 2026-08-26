package com.example.kidhackbackend.users.filtering.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자가 등록한 전역 차단 키워드.
 *
 * <p>{@link BlockRule}이 자녀별 블랙리스트인 것과 달리, 이 키워드는 전체 사용자에게 적용된다.
 * 판정은 {@code BlockRuleService.isBlocked()}에서 자녀별 규칙과 함께 수행한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "harmful_keyword")
public class HarmfulKeyword extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 차단 키워드 또는 URL 조각. */
    @Column(nullable = false, unique = true)
    private String keyword;

    public HarmfulKeyword(String keyword) {
        this.keyword = keyword;
    }
}
