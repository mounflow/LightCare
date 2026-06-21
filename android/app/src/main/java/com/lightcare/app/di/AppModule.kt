package com.lightcare.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.lightcare.app.data.db.FoodItemDao
import com.lightcare.app.data.db.LightCareDatabase
import com.lightcare.app.data.db.MealCacheDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** PR1: 拍照即入列 V3 — 给 meal_cache 加 waterMl + recognitionStatus 两列。 */
val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE meal_cache ADD COLUMN waterMl INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE meal_cache ADD COLUMN recognitionStatus TEXT NOT NULL DEFAULT 'DONE'")
    }
}

/** PR-D: 食物库迁 server — 给 food_item 加 perServingWaterMl（之前缺，M3 水分会丢）。 */
val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE food_item ADD COLUMN perServingWaterMl INTEGER NOT NULL DEFAULT 0")
    }
}

/** V5: 详情页信息流卡片 — meal_cache 加 location + description。 */
val MIGRATION_4_5: Migration = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE meal_cache ADD COLUMN location TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE meal_cache ADD COLUMN description TEXT NOT NULL DEFAULT ''")
    }
}

/** V6: meal_cache 加 itemsJson（按食物分类染色）。 */
val MIGRATION_5_6: Migration = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE meal_cache ADD COLUMN itemsJson TEXT NOT NULL DEFAULT ''")
    }
}

/** V7: PR-Auth 账号系统升级 —— 旧缓存按 profileId 索引，未按 userId 隔离；跨账号会脏读。
 *  直接清空两张表，让用户登录后从 server 重新拉数据。 */
val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("DELETE FROM meal_cache")
        db.execSQL("DELETE FROM food_item")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun database(@ApplicationContext context: Context): LightCareDatabase =
        Room.databaseBuilder(context, LightCareDatabase::class.java, "lightcare.db")
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
            .fallbackToDestructiveMigration()  // 兜底：万一 migration 漏挂，丢旧缓存比崩好
            .build()

    @Provides
    fun mealCacheDao(db: LightCareDatabase): MealCacheDao = db.mealCacheDao()

    @Provides
    fun foodItemDao(db: LightCareDatabase): FoodItemDao = db.foodItemDao()
}