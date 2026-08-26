package com.example.kidhackbackend.users.location.repository;

import com.example.kidhackbackend.users.location.entity.TrackingSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrackingSettingRepository extends JpaRepository<TrackingSetting, Long> {

    Optional<TrackingSetting> findByChildId(Long childId);
}
