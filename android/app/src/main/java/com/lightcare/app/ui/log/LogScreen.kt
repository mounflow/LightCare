package com.lightcare.app.ui.log

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.ContextCompat
import com.lightcare.app.data.food.FoodItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import com.lightcare.app.ui.theme.BorderSubtle
import com.lightcare.app.ui.theme.D
import com.lightcare.app.ui.theme.Error
import com.lightcare.app.ui.theme.LCAppear
import com.lightcare.app.ui.theme.LCEmojiBadge
import com.lightcare.app.ui.theme.LCPrimaryButton
import com.lightcare.app.ui.theme.LCSecondaryButton
import com.lightcare.app.ui.theme.LCStatCell
import com.lightcare.app.ui.theme.LCTopBar
import com.lightcare.app.ui.theme.OnPrimary
import com.lightcare.app.ui.theme.Outline
import com.lightcare.app.ui.theme.Primary
import com.lightcare.app.ui.theme.PrimaryContainer
import com.lightcare.app.ui.theme.PureWhite
import com.lightcare.app.ui.theme.S
import com.lightcare.app.ui.theme.SecondaryContainer
import com.lightcare.app.ui.theme.SurgicalGreen
import com.lightcare.app.ui.theme.ambientCard
import com.lightcare.app.ui.theme.categoryEmojiOf
import kotlinx.coroutines.launch

/**
 * 记录页（深度优化版）。
 *
 * - 顶栏用 LCTopBar。
 * - 三选一区用大圆形 emoji 入口（拍照 / 相册 / 食物库）。
 * - 已选清单 + 总计卡用 ambientCard。
 * - 照片编辑面板：分段（顶栏 / 餐别 / 地点+描述 / 食材行 / 提交），
 *   食材行用统一 EditableFoodRow。
 */
@Composable
fun LogScreen(
    onClose: () -> Unit,
    onMealPending: (Long) -> Unit = {},
    vm: LogViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var pickerOpen by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    LaunchedEffect(vm) { vm.onPendingMeal = { mealId -> onMealPending(mealId) } }

    val pickImage = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) scope.launch {
            val file = compressUriToJpgCache(context, uri)
            if (file != null) vm.startPhotoEdit(file)
        }
    }

    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val uri = pendingCameraUri
        pendingCameraUri = null
        if (success && uri != null) scope.launch {
            val file = compressUriToJpgCache(context, uri)
            if (file != null) vm.startPhotoEdit(file)
        }
    }

    val cameraPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera(context, takePicture) { pendingCameraUri = it }
        else android.widget.Toast.makeText(
            context, "需要相机权限才能拍照，请到设置开启",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }

    val locationPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.any { it }) vm.fetchLocation()
        else android.widget.Toast.makeText(
            context, "需要定位权限才能记录地点，可手填",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
    val onRelocateClick = {
        val hasLoc = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        if (hasLoc) vm.fetchLocation()
        else locationPermission.launch(arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ))
    }
    LaunchedEffect(state.editingPhotoFile) {
        if (state.editingPhotoFile != null) {
            val hasLoc = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (hasLoc) vm.fetchLocation()
        }
    }
    LaunchedEffect(state.saved) { if (state.saved) { vm.reset(); onClose() } }
    LaunchedEffect(state.error) {
        state.error?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LCTopBar(title = "记录一餐", onBack = onClose, emoji = "📝")
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            PickEntry(
                onPhoto = {
                    if (ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        launchCamera(context, takePicture) { pendingCameraUri = it }
                    } else {
                        cameraPermission.launch(Manifest.permission.CAMERA)
                    }
                },
                onGallery = {
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPick = { pickerOpen = true },
                hasItems = state.detected.isNotEmpty(),
                recognizing = state.recognizing
            )
        }
        LCAppear(visible = state.detected.isNotEmpty()) {
            ConfirmSection(
                state = state,
                saving = state.saving,
                onSave = vm::save,
                onRemove = vm::removeDetected
            )
        }
    }

    if (pickerOpen) {
        FoodPickerSheet(
            vm = vm,
            onDismiss = { pickerOpen = false },
            onAdd = { items ->
                vm.addDetected(items)
                pickerOpen = false
            }
        )
    }

    if (state.editingPhotoFile != null) {
        EditPhotoSheet(
            state = state,
            onNameChange = vm::updateEditableName,
            onCategoryChange = vm::updateEditableCategory,
            onWeightChange = vm::updateEditableWeight,
            onRemove = vm::removeEditable,
            onAddBlank = vm::addBlankEditable,
            onSlotChange = vm::setSlot,
            onLocationChange = vm::updateLocation,
            onDescriptionChange = vm::updateDescription,
            onRelocate = onRelocateClick,
            onSubmit = vm::submitPhotoEdit,
            onDismiss = vm::dismissEdit
        )
    }
}

