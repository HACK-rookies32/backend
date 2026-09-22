package com.example.kidhackbackend.global.storage;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DbFileRepository extends JpaRepository<DbFile, Long> {
}
