package com.example.kidhackbackend.users.appcontrol.entity;

import com.example.kidhackbackend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "app_control_rule",
        indexes = @Index(name = "idx_app_control_rule_child", columnList = "childId"),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_app_control_rule_child_pkg",
                columnNames = {"childId", "packageName"})
)
public class AppControlRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long childId;

    /** 통제 대상 앱 패키지명. */
    @Column(nullable = false)
    private String packageName;

    /** 사용자에게 보이는 앱 이름. */
    @Column(nullable = false)
    private String appName;

    /** 하루 사용 제한 시간(분). 0이면 완전 차단. */
    @Column(nullable = false)
    private int dailyLimitMinutes;

    /** 통제 활성화 여부. */
    @Column(nullable = false)
    private boolean enabled = true;

    public AppControlRule(Long childId, String packageName, String appName, int dailyLimitMinutes) {
        this.childId = childId;
        this.packageName = packageName;
        this.appName = appName;
        this.dailyLimitMinutes = dailyLimitMinutes;
    }

    public void update(Integer dailyLimitMinutes, Boolean enabled) {
        if (dailyLimitMinutes != null) this.dailyLimitMinutes = dailyLimitMinutes;
        if (enabled != null) this.enabled = enabled;
    }
}