private fun launchCamera(
    context: Context,
    takePicture: androidx.activity.result.ActivityResultLauncher<Uri>,
    setPendingUri: (Uri) -> Unit
) {
    val photoFile = File(context.cacheDir, "photo").apply { mkdirs() }
        .let { dir -> File(dir, "capture_${System.currentTimeMillis()}.jpg") }
    val uri = androidx.core.content.FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        photoFile
    )
    setPendingUri(uri)
    takePicture.launch(uri)
}

private suspend fun compressUriToJpgCache(context: Context, uri: Uri): File? =
    withContext(Dispatchers.IO) {
        try {
            val input = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val raw: Bitmap = BitmapFactory.decodeStream(input) ?: return@withContext null
            input.close()
            val maxSide = 1024
            val scale = maxSide.toFloat() / maxOf(raw.width, raw.height).toFloat()
            val resized = if (scale < 1f) Bitmap.createScaledBitmap(
                raw, (raw.width * scale).toInt(), (raw.height * scale).toInt(), true
            ) else raw
            val file = File(context.cacheDir, "rec_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                resized.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            if (resized !== raw) resized.recycle()
            file
        } catch (e: Exception) { null }
    }

// ─────────────────────────────────────────────────
// 三选一入口：拍照 / 相册 / 食物库
// ─────────────────────────────────────────────────
@Composable
private fun PickEntry(
    onPhoto: () -> Unit,
    onGallery: () -> Unit,
    onPick: () -> Unit,
    hasItems: Boolean,
    recognizing: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = S.screenH, vertical = S.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(D.iconHero)
                .background(PrimaryContainer, RoundedCornerShape(D.radiusPill)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.PhotoCamera,
                contentDescription = null,
                tint = Primary,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(Modifier.height(S.lg))
        Text(
            if (hasItems) "继续添加食物" else "记录刚才吃了什么",
            style = MaterialTheme.typography.titleLarge,
            color = Primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(S.xs))
        Text(
            "拍照 / 相册 / 食物库 任选",
            style = MaterialTheme.typography.bodySmall,
            color = Outline
        )
        Spacer(Modifier.height(S.xxl))
        LCPrimaryButton(
            text = "拍照",
            onClick = onPhoto,
            enabled = !recognizing,
            loading = recognizing,
            leadingEmoji = "📷"
        )
        Spacer(Modifier.height(S.md))
        LCSecondaryButton(
            text = "从相册选",
            onClick = onGallery,
            enabled = !recognizing,
            leadingEmoji = "🖼️"
        )
        Spacer(Modifier.height(S.md))
        LCSecondaryButton(
            text = "从食物库选",
            onClick = onPick,
            leadingEmoji = "🍱"
        )
    }
}

// ─────────────────────────────────────────────────
// 已选食物清单 + 总计 + 确认按钮
// ─────────────────────────────────────────────────
@Composable
private fun ConfirmSection(
    state: LogUiState,
    saving: Boolean,
    onSave: () -> Unit,
    onRemove: (FoodItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .ambientCard()
            .padding(S.xl),
        verticalArrangement = Arrangement.spacedBy(S.md)
    ) {
        com.lightcare.app.ui.theme.LCCardLabel("已选食物", emoji = "🍽️")
        state.detected.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRemove(item) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                LCEmojiBadge(
                    emoji = categoryEmojiOf(item.category),
                    size = D.avatarMd,
                    background = MaterialTheme.colorScheme.surface
                )
                Spacer(Modifier.width(S.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Primary
                    )
                    Text(
                        "${item.perServingKcal} kcal · ${item.category}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Outline
                    )
                }
                Text(
                    "移除 ×",
                    style = MaterialTheme.typography.labelMedium,
                    color = Error,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        TotalCard(state)
        LCPrimaryButton(
            text = "确认并记录",
            onClick = onSave,
            loading = saving,
            modifier = Modifier
        )
    }
}

@Composable
private fun TotalCard(state: LogUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(D.radiusLg))
            .border(1.dp, BorderSubtle, RoundedCornerShape(D.radiusLg))
            .padding(S.lg),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                "合计（${state.portion.display}）",
                style = MaterialTheme.typography.labelSmall,
                color = Outline
            )
            Spacer(Modifier.height(S.xxs))
            Text(
                "${state.totalKcal} Kcal",
                style = MaterialTheme.typography.headlineMedium,
                color = Primary,
                fontWeight = FontWeight.Bold
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(S.sm)) {
            LCStatCell("蛋白", formatG(state.totalProtein), "g", modifier = Modifier)
            LCStatCell("脂肪", formatG(state.totalFat), "g", modifier = Modifier)
            LCStatCell("碳水", formatG(state.totalCarb), "g", modifier = Modifier)
        }
    }
}

