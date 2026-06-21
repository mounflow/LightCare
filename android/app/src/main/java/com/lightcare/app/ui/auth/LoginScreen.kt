package com.lightcare.app.ui.auth

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lightcare.app.ui.theme.LCPrimaryButton
import com.lightcare.app.ui.theme.LCSecondaryButton
import com.lightcare.app.ui.theme.S

/**
 * PR-Auth：登录屏（升级版 v2）。
 *
 * 视觉与 RegisterScreen 对齐：图标式输入框 + 密码可见切换 + 登录/去注册分层按钮。
 */
@Composable
fun LoginScreen(
    onAuthed: () -> Unit,
    onGoRegister: () -> Unit,
    vm: AuthViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.success) {
        if (state.success) onAuthed()
    }

    AuthScaffold(
        title = "欢迎回来",
        subtitle = "登录以继续你的轻养记录"
    ) {
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
                label = "密码",
                value = state.password,
                onChange = vm::updatePassword,
                keyboard = KeyboardType.Password,
                imeAction = ImeAction.Done,
                placeholder = "至少 6 位",
                isPassword = true,
                passwordVisible = state.passwordVisible,
                onTogglePasswordVisible = vm::togglePasswordVisible,
                leadingIcon = Icons.Outlined.Lock
            )
            Spacer(Modifier.height(S.lg))
            LCPrimaryButton(
                text = "登录",
                onClick = vm::submit,
                loading = state.loading,
                enabled = !state.loading
            )
            Spacer(Modifier.height(S.sm))
            LCSecondaryButton(
                text = "去注册",
                onClick = onGoRegister,
                leadingEmoji = "✨"
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