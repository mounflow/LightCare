package com.lightcare.app.data.recipe

import android.util.Log
import com.lightcare.app.data.api.LightCareApi
import com.lightcare.app.data.api.RecipeDto
import com.lightcare.app.data.api.UpsertRecipeReq
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 食物做法（PR-Recipe）仓库。
 *
 * 约定：
 * - `getRecipe(foodId)` 拿到 null = "无做法"（server 404 也算无做法）。
 * - `upsertRecipe(...)` 返回 sealed [UpsertResult]，让 UI 区分成功 / 鉴权失败 / 网络错误。
 *
 * 全程不缓存到本地：做法低频、实时性高，不值得引入 Room 复杂度。
 */
@Singleton
class RecipeRepository @Inject constructor(
    private val api: LightCareApi
) {
    private val tag = "RecipeRepo"

    sealed class UpsertResult {
        data class Success(val recipe: RecipeDto) : UpsertResult()
        /** 种子食物不可改（server 403）。UI 提示用户换一道菜。 */
        object Forbidden : UpsertResult()
        /** 其它错误（断网 / 500）。 */
        data class Error(val message: String) : UpsertResult()
    }

    /**
     * 拉一份做法。404 当作 null（无做法）；其它错误抛给上层。
     */
    suspend fun getRecipe(foodId: Long): RecipeDto? = try {
        val resp = api.getRecipe(foodId)
        resp.data
    } catch (e: HttpException) {
        if (e.code() == 404) null
        else {
            Log.w(tag, "getRecipe failed: http ${e.code()}", e)
            throw e
        }
    } catch (e: Exception) {
        Log.w(tag, "getRecipe failed: ${e.javaClass.simpleName}", e)
        throw e
    }

    /**
     * upsert。null 字段按 server "不修改" 语义；空 list 当作 "清空"。
     */
    suspend fun upsertRecipe(foodId: Long, req: UpsertRecipeReq): UpsertResult = try {
        val resp = api.upsertRecipe(foodId, req)
        val dto = resp.data
        if (dto != null) UpsertResult.Success(dto)
        else UpsertResult.Error("服务器未返回做法数据")
    } catch (e: HttpException) {
        when (e.code()) {
            403 -> UpsertResult.Forbidden
            else -> UpsertResult.Error("HTTP ${e.code()}: ${e.message()}")
        }
    } catch (e: Exception) {
        Log.w(tag, "upsertRecipe failed", e)
        UpsertResult.Error(e.message ?: "保存失败")
    }
}