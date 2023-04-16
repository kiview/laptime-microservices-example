package com.racing.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LapTimeRepository extends JpaRepository<LapTimeEntity, Long> {
}
