package com.lightcare.app.data.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.*

interface LightCareApi {
    @GET("/v1/profiles")
    suspend fun listProfiles(): ApiResponse<List<ProfileDto>>

    @POST("/v1/profiles")
    suspend fun createProfile(@Body req: CreateProfileReq): ApiResponse<ProfileDto>

    /** P28：更新身体数据（身高/体重/年龄/性别/活动量），触发 Mifflin-St Jeor 重算目标值 */
    @PATCH("/v1/profiles/{id}/physique")
    suspend fun updatePhysique(@Path("id") id: Long, @Body req: UpdatePhysiqueReq): ApiResponse<ProfileDto>

    /** 本地化建档（无鉴权）：本地运行无需登录，App 首次进入调用。 */
    @POST("/v1/profiles/bootstrap")
    suspend fun bootstrap(@Body req: BootstrapReq): ApiResponse<BootstrapRes>

    @GET("/v1/meals")
    suspend fun listMeals(
        @Query("profileId") profileId: Long,
        @Query("date") date: String
    ): ApiResponse<List<MealDto>>

    /** P38: 周/月视图一次拉范围（替代 N 次连续 GET） */
    @GET("/v1/meals/range")
    suspend fun listMealsRange(
        @Query("profileId") profileId: Long,
        @Query("from") from: String,
        @Query("to") to: String
    ): ApiResponse<List<MealDto>>

    @POST("/v1/meals")
    suspend fun createMeal(@Body req: CreateMealReq): ApiResponse<MealDto>

    /** PR3: 拍照即入列。multipart 一次走完：image 文件 + previewItems JSON + 表单字段。 */
    @Multipart
    @POST("/v1/meals/photo")
    suspend fun createMealFromPhoto(
        @Part("profileId") profileId: RequestBody,
        @Part("slot") slot: RequestBody,
        @Part("portion") portion: RequestBody,
        @Part("summary") summary: RequestBody,
        @Part("location") location: RequestBody,
        @Part("description") description: RequestBody,
        @Part("previewItems") previewItems: RequestBody,
        @Part image: MultipartBody.Part
    ): ApiResponse<MealDto>

    /** PR3: 单条查询，给客户端轮询识别状态。 */
    @GET("/v1/meals/{id}")
    suspend fun getMeal(@Path("id") id: Long): ApiResponse<MealDto>

    @DELETE("/v1/meals/{id}")
    suspend fun deleteMeal(@Path("id") id: Long): ApiResponse<Unit>

    @POST("/v1/exercises")
    suspend fun createExercise(@Body req: CreateExerciseReq): ApiResponse<Unit>

    @POST("/v1/exercises/water")
    suspend fun addWater(@Body req: AddWaterReq): ApiResponse<Unit>

    @GET("/v1/recommend/today")
    suspend fun todayRecommend(@Query("profileId") profileId: Long): ApiResponse<List<RecommendCardDto>>

    @POST("/v1/recommend/exercise")
    suspend fun exerciseRecommend(@Body req: ExerciseRecReq): ApiResponse<List<RecommendCardDto>>

    @POST("/v1/recommend/{id}/accept")
    suspend fun acceptRecommend(@Path("id") id: Long): ApiResponse<Unit>

    @POST("/v1/recommend/{id}/skip")
    suspend fun skipRecommend(@Path("id") id: Long): ApiResponse<Unit>

    @GET("/v1/reports/weekly")
    suspend fun weeklyReport(@Query("profileId") profileId: Long): ApiResponse<WeeklyReportDto>

    /** P6 拍照识别：上传图片 → server 调智谱 GLM-4V → 返回候选食物。 */
    @Multipart
    @POST("/v1/recognize")
    suspend fun recognize(@Part image: MultipartBody.Part): ApiResponse<List<RecognizedItem>>

    // ===== PR-D: 食物库（server 统一管理） =====

    /** 列表：自己的 + 全局种子。 */
    @GET("/v1/foods")
    suspend fun listFoods(): ApiResponse<List<FoodDto>>

    /** 搜索：displayName/category 包含 q。 */
    @GET("/v1/foods/search")
    suspend fun searchFoods(
        @Query("q") q: String,
        @Query("limit") limit: Int
    ): ApiResponse<List<FoodDto>>

    /** 新增。命中重名（四项营养全等）→ code=40901 + data=现有项。 */
    @POST("/v1/foods")
    suspend fun createFood(@Body req: UpsertFoodReq): ApiResponse<FoodDto>

    /** 更新（只能改自己的，种子不可改）。 */
    @PUT("/v1/foods/{id}")
    suspend fun updateFood(
        @Path("id") id: Long,
        @Body req: UpsertFoodReq
    ): ApiResponse<FoodDto>

    /** 删除（只能删自己的，种子不可删）。 */
    @DELETE("/v1/foods/{id}")
    suspend fun deleteFood(@Path("id") id: Long): ApiResponse<Unit>

    /** 待处理冲突列表（拍照自动入库命中"同名不同营养"时轮询）。 */
    @GET("/v1/foods/conflicts")
    suspend fun listFoodConflicts(): ApiResponse<List<FoodDto>>

    /** 解决冲突：RENAME（换名）/OVERWRITE（覆盖同名）/SKIP（不入库）。 */
    @POST("/v1/foods/{id}/resolve")
    suspend fun resolveFood(
        @Path("id") id: Long,
        @Query("action") action: String,
        @Query("newName") newName: String?
    ): ApiResponse<Unit>
}
