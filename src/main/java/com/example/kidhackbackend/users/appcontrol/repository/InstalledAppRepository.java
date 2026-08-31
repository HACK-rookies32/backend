package com.example.kidhackbackend.users.appcontrol.repository;

import com.example.kidhackbackend.users.appcontrol.entity.InstalledApp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InstalledAppRepository extends JpaRepository<InstalledApp, Long> {
    List<InstalledApp> findByChildId(Long childId);
    Optional<InstalledApp> findByChildIdAndPackageName(Long childId, String packageName);
    /**
     * 자녀의 설치앱 목록을 통째로 지운다. 동기화(치환) 직전에 부른다.
     *
     * <p><b>파생 삭제로 두면 안 된다.</b> 파생 삭제는 엔티티를 읽어 하나씩 remove 하고 실제 DELETE 는
     * flush 시점에 나가는데, Hibernate 는 INSERT 를 먼저 내보낸다. 그래서 옛 행이 남은 채로 새 행이
     * 들어가 {@code (child_id, package_name)} 유니크 제약에 걸렸다 — 두 번째 동기화부터 500 이 났다.
     * 벌크 DML 은 즉시 실행돼 삭제가 먼저인 것이 보장된다.
     * ({@code CommunityBoardLikeRepository.deleteByBoardId} 와 같은 사정)
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from InstalledApp a where a.childId = :childId")
    int deleteByChildId(@Param("childId") Long childId);
}