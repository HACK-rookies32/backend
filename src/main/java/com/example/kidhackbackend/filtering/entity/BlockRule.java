package com.example.kidhackbackend.filtering.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부모가 등록한 차단 규칙(블랙리스트) — 역할표 No.6.
 *
 * <p>{@code pattern}이 자녀가 접속한 대상(앱 식별자/URL)에 포함되면 차단으로 판정한다.
 * (화이트리스트 없이 블랙리스트만 사용)
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "block_rule",
        indexes = @Index(name = "idx_block_rule_child", columnList = "childId")
)
public class BlockRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 규칙이 속한 자녀. */
    @Column(nullable = false)
    private Long childId;

    /** 차단 패턴(앱 식별자 또는 URL 키워드). */
    @Column(nullable = false)
    private String pattern;

    public BlockRule(Long childId, String pattern) {
        this.childId = childId;
        this.pattern = pattern;
    }
}
