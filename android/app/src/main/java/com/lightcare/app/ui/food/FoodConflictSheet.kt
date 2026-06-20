package com.lightcare.app.ui.food

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lightcare.app.data.api.FoodDto
import com.lightcare.app.ui.theme.OnErrorContainer
import com.lightcare.app.ui.theme.ErrorContainer
import com.lightcare.app.ui.theme.Outline
import com.lightcare.app.ui.theme.Primary
import com.lightcare.app.ui.theme.PureWhite

/**
 * PR-D: 食物库冲突处理弹层。
 *
 * 拍照识别出的食物与库中"同名但营养不同"时弹出，让用户决定：
 *   - 换名（RENAME）：这条用新名字入库，保留原同名项
 *   - 覆盖（OVERWRITE）：用识别项的营养覆盖原同名项
 *   - 跳过（SKIP）：不入库这条
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodConflictSheet(
    conflicts: List<FoodDto>,
    resolving: Boolean,
    onResolve: (id: Long, action: String, newName: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Text("📷 发现重复食物", style = MaterialTheme.typography.titleLarge,
                color = Primary, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text("识别出的食物与库里同名但营养不同，请选择处理方式",
                style = MaterialTheme.typography.labelSmall, color = Outline)
            Spacer(Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(conflicts, key = { it.id }) { c ->
                    ConflictRow(c, resolving, onResolve)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun ConflictRow(c: FoodDto, resolving: Boolean, onResolve: (Long, String, String?) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ErrorContainer, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Text(c.displayName, style = MaterialTheme.typography.titleMedium,
            color = OnErrorContainer, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "${c.kcal} kcal · 蛋白${fmt(c.proteinG)} · 脂肪${fmt(c.fatG)} · 碳水${fmt(c.carbG)}",
            style = MaterialTheme.typography.labelMedium, color = Outline
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConflictBtn("换名", Modifier.weight(1f), enabled = !resolving) {
                // 换名：原名 + 后缀 _2（简单起见，用户可在食物库页再改）
                onResolve(c.id, "RENAME", "${c.displayName}_2")
            }
            ConflictBtn("覆盖", Modifier.weight(1f), enabled = !resolving) {
                onResolve(c.id, "OVERWRITE", null)
            }
            ConflictBtn("跳过", Modifier.weight(1f), enabled = !resolving) {
                onResolve(c.id, "SKIP", null)
            }
        }
        if (resolving) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = Primary, strokeWidth = 2.dp, modifier = Modifier.height(14.dp).width(14.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("处理中…", style = MaterialTheme.typography.labelSmall, color = Outline)
            }
        }
    }
}

@Composable
private fun ConflictBtn(text: String, modifier: Modifier, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(40.dp)
            .background(if (enabled) Primary else Outline, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = PureWhite, fontWeight = FontWeight.SemiBold,
            style = MaterialTheme.typography.labelLarge)
    }
}

private fun fmt(v: Double): String = if (v >= 100) "${v.toInt()}g" else "${"%.1f".format(v)}g"