private fun formatG(v: Double): String =
    if (v >= 100) "${v.toInt()}g" else "${"%.1f".format(v)}g"

// ─────────────────────────────────────────────────
// 食物选择底部弹层
// ─────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodPickerSheet(
    vm: LogViewModel,
    onDismiss: () -> Unit,
    onAdd: (List<FoodItem>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val queryFlow = remember { MutableStateFlow("") }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    val selected = remember { androidx.compose.runtime.mutableStateListOf<FoodItem>() }

    LaunchedEffect(Unit) {
        @OptIn(FlowPreview::class)
        queryFlow.debounce(200).distinctUntilChanged().collectLatest { q ->
            results = vm.foodRepo.search(q)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = S.xl)) {
            Text(
                "选择食物",
                style = MaterialTheme.typography.titleLarge,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(S.md))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; queryFlow.value = it },
                placeholder = { Text("搜索食物，如：鸡胸、米饭") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, tint = Outline) },
                singleLine = true,
                shape = RoundedCornerShape(D.radiusMd),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(S.md))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(360.dp),
                verticalArrangement = Arrangement.spacedBy(S.sm)
            ) {
                items(results, key = { it.key }) { item ->
                    val isSelected = selected.any { it.key == item.key }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) PrimaryContainer else MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(D.radiusSm)
                            )
                            .clickable {
                                if (isSelected) selected.removeAll { it.key == item.key }
                                else selected.add(item)
                            }
                            .padding(S.md),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(categoryEmojiOf(item.category), style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.width(S.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                item.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Primary
                            )
                            Text(
                                "${item.perServingKcal} kcal · 蛋白${formatG(item.perServingProtein)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Outline
                            )
                        }
                        Text(
                            if (isSelected) "✓" else "",
                            color = Primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(Modifier.height(S.md))
            LCPrimaryButton(
                text = if (selected.isEmpty()) "选择食物后添加" else "添加 ${selected.size} 项",
                onClick = { onAdd(selected.toList()) },
                enabled = selected.isNotEmpty()
            )
            Spacer(Modifier.height(S.xl))
        }
    }
}

