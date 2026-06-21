package com.lightcare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lightcare.app.data.auth.AuthStore
import com.lightcare.app.ui.auth.AuthNavGraph
import com.lightcare.app.ui.food.FoodConflictSheet
import com.lightcare.app.ui.food.FoodConflictViewModel
import com.lightcare.app.ui.food.FoodLibraryScreen
import com.lightcare.app.ui.food.detail.FoodDetailScreen
import com.lightcare.app.ui.history.MealHistoryScreen
import com.lightcare.app.ui.home.HomeScreen
import com.lightcare.app.ui.insight.InsightScreen
import com.lightcare.app.ui.log.LogScreen
import com.lightcare.app.ui.log.RecognitionViewModel
import com.lightcare.app.ui.profile.PhysiqueScreen
import com.lightcare.app.ui.profile.ProfileSelectionScreen
import com.lightcare.app.ui.settings.SettingsScreen
import com.lightcare.app.ui.shared.MainScaffold
import com.lightcare.app.ui.shared.MainTab
import com.lightcare.app.ui.theme.LightCareTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var authStore: AuthStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LightCareTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // PR-Auth 三态：
                    //   token == null      → AuthNavGraph (登录 / 注册)
                    //   profileId == null  → ProfileSelectionScreen (选档 / 建档，登录后还没选)
                    //   都非 null          → MainNavGraph (主页 + 二级页)
                    val token by authStore.tokenFlow.collectAsStateWithLifecycle(initialValue = null)
                    val profileId by authStore.profileIdFlow.collectAsStateWithLifecycle(initialValue = null)

                    when {
                        token.isNullOrBlank() -> AuthNavGraph(onAuthed = { /* tokenFlow 自动切到下一态 */ })
                        profileId == null -> ProfileSelectionScreen()
                        else -> MainNavGraph(authStore)
                    }
                }
            }
        }
    }
}

/**
 * 主页 NavHost。
 *
 * - 主 tab（Home / Data / Settings）用 remember state 切换，不走 NavHost（tab 不是"页面栈"语义）。
 * - 二级详情页（log / food_library / physique）走 NavHost，系统手势返回 / 返回键天然生效（P43）。
 *   - 从这些页面 popBackStack 会回到进入前的主 tab（Home 或 Settings）。
 *
 * PR3: RecognitionViewModel 在 NavGraph 顶级（ActivityScope）持有，LogScreen 关闭后轮询不中断。
 *      识别完成时 refreshKey++ 触发 HomeViewModel 拉最新数据，余量数字跳到 M3 精确值。
 */
@Composable
private fun MainNavGraph(authStore: AuthStore) {
    val nav = rememberNavController()
    var tab by remember { mutableStateOf(MainTab.Home) }
    var refreshKey by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val recognitionVm: RecognitionViewModel = hiltViewModel()  // PR3: ActivityScope
    val foodConflictVm: FoodConflictViewModel = hiltViewModel()  // PR-D: ActivityScope
    val foodConflictState by foodConflictVm.state.collectAsStateWithLifecycle()

    // P0: 回到前台（ON_RESUME）时刷新首页 —— 解决跨天后余量还显示昨天的累计、
    // 从记录页返回不刷新等问题。refreshKey++ 触发 HomeScreen 的 LaunchedEffect 重拉。
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                refreshKey++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val appContext = androidx.compose.ui.platform.LocalContext.current

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            MainScaffold(
                currentTab = tab,
                onTabSelected = { tab = it },
                onFabClick = { nav.navigate("log") }
            ) {
                when (tab) {
                    MainTab.Home -> HomeScreen(
                        refreshKey = refreshKey,
                        onViewHistory = { nav.navigate("meal_history") },
                        onOpenFood = { foodId -> nav.navigate("food_detail/$foodId") },
                        onOpenPhysique = { nav.navigate("physique") }
                    )
                    MainTab.Data -> InsightScreen(onViewHistory = { nav.navigate("meal_history") })
                    MainTab.Settings -> SettingsScreen(
                        onSwitchProfile = {
                            scope.launch { authStore.clearProfile() }
                        },
                        onNavigateToFoodLibrary = { nav.navigate("food_library") },
                        onNavigateToPhysique = { nav.navigate("physique") }
                    )
                }
            }
        }
        composable("log") {
            LogScreen(
                onClose = {
                    nav.popBackStack()
                    refreshKey++   // 回 Home 时刷新今日记录（基础值入库）
                },
                onMealPending = { mealId ->
                    // 启动轮询：识别完成 / 失败 / 30s 超时时回调
                    // PR-D: 识别完成后顺便拉一次食物库冲突
                    // P0-2: FAILED 时 Toast 告诉用户「识别失败，已按粗估值记录」
                    recognitionVm.startPolling(mealId) { status ->
                        refreshKey++
                        foodConflictVm.checkConflicts()
                        if (status == "FAILED") {
                            android.widget.Toast.makeText(
                                appContext,
                                "图片识别失败，已按你填的估算值记录",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        } else if (status == "PENDING") {
                            android.widget.Toast.makeText(
                                appContext,
                                "识别还在进行，稍后自动更新",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
        }
        composable("food_library") {
            FoodLibraryScreen(
                onBack = { nav.popBackStack() },
                onOpenFood = { foodId -> nav.navigate("food_detail/$foodId") }
            )
        }
        composable(
            route = "food_detail/{foodId}",
            arguments = listOf(androidx.navigation.navArgument("foodId") { type = androidx.navigation.NavType.LongType })
        ) {
            FoodDetailScreen(onBack = { nav.popBackStack() })
        }
        composable("meal_history") {
            MealHistoryScreen(onBack = { nav.popBackStack() })
        }
        composable("physique") {
            PhysiqueScreen(onBack = { nav.popBackStack() })
        }
    }

    // PR-D: 食物库冲突处理弹层（覆盖在 NavHost 之上，跨页面可见）
    if (foodConflictState.showSheet && foodConflictState.conflicts.isNotEmpty()) {
        FoodConflictSheet(
            conflicts = foodConflictState.conflicts,
            resolving = foodConflictState.resolving,
            onResolve = { id, action, newName -> foodConflictVm.resolve(id, action, newName) },
            onDismiss = { foodConflictVm.dismiss() }
        )
    }
}
