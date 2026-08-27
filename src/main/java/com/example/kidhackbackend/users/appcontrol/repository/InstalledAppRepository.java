package com.example.kidhackbackend.users.appcontrol.repository;

import com.example.kidhackbackend.users.appcontrol.entity.InstalledApp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstalledAppRepository extends JpaRepository<InstalledApp, Long> {
    List<InstalledApp> findByChildId(Long childId);
    Optional<InstalledApp> findByChildIdAndPackageName(Long childId, String packageName);
    void deleteByChildId(Long childId);
}