// ─────────────────────────────────────────────────
// 拍照编辑面板
// ─────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPhotoSheet(
    state: LogUiState,
    onNameChange: (Int, String) -> Unit,
    onCategoryChange: (Int, String) -> Unit,
    onWeightChange: (Int, Int) -> Unit,
    onRemove: (Int) -> Unit,
    onAddBlank: () -> Unit,
    onSlotChange: (MealSlot) -> Unit,
    onLocationChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onRelocate: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val aggKcal = remember(state.editableItems) {
        val defaults = com.lightcare.app.data.food.FoodLibrary.DEFAULTS
        val kcalPer100gByCat = defaults.groupBy { it.category }.mapValues { (_, list) ->
            list.map { it.perServingKcal.toDouble() }.average()
        }
        val globalAvg = defaults.map { it.perServingKcal.toDouble() }.average()
        state.editableItems.sumOf { e ->
            val per100 = kcalPer100gByCat[e.category] ?: globalAvg
            (per100 * e.weightG / 100.0).toInt().coerceAtLeast(0)
        }
    }
    val aggWater = remember(state.editableItems) { state.editableItems.sumOf { it.waterMl } }
    var showDiscardConfirm by remember { mutableStateOf(false) }
    val hasContent = state.editableItems.any { it.name.isNotBlank() } || state.description.isNotBlank()
    val requestDismiss = { if (hasContent) showDiscardConfirm = true else onDismiss() }

    ModalBottomSheet(onDismissRequest = requestDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = S.xl)) {
            Text(
                "编辑食材",
                style = MaterialTheme.typography.titleLarge,
                color = Primary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(S.xs))
            Text(
                "按需调整名称、分类、重量（先粗估，识别完成后会精确更新）",
                style = MaterialTheme.typography.labelSmall,
                color = Outline
            )
            Spacer(Modifier.height(S.md))

            // 餐别
            Row(
                horizontalArrangement = Arrangement.spacedBy(S.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MealSlot.entries.forEach { ms ->
                    FilterChip(
                        selected = state.slot == ms,
                        onClick = { onSlotChange(ms) },
                        label = { Text(ms.display, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryContainer,
                            selectedLabelColor = Primary
                        )
                    )
                }
            }
            Spacer(Modifier.height(S.md))

            // 错误条
            if (state.error != null && !state.submittingPhoto) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer, RoundedCornerShape(D.radiusSm))
                        .padding(S.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "⚠ ${state.error}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(Modifier.height(S.md))
            }

            // 拍完照的本地预览
            state.editingPhotoFile?.let { file ->
                val bitmap = remember(file) {
                    try { decodeSampledBitmap(file, 480) } catch (e: Exception) { null }
                }
                bitmap?.let {
                    androidx.compose.foundation.Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "拍的饭",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(D.radiusLg)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Spacer(Modifier.height(S.md))
                }
            }

            // 地点 + 描述
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.LocationOn,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(S.xs))
                OutlinedTextField(
                    value = state.location,
                    onValueChange = onLocationChange,
                    placeholder = {
                        Text(
                            if (state.locating) "定位中…" else "地点（可手填）",
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(D.radiusSm),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(S.xs))
                Text(
                    "⟳",
                    style = MaterialTheme.typography.titleMedium,
                    color = Primary,
                    modifier = Modifier
                        .clickable(onClick = onRelocate)
                        .padding(S.xs)
                )
            }
            Spacer(Modifier.height(S.md))
            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                placeholder = { Text("记一笔：和谁吃的、心情如何…", style = MaterialTheme.typography.labelMedium) },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(D.radiusSm),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(S.md))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(420.dp),
                verticalArrangement = Arrangement.spacedBy(S.md)
            ) {
                items(state.editableItems, key = { it.id }) { e ->
                    val idx = state.editableItems.indexOf(e)
                    EditableFoodRow(
                        item = e,
                        onNameChange = { onNameChange(idx, it) },
                        onCategoryChange = { onCategoryChange(idx, it) },
                        onWeightChange = { onWeightChange(idx, it) },
                        onRemove = { onRemove(idx) },
                        canRemove = state.editableItems.size > 1
                    )
                }
            }

            // + 添加一项
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(D.controlMd)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(D.radiusSm))
                    .clickable(onClick = onAddBlank),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = null,
                    tint = Primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(S.xs))
                Text(
                    "添加一项",
                    style = MaterialTheme.typography.titleSmall,
                    color = Primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(S.md))

            val canSubmit = state.editableItems.any { it.name.isNotBlank() && it.weightG > 0 } && !state.submittingPhoto
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(D.controlXl + 4.dp)
                    .background(
                        if (canSubmit) Primary else Outline,
                        RoundedCornerShape(D.radiusMd)
                    )
                    .clickable(enabled = canSubmit, onClick = onSubmit),
                contentAlignment = Alignment.Center
            ) {
                if (state.submittingPhoto) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.CircularProgressIndicator(
                            color = OnPrimary,
                            strokeWidth = D.thick,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(S.sm))
                        Text(
                            "提交中…",
                            color = OnPrimary,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                } else {
                    Text(
                        "提交  $aggKcal Kcal · ${aggWater}ml 水",
                        color = OnPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            Spacer(Modifier.height(S.xl))
        }
    }

    if (showDiscardConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text("放弃这次记录？", color = Primary, fontWeight = FontWeight.Bold) },
            text = { Text("你填的内容还没提交，关闭后会丢失。", style = MaterialTheme.typography.bodySmall, color = Outline) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showDiscardConfirm = false
                    onDismiss()
                }) { Text("放弃", color = Error, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showDiscardConfirm = false }) {
                    Text("继续编辑", color = Outline)
                }
            }
        )
    }
}

