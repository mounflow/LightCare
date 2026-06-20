package com.lightcare.server.exercise;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ExerciseRepository extends JpaRepository<ExerciseEntity, Long> {

    @Query("select e from ExerciseEntity e where e.profileId = :pid and e.exerciseDate >= :from order by e.exerciseDate desc")
    List<ExerciseEntity> findRecent(@Param("pid") Long profileId, @Param("from") LocalDate from);

    @Query("select coalesce(sum(e.durationMin), 0) from ExerciseEntity e where e.profileId = :pid and e.exerciseDate = :d")
    int sumDurationByDate(@Param("pid") Long profileId, @Param("d") LocalDate date);
}
