package com.racing.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class LapTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    String driver;

    String track;

    long time;

    public LapTimeEntity() {
    }

    public LapTimeEntity(String driver, String track, long time) {
        this.driver = driver;
        this.track = track;
        this.time = time;
    }

}
