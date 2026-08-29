package com.example.kidhackbackend.users.appcontrol.entity;

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
 * 자녀별 하루 총 스크린타임 한도.
 *
 * <p>앱 통제({@link AppControlRule})는 <b>앱별</b> 한도만 다룬다. "하루 전체 3시간" 같은
 * 총량 개념이 서버에 없어서, 모바일이 180분을 상수로 들고 모든 자녀에게 같은 값을 보여
 * 주고 있었다. 부모가 자녀마다 정할 수 있도록 여기에 둔다.
 *
 * <p>{@code dailyLimitMinutes} 가 {@code null} 이면 <b>제한 없음</b>이다. 0 은 "한도 0분"
 * 이라는 뜻이라 의미가 다르다 — 둘을 구분해야 해서 nullable 로 둔다.
 *
 * <p>한도 도달 판정은 서버가 하지 않는다. 사용량을 실측하는 자녀 앱이 판정해 보고하고
 * 서버는 알림만 발행한다({@code POST /api/appcontrol/screentime/{childId}/reached}).
 * 서버가 집계로 판정하려면 주기 배치가 필요하고, 그 집계는 자녀 앱이 올린 값에 의존하므로
 * 결국 같은 신뢰 수준이면서 지연만 늘어난다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "screen_time_setting")
public class ScreenTimeSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대상 자녀 (자녀당 1개). */
    @Column(nullable = false, unique = true)
    private Long childId;

    /** 하루 총 사용 한도(분). null 이면 제한 없음. */
    private Integer dailyLimitMinutes;

    public ScreenTimeSetting(Long childId, Integer dailyLimitMinutes) {
        this.childId = childId;
        this.dailyLimitMinutes = dailyLimitMinutes;
    }

    public void setDailyLimitMinutes(Integer dailyLimitMinutes) {
        this.dailyLimitMinutes = dailyLimitMinutes;
    }
}
