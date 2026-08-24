package com.example.kidhackbackend.location.repository;

import com.example.kidhackbackend.location.entity.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaceRepository extends JpaRepository<Place, Long> {

    /** 자녀에 등록된 모든 공간. */
    List<Place> findByChildId(Long childId);
}
