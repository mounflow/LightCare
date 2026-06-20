package com.lightcare.server.meal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface MealRepository extends JpaRepository<MealEntity, Long> {

    List<MealEntity> findByProfileIdAndMealDateOrderByMealTime(Long profileId, LocalDate date);

    @Query("select m from MealEntity m where m.profileId = :pid and m.mealDate >= :from order by m.mealDate desc, m.mealTime desc")
    List<MealEntity> findRecent(@Param("pid") Long profileId, @Param("from") LocalDate from);

    /** P38: 周/月视图一次拿范围（按日期升序，按时间升序），App 本地按日分组算柱高 */
    @Query("select m from MealEntity m where m.profileId = :pid and m.mealDate >= :from and m.mealDate <= :to order by m.mealDate asc, m.mealTime asc")
    java.util.List<MealEntity> findByProfileIdAndDateRange(@Param("pid") Long profileId,
                                                       @Param("from") LocalDate from,
                                                       @Param("to") LocalDate to);

    @Query("select coalesce(sum(m.proteinG), 0) from MealEntity m where m.profileId = :pid and m.mealDate = :d")
    double sumProteinByDate(@Param("pid") Long profileId, @Param("d") LocalDate date);

    @Query("select coalesce(sum(m.vegServings), 0) from MealEntity m where m.profileId = :pid and m.mealDate = :d")
    int sumVegServingsByDate(@Param("pid") Long profileId, @Param("d") LocalDate date);

    @Query("select coalesce(sum(m.kcal), 0) from MealEntity m where m.profileId = :pid and m.mealDate = :d")
    int sumKcalByDate(@Param("pid") Long profileId, @Param("d") LocalDate date);

    @Query("select coalesce(sum(m.waterMl), 0) from MealEntity m where m.profileId = :pid and m.mealDate = :d")
    int sumWaterMlByDate(@Param("pid") Long profileId, @Param("d") LocalDate date);
}
