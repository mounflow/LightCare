package com.lightcare.app.ui.history

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.lightcare.app.di.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 异步加载某条 meal 的拍照图片（GET /v1/meals/{id}/image）。
 *
 * 用同一个 OkHttpClient（带 AuthInterceptor，自动注入 X-LightCare-User-Id 头）。
 * 返回解码后的 Bitmap（null = 404 / 解码失败 / 网络错）。
 * UI 用 produceState { value = loader.load(mealId) } 调用。
 */
@Singleton
class MealImageLoader @Inject constructor(
    private val client: OkHttpClient
) {
    suspend fun load(mealId: Long): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val url = "${NetworkModule.BASE_URL}v1/meals/$mealId/image"
            val req = Request.Builder().url(url).build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext null
                val bytes = resp.body?.bytes() ?: return@withContext null
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            }
        } catch (e: Exception) {
            null
        }
    }
}
