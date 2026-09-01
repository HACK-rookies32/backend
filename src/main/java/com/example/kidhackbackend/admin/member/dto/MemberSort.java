package com.example.kidhackbackend.admin.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Sort;

/**
 * 관리자 회원 목록 정렬 기준 — 웹 드롭다운이 그대로 쓰는 값(#173).
 *
 * <p>정렬을 지정하지 않으면 DB 가 반환 순서를 자유롭게 정한다. 데이터가 바뀌지 않는 한 매번 같은
 * 순서를 주므로 <b>특정 회원이 늘 같은 자리에 고정된 것처럼</b> 보이고(QA #8), {@code fetch first N rows}
 * 와 함께 쓰면 <b>페이지 경계가 요청마다 달라져</b> 어떤 회원은 중복되고 어떤 회원은 누락된다.
 *
 * <p><b>컬럼명을 쓰는 이유</b>: 대상 쿼리가 네이티브라(LIKE 바인딩 우회) {@code Sort} 의 프로퍼티가
 * 엔티티 필드명이 아니라 실제 컬럼명으로 나간다.
 *
 * <p><b>2차 키 {@code id}</b>: 1차 키가 같을 때 순서가 흔들리지 않게 고정한다. 페이징 커서 안정용이라
 * 값의 의미는 상관없고 매번 같기만 하면 된다.
 *
 * <p><b>{@code NULLS LAST}</b>: {@code created_at} 은 nullable 인데 정렬 시 NULL 위치가
 * Oracle 과 H2 에서 반대다. 명시하지 않으면 운영과 로컬의 순서가 갈린다.
 */
@Schema(description = "회원 목록 정렬 기준", enumAsRef = true)
public enum MemberSort {

    /** 가입 최신순 (기본값). */
    RECENT(Sort.by(
            new Sort.Order(Sort.Direction.DESC, "created_at", Sort.NullHandling.NULLS_LAST),
            Sort.Order.desc("id"))),

    /** 가입 오래된순. */
    OLDEST(Sort.by(
            new Sort.Order(Sort.Direction.ASC, "created_at", Sort.NullHandling.NULLS_LAST),
            Sort.Order.asc("id"))),

    /** 이름 가나다순. */
    NAME(Sort.by(Sort.Order.asc("username"), Sort.Order.desc("id"))),

    /** 이름 역순. */
    NAME_DESC(Sort.by(Sort.Order.desc("username"), Sort.Order.desc("id")));

    private final Sort sort;

    MemberSort(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        return sort;
    }
}
