package com.lightcare.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lightcare.app.ui.theme.D
import com.lightcare.app.ui.theme.LCPrimaryButton
import com.lightcare.app.ui.theme.LCSecondaryButton
import com.lightcare.app.ui.theme.OnPrimaryContainer
import com.lightcare.app.ui.theme.Outline
import com.lightcare.app.ui.theme.Primary
import com.lightcare.app.ui.theme.PrimaryContainer
import com.lightcare.app.ui.theme.S

/**
 * PR-Auth：注册屏（升级版 v2）。
 *
 * 设计调整：
 *   - **去掉身高 / 体重选填**——进入后到"我的身体数据"里填，server 会按 Mifflin-St Jeor 自动重算。
 *   - 顶部 hero：🌱 + 一句话隐私说明（"数据存云端，本机切换账号也能继续看到"）。
 *   - 输入字段加 leading icon（📱/🔒/👤）+ 密码可见切换按钮。
 *   - 底部"已有账号，去登录"次按钮与"注册并开始"主按钮分离。
 *
 * 注册成功 → server 自动建默认 SELF profile → MainActivity 直接进主页。
 */
@Composable
fun RegisterScreen(
    onAuthed: () -> Unit,
    onBack: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.success) {
        if (state.success) onAuthed()
    }

    LaunchedEffect(Unit) { vm.setMode(AuthViewModel.Mode.REGISTER) }

    AuthScaffold(
        title = "创建账号",
        subtitle = "30 秒搞定，数据全在云端",
        onBack = onBack
    ) {
        // 隐私说明卡片（hero 下方，呼应"上云"卖点）
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = S.xxl, vertical = S.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(PrimaryContainer, RoundedCornerShape(D.radiusMd))
                    .padding(horizontal = S.lg, vertical = S.md)
            ) {
                Text(
                    "📱 你的数据",
                    style = MaterialTheme.typography.labelLarge,
                    color = Primary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(S.xxs))
                Text(
                    "手机号 + 密码登录，饮食/作息记录全部存云端。本机切换账号也能继续看到。",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnPrimaryContainer
                )
            }
        }

        AuthCard {
            AuthField(
                label = "手机号",
                value = state.phone,
                onChange = vm::updatePhone,
                keyboard = KeyboardType.Phone,
                imeAction = ImeAction.Next,
                placeholder = "11 位手机号",
                leadingIcon = Icons.Outlined.Phone
            )
            Spacer(Modifier.height(S.md))
            AuthField(
                label = "设置密码",
                value = state.password,
                onChange = vm::updatePassword,
                keyboard = KeyboardType.Password,
                imeAction = ImeAction.Next,
                placeholder = "至少 6 位",
                isPassword = true,
                passwordVisible = state.passwordVisible,
                onTogglePasswordVisible = vm::togglePasswordVisible,
                leadingIcon = Icons.Outlined.Lock
            )
            Spacer(Modifier.height(S.md))
            AuthField(
                label = "你的称呼",
                value = state.displayName,
                onChange = vm::updateName,
                keyboard = KeyboardType.Text,
                imeAction = ImeAction.Done,
                placeholder = "如：我、爸爸（用于档案显示）",
                leadingIcon = Icons.Outlined.Person
            )
            Spacer(Modifier.height(S.lg))
            LCPrimaryButton(
                text = "创建账号",
                onClick = vm::submit,
                loading = state.loading,
                enabled = !state.loading,
                leadingEmoji = "✨"
            )
            Spacer(Modifier.height(S.sm))
            LCSecondaryButton(
                text = "已有账号，去登录",
                onClick = onBack
            )
        }
        if (state.error != null) {
            Spacer(Modifier.height(S.md))
            Text(
                state.error!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}