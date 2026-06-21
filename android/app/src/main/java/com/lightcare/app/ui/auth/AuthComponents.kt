package com.lightcare.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.lightcare.app.ui.theme.BorderSubtle
import com.lightcare.app.ui.theme.D
import com.lightcare.app.ui.theme.OnPrimary
import com.lightcare.app.ui.theme.OnPrimaryContainer
import com.lightcare.app.ui.theme.Outline
import com.lightcare.app.ui.theme.Primary
import com.lightcare.app.ui.theme.PrimaryContainer
import com.lightcare.app.ui.theme.S

/**
 * PR-Auth：登录/注册共享的骨架（品牌区 + 标题 + 内容插槽）。
 *
 * 设计：左上圆形软 emoji hero + 中文品牌名 + 副标题（vs 老版本 hero 与副标题并排）。
 * 内容区默认卡片化。
 */
@Composable
fun AuthScaffold(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        if (onBack != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = S.sm, vertical = S.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(D.topBar - 16.dp)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "返回",
                        tint = Primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        // 品牌区
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = S.xxl, vertical = S.lg)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(PrimaryContainer, androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌿", style = MaterialTheme.typography.displaySmall)
                }
                Spacer(Modifier.width(S.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "轻养 LightCare",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(S.xxs))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Outline
                    )
                }
            }
            Spacer(Modifier.height(S.lg))
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                color = Primary,
                fontWeight = FontWeight.Bold
            )
        }
        content()
    }
}

/** 单卡：输入字段 + 按钮的容器（柔和边框 + 顶部小绿条）。 */
@Composable
fun AuthCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = S.xxl, vertical = S.lg),
        verticalArrangement = Arrangement.spacedBy(S.md)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(D.radiusXl))
                .border(D.hairline, BorderSubtle, RoundedCornerShape(D.radiusXl))
        ) {
            // 顶部小绿条装饰
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(Primary, RoundedCornerShape(topStart = D.radiusXl, topEnd = D.radiusXl))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(S.xxl),
                verticalArrangement = Arrangement.spacedBy(S.md)
            ) {
                content()
            }
        }
    }
}

/** PR-Auth：单输入框。带 leading emoji / icon、focus 边色变化；密码可切换可见性。 */
@Composable
fun AuthField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    keyboard: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    leadingEmoji: String? = null,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePasswordVisible: (() -> Unit)? = null,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        enabled = enabled,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = Outline) } },
        singleLine = true,
        shape = RoundedCornerShape(D.radiusMd),
        visualTransformation = when {
            !isPassword -> VisualTransformation.None
            passwordVisible -> VisualTransformation.None
            else -> PasswordVisualTransformation()
        },
        leadingIcon = when {
            leadingEmoji != null -> {
                { Text(leadingEmoji, style = MaterialTheme.typography.titleMedium) }
            }
            leadingIcon != null -> {
                { Icon(leadingIcon, contentDescription = null, tint = Primary) }
            }
            else -> null
        },
        trailingIcon = if (isPassword && onTogglePasswordVisible != null) {
            {
                IconButton(onClick = onTogglePasswordVisible) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) "隐藏密码" else "显示密码",
                        tint = Outline
                    )
                }
            }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = imeAction),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Primary,
            unfocusedBorderColor = BorderSubtle,
            focusedLabelColor = Primary,
            unfocusedLabelColor = Outline,
            cursorColor = Primary,
            focusedLeadingIconColor = Primary,
            unfocusedLeadingIconColor = Outline
        ),
        modifier = modifier.fillMaxWidth()
    )
}