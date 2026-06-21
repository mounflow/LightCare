package com.lightcare.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lightcare.app.data.auth.AuthRepository
import com.lightcare.app.data.db.LightCareDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * PR-Auth：设置页 VM。
 *
 * 退出登录 = 通知 server（best-effort）+ 清 AuthStore + 清 Room 缓存（meal / food_item）。
 * Room 缓存按 user 维度隔离，退出后旧数据对当前 user 已无意义，避免脏读。
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val db: LightCareDatabase
) : ViewModel() {

    /** 退出登录：清 server 端会话 + 本地账号 + 本地缓存。 */
    fun logout(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            authRepo.logout()
            db.clearAllTables()
            onDone()
        }
    }
}