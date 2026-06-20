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

@Singleton
class AuthStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyUserId = longPreferencesKey("user_id")
    private val keyProfileId = longPreferencesKey("profile_id")
    private val keyNickname = stringPreferencesKey("nickname")
    private val keyToken = stringPreferencesKey("token")

    val userIdFlow: Flow<Long?> = context.authDataStore.data.map { it[keyUserId] }
    val profileIdFlow: Flow<Long?> = context.authDataStore.data.map { it[keyProfileId] }
    val tokenFlow: Flow<String?> = context.authDataStore.data.map { it[keyToken] }

    suspend fun userId(): Long? = context.authDataStore.data.map { it[keyUserId] }.first()
    suspend fun profileId(): Long? = context.authDataStore.data.map { it[keyProfileId] }.first()

    suspend fun save(userId: Long, profileId: Long, nickname: String, token: String) {
        context.authDataStore.edit {
            it[keyUserId] = userId
            it[keyProfileId] = profileId
            it[keyNickname] = nickname
            it[keyToken] = token
        }
    }

    suspend fun clear() {
        context.authDataStore.edit { it.clear() }
    }

    /** 只清当前 profileId，保留 userId —— 用于切档后回到选档页能继续用同一 user 列出已有档案。 */
    suspend fun clearProfile() {
        context.authDataStore.edit {
            it.remove(keyProfileId)
            it.remove(keyNickname)
            it.remove(keyToken)
        }
    }
}
