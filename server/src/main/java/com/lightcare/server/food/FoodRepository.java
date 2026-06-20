package com.lightcare.server.food;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FoodRepository extends JpaRepository<FoodEntity, Long> {

    /**
     * 用户可见食物 = 自己的 + 全局种子。种子优先（is_default desc）+ 按更新时间倒序。
     */
    @Query("select f from FoodEntity f where f.ownerUserId = :uid or f.ownerUserId is null " +
           "order by f.isDefault desc, f.updatedAt desc")
    List<FoodEntity> findVisible(@Param("uid") Long uid);

    /**
     * 同名查询（重名判定用）：自己的 + 全局种子中 display_name 忽略大小写相等。
     */
    @Query("select f from FoodEntity f where (f.ownerUserId = :uid or f.ownerUserId is null) " +
           "and lower(f.displayName) = lower(:name)")
    List<FoodEntity> findByNameIgnoreCase(@Param("uid") Long uid, @Param("name") String name);

    List<FoodEntity> findByOwnerUserIdAndConflictStatus(Long ownerUserId, FoodEntity.ConflictStatus conflictStatus);
}
