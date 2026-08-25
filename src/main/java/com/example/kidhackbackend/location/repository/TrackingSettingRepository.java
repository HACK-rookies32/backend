package com.example.kidhackbackend.location.repository;

import com.example.kidhackbackend.location.entity.TrackingSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrackingSettingRepository extends JpaRepository<TrackingSetting, Long> {

    Optional<TrackingSetting> findByChildId(Long childId);
}
