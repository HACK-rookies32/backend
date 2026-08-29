package com.example.kidhackbackend.users.appcontrol.repository;

import com.example.kidhackbackend.users.appcontrol.entity.ScreenTimeSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScreenTimeSettingRepository extends JpaRepository<ScreenTimeSetting, Long> {

    Optional<ScreenTimeSetting> findByChildId(Long childId);
}
