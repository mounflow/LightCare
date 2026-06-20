package com.lightcare.app.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * 拿当前地点名（反地理编码到「行政区 + 街道」级短文本）。
 *
 * 不引 Google Play Services：用原生 LocationManager（系统自带）+ Geocoder（系统服务反查地名）。
 * 权限：ACCESS_COARSE_LOCATION 或 ACCESS_FINE_LOCATION 任一即可。调用方需先做运行时申请。
 *
 * 失败（无权限 / 定位超时 / Geocoder 无结果）→ 返回 null（调用方显示「未记录」）。
 * 整体超时 3s（拍照体验优先，拿不到就空着）。
 */
@Singleton
class LocationFetcher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @SuppressLint("MissingPermission")
    suspend fun currentPlace(): String? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) return@withContext null
        val loc = lastKnownLocation() ?: return@withContext null
        reverseGeocode(loc.latitude, loc.longitude)
    }

    private fun hasLocationPermission(): Boolean {
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }

    @SuppressLint("MissingPermission")
    private suspend fun lastKnownLocation(): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        // 优先 NETWORK（室内/省电），fallback GPS
        val provider = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            .firstOrNull { lm.isProviderEnabled(it) } ?: return null
        // P2-15: getLastKnownLocation 是同步的，直接返回（之前包 withTimeoutOrNull + suspendCancellableCoroutine
        // 但协程体立即 resume，超时永远用不上，是死代码）。可能为 null（首次无缓存），调用方处理。
        return try { lm.getLastKnownLocation(provider) } catch (e: SecurityException) { null }
    }

    /**
     * 反查地名：优先用高德 Web 服务（国内可靠），没配 key 时 fallback 系统 Geocoder（国内可能返回空）。
     * 取「市 + 区」短文本，如「北京市海淀区」。
     */
    private suspend fun reverseGeocode(lat: Double, lng: Double): String? {
        val amapKey = com.lightcare.app.BuildConfig.AMAP_KEY
        if (!amapKey.isNullOrBlank()) {
            val amap = amapReverse(lat, lng, amapKey)
            if (amap != null) return amap
        }
        return systemGeocoderReverse(lat, lng)
    }

    /** 高德逆地理编码：restapi.amap.com/v3/geocode/regeo。返回「市+区」或「区」。 */
    private suspend fun amapReverse(lat: Double, lng: Double, key: String): String? = try {
        // 高德要求 location=经度,纬度（lng,lat 顺序，注意）
        val url = "https://restapi.amap.com/v3/geocode/regeo?location=$lng,$lat&key=$key&extensions=base"
        val req = Request.Builder().url(url).build()
        OkHttpClient().newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@use null
            val body = resp.body?.string() ?: return@use null
            // 简单解析（不引 JSON 库）：抽 city / district。高德返回形如 "city":"北京市","district":"海淀区"
            val city = extractJsonField(body, "city")?.takeIf { it.isNotEmpty() && it != "[]" }
            val district = extractJsonField(body, "district")
            listOfNotNull(city, district).filter { it.isNotBlank() }.joinToString("")
                .ifBlank { null }
        }
    } catch (e: Exception) { null }

    /** 系统 Geocoder 反查（fallback，国内多数手机无 Google 服务会返回空）。 */
    private suspend fun systemGeocoderReverse(lat: Double, lng: Double): String? = try {
        val geocoder = Geocoder(context, Locale.CHINA)
        val addresses = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine<List<android.location.Address>> { cont ->
                geocoder.getFromLocation(lat, lng, 1) { addresses ->
                    Looper.prepareMainLooper()
                    cont.resume(addresses)
                }
            }
        } else {
            @Suppress("DEPRECATION")
            geocoder.getFromLocation(lat, lng, 1).orEmpty()
        }
        addresses.firstOrNull()?.let { a ->
            val parts = listOfNotNull(a.locality, a.subLocality).filter { it.isNotBlank() }
            if (parts.isNotEmpty()) parts.joinToString("") else a.featureName
        }
    } catch (e: Exception) { null }

    /** 从 JSON 字符串里粗暴抽 "field":"value" 的 value（避免引 Moshi）。 */
    private fun extractJsonField(json: String, field: String): String? {
        val key = "\"$field\":\""
        val start = json.indexOf(key)
        if (start < 0) return null
        val valueStart = start + key.length
        val valueEnd = json.indexOf('"', valueStart)
        return if (valueEnd > valueStart) json.substring(valueStart, valueEnd) else null
    }
}
