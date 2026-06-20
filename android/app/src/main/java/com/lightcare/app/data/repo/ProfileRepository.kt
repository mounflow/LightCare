package com.lightcare.app.data.repo

import com.lightcare.app.data.api.BootstrapReq
import com.lightcare.app.data.api.CreateProfileReq
import com.lightcare.app.data.api.LightCareApi
import com.lightcare.app.data.api.ProfileDto
import com.lightcare.app.data.api.UpdatePhysiqueReq
import com.lightcare.app.data.auth.AuthStore
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 档案仓库（本地化：不依赖登录）。
 *
 * - [bootstrap]：首次进入时调用，后端建占位 user + 首份 SELF 档案，返回 userId+profile。
 *   成功后把 userId（=ownerUserId）、profileId、displayName 写入 [AuthStore]，后续请求自动带 X-LightCare-User-Id 头。
 * - [list]：拉取当前 userId 名下所有档案（用于"选/切档案"）。
 * - [select]：选中已有档案（写入 AuthStore）。
 * - [updatePhysique]：更新身体数据（身高/体重/年龄/性别/活动量），server 自动按 Mifflin-St Jeor 重算目标值。
 *
 * 为什么 userId 用 ownerUserId：后端 CurrentUserResolver 从 X-LightCare-User-Id 头读 userId，
 * 而 ProfileController.mustOwn 校验 ownerUserId==userId || managedByUserId==userId，
 * 所以把 ownerUserId 当 userId 存，校验天然成立。
 */
@Singleton
class ProfileRepository @Inject constructor(
    private val api: LightCareApi,
    private val authStore: AuthStore
) {
    /** 拉取当前登录 userId 名下的档案列表。 */
    suspend fun list(): List<ProfileDto>? = try {
        api.listProfiles().data
    } catch (e: Exception) {
        null
    }

    /**
     * 本地化建档：displayName 必填，physique（生日/性别/身高/体重/活动量）均可空。
     * 成功写入 AuthStore（profileId + userId=ownerUserId）。
     */
    suspend fun bootstrap(
        displayName: String,
        birthDate: String? = null,
        gender: String? = null,
        heightCm: Int? = null,
        weightKg: Double? = null,
        activityLevel: String? = null
    ): ProfileDto? {
        val res = try {
            api.bootstrap(
                BootstrapReq(
                    displayName = displayName.trim(),
                    birthDate = birthDate,
                    gender = gender,
                    heightCm = heightCm,
                    weightKg = weightKg,
                    activityLevel = activityLevel
                )
            ).data
        } catch (e: Exception) {
            null
        } ?: return null
        authStore.save(
            userId = res.userId,
            profileId = res.profile.id,
            nickname = res.profile.displayName,
            token = ""
        )
        return res.profile
    }

    /** 选中已有档案（切换当前档案）。写入 AuthStore。 */
    suspend fun select(profile: ProfileDto) {
        authStore.save(
            userId = profile.ownerUserId,
            profileId = profile.id,
            nickname = profile.displayName,
            token = ""
        )
    }

    /** 更新身体数据（任意字段为空则保持服务端原值），server 自动按 Mifflin-St Jeor 重算目标。 */
    suspend fun updatePhysique(
        profileId: Long,
        birthDate: String? = null,
        gender: String? = null,
        heightCm: Int? = null,
        weightKg: Double? = null,
        activityLevel: String? = null
    ): ProfileDto? = try {
        api.updatePhysique(
            profileId,
            UpdatePhysiqueReq(birthDate, gender, heightCm, weightKg, activityLevel)
        ).data
    } catch (e: Exception) {
        null
    }
}
