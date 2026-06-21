package com.lightcare.app.ui.auth

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * PR-Auth：登录 / 注册二级 NavHost。
 *
 * 入口由 MainActivity 持有（仅 token == null 时挂载）；登录/注册成功 → 调 onAuthed()，
 * MainActivity 据此切到 ProfileSelectionScreen 或 MainNavGraph。
 */
@Composable
fun AuthNavGraph(onAuthed: () -> Unit) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onAuthed = onAuthed,
                onGoRegister = { nav.navigate("register") }
            )
        }
        composable("register") {
            RegisterScreen(
                onAuthed = onAuthed,
                onBack = { nav.popBackStack() }
            )
        }
    }
}