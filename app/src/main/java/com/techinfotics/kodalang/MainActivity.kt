package com.techinfotics.kodalang

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.techinfotics.kodalang.data.AuthRepository
import com.techinfotics.kodalang.data.DashboardRepository
import com.techinfotics.kodalang.ui.auth.AuthScreen
import com.techinfotics.kodalang.ui.auth.AuthViewModel
import com.techinfotics.kodalang.ui.home.HomeScreen
import com.techinfotics.kodalang.ui.home.HomeViewModel
import com.techinfotics.kodalang.ui.nav.BottomTabs
import com.techinfotics.kodalang.ui.nav.Routes
import com.techinfotics.kodalang.ui.practice.PracticeScreen
import com.techinfotics.kodalang.ui.practice.PracticeViewModel
import com.techinfotics.kodalang.ui.profile.ProfileScreen
import com.techinfotics.kodalang.ui.profile.ProfileViewModel
import com.techinfotics.kodalang.ui.splash.SplashScreen
import com.techinfotics.kodalang.ui.splash.SplashViewModel
import com.techinfotics.kodalang.ui.theme.KodaLangTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KodaLangTheme {
                AppNav()
            }
        }
    }
}

/** Simple ViewModel factory that injects the app-scoped repositories. */
@Composable
inline fun <reified VM : ViewModel> kodaViewModel(
    key: String? = null,
    crossinline create: (android.content.Context) -> VM,
): VM {
    val context = LocalContext.current
    return viewModel(
        key = key,
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                create(context.applicationContext) as T
        },
    )
}

@Composable
private fun AppNav() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val showBottomBar = route in BottomTabs

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    BottomNavItem(Routes.HOME, "Home", Icons.Filled.Home, route, navController)
                    BottomNavItem(Routes.PRACTICE, "Practice", Icons.Filled.ChatBubble, route, navController)
                    BottomNavItem(Routes.PROFILE, "Profile", Icons.Filled.Person, route, navController)
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.SPLASH) {
                val vm: SplashViewModel = kodaViewModel { ctx -> SplashViewModel(AuthRepository(ctx)) }
                SplashScreen(
                    viewModel = vm,
                    onSignedIn = {
                        navController.navigate(Routes.HOME) { popUpTo(0) }
                    },
                    onSignedOut = {
                        navController.navigate(Routes.AUTH) { popUpTo(0) }
                    },
                )
            }
            composable(Routes.AUTH) {
                val vm: AuthViewModel = kodaViewModel { ctx -> AuthViewModel(AuthRepository(ctx)) }
                AuthScreen(
                    viewModel = vm,
                    onSignedIn = {
                        navController.navigate(Routes.HOME) { popUpTo(0) }
                    },
                )
            }
            composable(Routes.HOME) {
                val vm: HomeViewModel = kodaViewModel { ctx ->
                    HomeViewModel(DashboardRepository(ctx), AuthRepository(ctx))
                }
                HomeScreen(
                    viewModel = vm,
                    onOpenPractice = {
                        navController.navigate(Routes.PRACTICE) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
            composable(Routes.PRACTICE) {
                val vm: PracticeViewModel = kodaViewModel { ctx -> PracticeViewModel(ctx) }
                PracticeScreen(
                    viewModel = vm,
                    onSessionExpired = {
                        navController.navigate(Routes.AUTH) { popUpTo(0) }
                    },
                )
            }
            composable(Routes.PROFILE) {
                val vm: ProfileViewModel = kodaViewModel { ctx -> ProfileViewModel(AuthRepository(ctx)) }
                ProfileScreen(
                    viewModel = vm,
                    onSignedOut = {
                        navController.navigate(Routes.AUTH) { popUpTo(0) }
                    },
                )
            }
        }
    }
}

@Composable
private fun BottomNavItem(
    route: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    currentRoute: String?,
    navController: androidx.navigation.NavHostController,
) {
    NavigationBarItem(
        selected = currentRoute == route,
        onClick = {
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        icon = { Icon(icon, contentDescription = label) },
        label = { Text(label) },
    )
}
