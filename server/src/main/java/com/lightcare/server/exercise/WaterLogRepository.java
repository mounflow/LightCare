package com.lightcare.server.exercise;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface WaterLogRepository extends JpaRepository<WaterLogEntity, Long> {

    @Query("select coalesce(sum(w.cups), 0) from WaterLogEntity w where w.profileId = :pid and w.logDate = :d")
    int sumCupsByDate(@Param("pid") Long profileId, @Param("d") LocalDate date);
}