// ─────────────────────────────────────────────────
// 食材行：emoji + 名称 + 分类 chip + 重量 stepper
// ─────────────────────────────────────────────────
@Composable
private fun EditableFoodRow(
    item: EditableFood,
    onNameChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onWeightChange: (Int) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean
) {
    val category = FoodCategory.fromLabel(item.category)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(D.radiusMd))
            .padding(S.md),
        verticalArrangement = Arrangement.spacedBy(S.sm)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LCEmojiBadge(
                emoji = category.emoji,
                size = D.avatarMd,
                background = PrimaryContainer
            )
            Spacer(Modifier.width(S.md))
            OutlinedTextField(
                value = item.name,
                onValueChange = onNameChange,
                placeholder = { Text("食物名称") },
                singleLine = true,
                shape = RoundedCornerShape(D.radiusSm),
                modifier = Modifier.weight(1f)
            )
            if (canRemove) {
                Spacer(Modifier.width(S.xs))
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = "移除",
                    tint = Error,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onRemove)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(S.xs),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FoodCategory.entries.forEach { fc ->
                FilterChip(
                    selected = category == fc,
                    onClick = { onCategoryChange(fc.display) },
                    label = { Text(fc.display, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryContainer,
                        selectedLabelColor = Primary
                    )
                )
            }
        }
        WeightStepper(valueG = item.weightG, onChange = onWeightChange)
    }
}

@Composable
private fun WeightStepper(valueG: Int, onChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(S.xs)
    ) {
        StepperBtn("-10") { onChange((valueG - 10).coerceAtLeast(1)) }
        StepperBtn("-") { onChange((valueG - 5).coerceAtLeast(1)) }
        OutlinedTextField(
            value = valueG.toString(),
            onValueChange = { v ->
                val n = v.filter { it.isDigit() }.toIntOrNull() ?: 0
                onChange(if (n <= 0) 1 else n.coerceAtMost(5000))
            },
            singleLine = true,
            shape = RoundedCornerShape(D.radiusSm),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(72.dp)
        )
        Text("g", style = MaterialTheme.typography.labelMedium, color = Outline)
        StepperBtn("+") { onChange(valueG + 5) }
        StepperBtn("+10") { onChange(valueG + 10) }
    }
}

@Composable
private fun StepperBtn(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(D.controlSm)
            .background(PrimaryContainer, RoundedCornerShape(D.radiusSm))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = Primary,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun decodeSampledBitmap(file: File, reqWidth: Int): android.graphics.Bitmap? {
    val opts = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeFile(file.absolutePath, opts)
    if (opts.outWidth <= 0) return null
    var sampleSize = 1
    while (opts.outWidth / sampleSize > reqWidth) sampleSize *= 2
    val decodeOpts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return android.graphics.BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
}
