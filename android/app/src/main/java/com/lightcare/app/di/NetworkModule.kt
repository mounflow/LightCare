package com.lightcare.app.di

import com.lightcare.app.BuildConfig
import com.lightcare.app.data.api.AuthInterceptor
import com.lightcare.app.data.api.LightCareApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /** server base URL（供 UI 拼图片等裸资源 URL，如 /v1/meals/{id}/image）。 */
    const val BASE_URL = "http://192.168.1.24:8080/"    // 真机：本机局域网 IP；模拟器请改回 10.0.2.2

    @Provides @Singleton
    fun moshi(): Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides @Singleton
    fun okHttp(authInterceptor: AuthInterceptor): OkHttpClient {
        val logger = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.BASIC
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logger)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)   // 拍照识别：M3 cold start 偶发 13-19s，给 60s 余量
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)   // 整体调用上限（含重试/上传）
            .build()
    }

    @Provides @Singleton
    fun retrofit(client: OkHttpClient, moshi: Moshi): Retrofit = Retrofit.Builder()
        .baseUrl("http://192.168.1.24:8080/")    // 真机：本机局域网 IP；模拟器请改回 10.0.2.2
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides @Singleton
    fun api(retrofit: Retrofit): LightCareApi = retrofit.create(LightCareApi::class.java)
}
