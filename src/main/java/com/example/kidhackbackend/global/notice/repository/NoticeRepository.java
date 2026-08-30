package com.example.kidhackbackend.global.notice.repository;

import com.example.kidhackbackend.global.notice.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NoticeRepository extends JpaRepository<Notice, Long> {

    Page<Notice> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * 제목 키워드 검색 — 최신순(id DESC). 키워드가 비어 있으면 전체.
     *
     * <p>JPQL LIKE 바인딩이 Hibernate 7 + H2 조합에서 깨지는 이슈가 있어 네이티브 쿼리로 우회한다
     * (CommunityBoardRepository.searchLatest 와 동일 패턴). 본문(content)은 CLOB 이라 DB 별 LIKE 지원이
     * 갈려서 제외하고 제목만 검색한다.
     */
    @Query(value = "SELECT * FROM notice "
            + "WHERE (:kw = '' OR LOWER(title) LIKE LOWER(('%' || :kw || '%'))) "
            + "ORDER BY id DESC",
            countQuery = "SELECT COUNT(*) FROM notice "
                    + "WHERE (:kw = '' OR LOWER(title) LIKE LOWER(('%' || :kw || '%')))",
            nativeQuery = true)
    Page<Notice> searchByTitle(@Param("kw") String kw, Pageable pageable);
}
