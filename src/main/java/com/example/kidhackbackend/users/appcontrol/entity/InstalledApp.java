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
        name = "installed_app",
        indexes = @Index(name = "idx_installed_app_child", columnList = "childId"),
        uniqueConstraints = @UniqueConstraint(
                name = "uk_installed_app_child_pkg",
                columnNames = {"childId", "packageName"})
)
public class InstalledApp extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long childId;

    /** 앱 패키지명 (예: com.google.youtube). */
    @Column(nullable = false)
    private String packageName;

    /** 사용자에게 보이는 앱 이름 (예: YouTube). */
    @Column(nullable = false)
    private String appName;

    public InstalledApp(Long childId, String packageName, String appName) {
        this.childId = childId;
        this.packageName = packageName;
        this.appName = appName;
    }

    public void updateAppName(String appName) {
        this.appName = appName;
    }
}