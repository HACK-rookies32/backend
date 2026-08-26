package com.example.kidhackbackend.users.device.repository;

import com.example.kidhackbackend.users.device.entity.DeviceEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceEventRepository extends JpaRepository<DeviceEvent, Long> {

    List<DeviceEvent> findByChildIdOrderByCreatedAtDesc(Long childId);
}
