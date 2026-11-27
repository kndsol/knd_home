package com.aircargo.manager.ui

import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aircargo.manager.AppViewModel
import com.aircargo.manager.ui.screens.BookingFormScreen
import com.aircargo.manager.ui.screens.BookingListScreen
import com.aircargo.manager.ui.screens.HomeScreen
import com.aircargo.manager.ui.screens.LiveStatusScreen
import com.aircargo.manager.ui.screens.LoginScreen
import com.aircargo.manager.ui.screens.NotifySettingsScreen
import com.aircargo.manager.ui.screens.ScheduleScreen
import com.aircargo.manager.ui.screens.SettingsScreen
import com.aircargo.manager.ui.screens.TrackingScreen
import com.aircargo.manager.ui.screens.WebScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val SCHEDULE = "schedule"
    const val BOOKING = "booking"
    const val BOOKINGS = "bookings"
    const val TRACKING = "tracking"
    const val LIVE = "live"
    const val SETTINGS = "settings"
    const val NOTIFY = "notify"
    const val WEB = "web?url={url}&title={title}"

    fun web(url: String, title: String) = "web?url=${Uri.encode(url)}&title=${Uri.encode(title)}"
}

fun NavHostController.openWeb(url: String, title: String) = navigate(Routes.web(url, title))

/** 관리자 전용 화면 가드 — 고객 접근 시 즉시 되돌림 */
@Composable
private fun AdminOnly(vm: AppViewModel, onDenied: () -> Unit) {
    LaunchedEffect(vm.isAdmin) { if (!vm.isAdmin) onDenied() }
}

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    val vm: AppViewModel = viewModel()
    val context = LocalContext.current

    // 메일 발송 결과 공통 표시
    LaunchedEffect(vm.mailResult) {
        vm.mailResult?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            vm.consumeMailResult()
        }
    }

    NavHost(navController = nav, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLogin = { id, pw -> vm.login(id, pw) },
                onCustomer = { vm.loginCustomer() },
                onSuccess = {
                    nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                vm = vm,
                onNavigate = { nav.navigate(it) },
                onLogout = {
                    vm.logout()
                    nav.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                },
            )
        }
        composable(Routes.SCHEDULE) {
            ScheduleScreen(vm, onBack = { nav.popBackStack() }, onOpenWeb = nav::openWeb,
                onSettings = { nav.navigate(Routes.SETTINGS) })
        }
        composable(Routes.BOOKING) {
            BookingFormScreen(vm, onBack = { nav.popBackStack() }, onDone = {
                if (vm.isAdmin) nav.navigate(Routes.BOOKINGS) { popUpTo(Routes.HOME) }
                else nav.popBackStack(Routes.HOME, inclusive = false)
            })
        }
        composable(Routes.BOOKINGS) {
            AdminOnly(vm) { nav.popBackStack() }
            if (vm.isAdmin) BookingListScreen(vm, onBack = { nav.popBackStack() },
                onNew = { nav.navigate(Routes.BOOKING) })
        }
        composable(Routes.TRACKING) {
            TrackingScreen(vm, onBack = { nav.popBackStack() }, onOpenWeb = nav::openWeb)
        }
        composable(Routes.LIVE) {
            LiveStatusScreen(vm, onBack = { nav.popBackStack() }, onOpenWeb = nav::openWeb)
        }
        composable(Routes.SETTINGS) {
            AdminOnly(vm) { nav.popBackStack() }
            if (vm.isAdmin) SettingsScreen(vm, onBack = { nav.popBackStack() }, onNotify = { nav.navigate(Routes.NOTIFY) })
        }
        composable(Routes.NOTIFY) {
            AdminOnly(vm) { nav.popBackStack() }
            if (vm.isAdmin) NotifySettingsScreen(vm, onBack = { nav.popBackStack() })
        }
        composable(
            Routes.WEB,
            arguments = listOf(
                navArgument("url") { type = NavType.StringType; defaultValue = "" },
                navArgument("title") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            WebScreen(
                url = entry.arguments?.getString("url").orEmpty(),
                title = entry.arguments?.getString("title").orEmpty(),
                onBack = { nav.popBackStack() },
            )
        }
    }
}
