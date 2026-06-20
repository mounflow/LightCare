package com.lightcare.app.data.db

import androidx.room.*

@Entity(tableName = "meal_cache")
data class MealCacheEntity(
    @PrimaryKey val id: Long,
    val profileId: Long,
    val slot: String,
    val portion: String,
    val source: String,
    val summary: String,
    val kcal: Int,
    val proteinG: Double,
    val fatG: Double,
    val carbG: Double,
    val fiberG: Double,
    val vegServings: Int,
    val waterMl: Int = 0,             // PR1: 本餐含水量（毫升），用于 Home 余量第 4 项
    val recognitionStatus: String = "DONE",  // PR1: DONE/PENDING/FAILED
    val location: String = "",        // 详情页信息流卡片用：地点（反地理编码，可空串）
    val description: String = "",     // 详情页用：关于这次美食的描述/笔记（可空串）
    /**
     * 一餐的食材明细（JSON 数组），用于"今日余量"按食物本身分类染色。
     * 元素形如 {"name":"鸡胸","category":"蛋白","weightG":150,"kcal":...}
     * 旧缓存可能为空串 → HomeViewModel 兜底走 slot 染色。
     */
    val itemsJson: String = "",
    val mealDate: String,
    val mealTime: String,
    val loggedAt: Long = System.currentTimeMillis()
)

@Dao
interface MealCacheDao {
    @Query("SELECT * FROM meal_cache WHERE profileId = :pid AND mealDate = :date ORDER BY mealTime")
    suspend fun listByDate(pid: Long, date: String): List<MealCacheEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<MealCacheEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: MealCacheEntity)

    @Query("DELETE FROM meal_cache WHERE id = :id")
    suspend fun delete(id: Long)

    /** A11 修复：清掉某 profile+date 的离线负 id 记录（server 已同步过来的正 id 即将写入）。 */
    @Query("DELETE FROM meal_cache WHERE profileId = :pid AND mealDate = :date AND id < 0")
    suspend fun deleteOfflineByDate(pid: Long, date: String)
}

/** 用户自定义食物缓存（server 是数据源，Room 降级为离线缓存 + 兜底）。 */
@Entity(tableName = "food_item")
data class FoodItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val key: String,            // 用户自定义 key 前缀 custom_ + 时间戳，避免与内置冲突
    val displayName: String,
    val category: String,
    val perServingProtein: Double,
    val perServingVeg: Int,
    val perServingKcal: Int,
    val perServingFat: Double,
    val perServingCarb: Double,
    val perServingWaterMl: Int = 0,   // PR-D: 对齐 server FoodDto（之前缺这列，M3 水分会丢）
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface FoodItemDao {
    @Query("SELECT * FROM food_item ORDER BY createdAt DESC")
    suspend fun all(): List<FoodItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FoodItemEntity): Long

    @Query("DELETE FROM food_item WHERE id = :id")
    suspend fun delete(id: Long)

    /** P45: 用 key 查一条（用于内置覆盖恢复、按 key 查同覆盖项） */
    @Query("SELECT * FROM food_item WHERE key = :key LIMIT 1")
    suspend fun byKey(key: String): FoodItemEntity?

    /** PR-D: 清空所有自定义缓存（server 列表成功后全量替换用）。 */
    @Query("DELETE FROM food_item")
    suspend fun clearAll()
}

@Database(
    entities = [MealCacheEntity::class, FoodItemEntity::class],
    version = 6,                   // V6: meal_cache 加 itemsJson（按食物分类染色用）
    exportSchema = false
)
abstract class LightCareDatabase : RoomDatabase() {
    abstract fun mealCacheDao(): MealCacheDao
    abstract fun foodItemDao(): FoodItemDao
}
