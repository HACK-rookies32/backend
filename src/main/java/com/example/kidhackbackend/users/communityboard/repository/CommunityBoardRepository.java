package com.example.kidhackbackend.users.communityboard.repository;

import com.example.kidhackbackend.users.communityboard.entity.CommunityBoard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommunityBoardRepository extends JpaRepository<CommunityBoard, Long> {

    /**
     * 게시글 목록/검색 — 최신순(작성 시각 DESC). 키워드가 비어 있으면 전체.
     * 키워드는 <b>제목 또는 작성자 닉네임</b>에서 부분·대소문자 무시 매칭한다.
     *
     * <p>JPQL LIKE 바인딩이 Hibernate 7 + H2 조합에서 깨지는 이슈가 있어 네이티브 쿼리로 우회한다
     * (MemberRepository.searchCustomers 와 동일 패턴). Pageable 은 정렬 없이 넘긴다(ORDER BY 가 쿼리에 포함).
     *
     * <p>동점일 때 id 로 한 번 더 가른다. 시드 데이터처럼 여러 행이 같은 시각에 들어가면
     * created_at 하나로는 순서가 매번 달라져, 페이지를 넘길 때 글이 겹치거나 빠진다.
     */
    @Query(value = "SELECT p.* FROM community_post p JOIN member m ON m.id = p.member_id "
            + "WHERE (:kw = '' "
            + "   OR LOWER(p.title) LIKE LOWER(('%' || :kw || '%')) "
            + "   OR LOWER(m.nickname) LIKE LOWER(('%' || :kw || '%'))) "
            + "ORDER BY p.created_at DESC, p.id DESC",
            countQuery = "SELECT COUNT(*) FROM community_post p JOIN member m ON m.id = p.member_id "
                    + "WHERE (:kw = '' "
                    + "   OR LOWER(p.title) LIKE LOWER(('%' || :kw || '%')) "
                    + "   OR LOWER(m.nickname) LIKE LOWER(('%' || :kw || '%')))",
            nativeQuery = true)
    Page<CommunityBoard> searchLatest(@Param("kw") String kw, Pageable pageable);

    Page<CommunityBoard> findByMemberId(Long memberId, Pageable pageable);
}
