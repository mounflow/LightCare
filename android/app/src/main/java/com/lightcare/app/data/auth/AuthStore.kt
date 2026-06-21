package com.lightcare.app.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore by preferencesDataStore(name = "auth")

/**
 * 账号持久化：手机号 + token + userId + 当前选中的 profileId/nickname。
 *
 * 设计：
 *  - keyToken 升级为正式 Bearer token（之前为预留字段，从未填值）。
 *  - keyPhone 增：登录页回填、退出登录时清空。
 *  - clear() 清一切（退出登录入口）；clearProfile() 只清 profile（切档）。
 */
@Singleton
class AuthStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyUserId = longPreferencesKey("user_id")
    private val keyProfileId = longPreferencesKey("profile_id")
    private val keyNickname = stringPreferencesKey("nickname")
    private val keyToken = stringPreferencesKey("token")
    private val keyPhone = stringPreferencesKey("phone")

    val userIdFlow: Flow<Long?> = context.authDataStore.data.map { it[keyUserId] }
    val profileIdFlow: Flow<Long?> = context.authDataStore.data.map { it[keyProfileId] }
    val tokenFlow: Flow<String?> = context.authDataStore.data.map { it[keyToken] }
    val phoneFlow: Flow<String?> = context.authDataStore.data.map { it[keyPhone] }
    val nicknameFlow: Flow<String?> = context.authDataStore.data.map { it[keyNickname] }

    suspend fun userId(): Long? = context.authDataStore.data.map { it[keyUserId] }.first()
    suspend fun profileId(): Long? = context.authDataStore.data.map { it[keyProfileId] }.first()
    suspend fun token(): String? = context.authDataStore.data.map { it[keyToken] }.first()
    suspend fun phone(): String? = context.authDataStore.data.map { it[keyPhone] }.first()
    suspend fun nickname(): String? = context.authDataStore.data.map { it[keyNickname] }.first()

    /** 登录成功后调用：写入 userId + token + phone + nickname。profileId 走 saveProfile 单独写。 */
    suspend fun saveSession(userId: Long, phone: String, nickname: String, token: String) {
        context.authDataStore.edit {
            it[keyUserId] = userId
            it[keyPhone] = phone
            it[keyNickname] = nickname
            it[keyToken] = token
        }
    }

    /**
     * 兼容 bootstrap 旧路径（无鉴权占位 user）的一次性写入：userId + profileId + nickname + 空 token。
     * token 为空 → AuthInterceptor 走 X-LightCare-User-Id fallback，与 server CurrentUserResolver fallback 对齐。
     * 正常 PR-Auth 流程走 [saveSession] + [saveProfile]。
     */
    suspend fun save(userId: Long, profileId: Long, nickname: String, token: String) {
        context.authDataStore.edit {
            it[keyUserId] = userId
            it[keyProfileId] = profileId
            it[keyNickname] = nickname
            it[keyToken] = token
        }
    }

    /** 注册成功后由 AuthRepository.saveSessionAndProfile 一次性写完。 */
    suspend fun saveProfile(profileId: Long, nickname: String?) {
        context.authDataStore.edit {
            it[keyProfileId] = profileId
            if (nickname != null) it[keyNickname] = nickname
        }
    }

    /** 退出登录：清一切（userId/token/phone/profile/nickname）。 */
    suspend fun clear() {
        context.authDataStore.edit { it.clear() }
    }

    /** 切档：保留 userId/token/phone，只清当前 profile。 */
    suspend fun clearProfile() {
        context.authDataStore.edit {
            it.remove(keyProfileId)
            it.remove(keyNickname)
        }
    }
}