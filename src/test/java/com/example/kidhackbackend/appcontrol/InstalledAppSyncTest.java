package com.example.kidhackbackend.appcontrol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.example.kidhackbackend.global.config.JpaAuditingConfig;
import com.example.kidhackbackend.users.appcontrol.dto.SyncRequest;
import com.example.kidhackbackend.users.appcontrol.entity.InstalledApp;
import com.example.kidhackbackend.users.appcontrol.repository.AppControlRuleRepository;
import com.example.kidhackbackend.users.appcontrol.repository.InstalledAppRepository;
import com.example.kidhackbackend.users.appcontrol.service.AppControlService;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * 설치 앱 동기화를 연속으로 부를 때.
 *
 * <p>{@code sync} 는 "해당 자녀의 기존 목록을 지우고 새로 넣는" 치환이다. 그런데 삭제가 파생 삭제라
 * 실제 DELETE 가 flush 시점까지 미뤄지고, Hibernate 는 INSERT 를 먼저 내보낸다. 그래서 옛 행이 남은
 * 채로 INSERT 가 들어가 {@code (child_id, package_name)} 유니크 제약에 걸렸다.
 *
 * <p>첫 동기화만 되고 <b>그 뒤로는 계속 500</b> 이었다. 앱 목록은 주기적으로 올리는 값이라
 * 사실상 항상 깨져 있었다.
 *
 * <p>모의 객체로는 이 순서를 못 잡으므로 실제 스키마를 쓰는 슬라이스 테스트로 둔다.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class InstalledAppSyncTest {

    private static final long CHILD_ID = 77L;

    @Autowired InstalledAppRepository installedAppRepository;
    @Autowired AppControlRuleRepository appControlRuleRepository;
    @Autowired EntityManager entityManager;

    private AppControlService service;

    @BeforeEach
    void setUp() {
        service = new AppControlService(installedAppRepository, appControlRuleRepository);
    }

    private SyncRequest req(String... packages) {
        List<SyncRequest.AppItem> items = java.util.Arrays.stream(packages)
                .map(p -> new SyncRequest.AppItem(p, p + " 앱"))
                .toList();
        return new SyncRequest(CHILD_ID, items);
    }

    @Test
    void 같은_목록으로_두_번_동기화해도_터지지_않는다() {
        assertThatCode(() -> service.sync(req("com.a", "com.b"))).doesNotThrowAnyException();
        entityManager.flush();
        entityManager.clear();

        // 앱 목록은 주기적으로 올라온다. 대개 직전과 같거나 겹친다.
        assertThatCode(() -> service.sync(req("com.a", "com.b"))).doesNotThrowAnyException();

        List<InstalledApp> saved = installedAppRepository.findByChildId(CHILD_ID);
        assertThat(saved).hasSize(2);
        assertThat(saved).extracting(InstalledApp::getPackageName)
                .containsExactlyInAnyOrder("com.a", "com.b");
    }

    @Test
    void 목록이_바뀌면_치환된다() {
        service.sync(req("com.a", "com.b"));
        entityManager.flush();
        entityManager.clear();

        service.sync(req("com.b", "com.c"));

        assertThat(installedAppRepository.findByChildId(CHILD_ID))
                .extracting(InstalledApp::getPackageName)
                .containsExactlyInAnyOrder("com.b", "com.c");
    }

    @Test
    void 요청_안에_같은_패키지가_중복이면_마지막_값이_남는다() {
        // [G-2] 로 이미 막아둔 것 — 회귀하지 않게 함께 고정한다.
        SyncRequest dup = new SyncRequest(CHILD_ID, List.of(
                new SyncRequest.AppItem("com.dup", "예전 이름"),
                new SyncRequest.AppItem("com.dup", "새 이름")));

        int count = service.sync(dup);

        assertThat(count).isEqualTo(1);
        assertThat(installedAppRepository.findByChildId(CHILD_ID))
                .singleElement()
                .extracting(InstalledApp::getAppName)
                .isEqualTo("새 이름");
    }

    @Test
    void 다른_자녀의_목록은_건드리지_않는다() {
        installedAppRepository.save(new InstalledApp(99L, "com.other", "남의 앱"));

        service.sync(req("com.a"));

        assertThat(installedAppRepository.findByChildId(99L))
                .singleElement()
                .extracting(InstalledApp::getPackageName)
                .isEqualTo("com.other");
    }
}
