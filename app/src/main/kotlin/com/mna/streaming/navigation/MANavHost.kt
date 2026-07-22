package com.mna.streaming.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mna.streaming.ui.auth.AuthViewModel
import com.mna.streaming.ui.auth.LoginScreen
import com.mna.streaming.ui.auth.SignupScreen
import com.mna.streaming.ui.detail.DetailScreen
import com.mna.streaming.ui.home.HomeScreen
import com.mna.streaming.ui.home.HomeViewModel
import com.mna.streaming.ui.profile.ProfileScreen
import com.mna.streaming.ui.search.SearchScreen
import com.mna.streaming.ui.search.SearchViewModel

// ── Route definitions ─────────────────────────────────────────────────────────

sealed class Screen(val route: String) {
    object Login   : Screen("login")
    object Signup  : Screen("signup")
    object Home    : Screen("home")
    object Search  : Screen("search")
    object Profile : Screen("profile")
    object Detail  : Screen("detail/{movieId}") {
        fun createRoute(movieId: String) = "detail/$movieId"
    }
}

// ── Root nav host ─────────────────────────────────────────────────────────────

@Composable
fun MANavHost() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
    val uiState by authViewModel.uiState.collectAsState()

    // ── Auth-driven navigation ────────────────────────────────────────────────

    LaunchedEffect(uiState.isSessionChecked, uiState.currentUser) {
        if (!uiState.isSessionChecked) return@LaunchedEffect

        val currentRoute = navController.currentBackStackEntry?.destination?.route

        if (uiState.currentUser != null) {
            if (currentRoute == Screen.Login.route || currentRoute == Screen.Signup.route) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else {
            if (currentRoute != Screen.Login.route && currentRoute != Screen.Signup.route) {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    // ── Nav graph ─────────────────────────────────────────────────────────────

    NavHost(
        navController    = navController,
        startDestination = Screen.Login.route
    ) {

        // ── Login ─────────────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            LoginScreen(
                uiState            = uiState,
                onLogin            = { email, password -> authViewModel.login(email, password) },
                onNavigateToSignup = { navController.navigate(Screen.Signup.route) },
                onClearErrors      = { authViewModel.clearLoginErrors() }
            )
        }

        // ── Signup ────────────────────────────────────────────────────────────
        composable(Screen.Signup.route) {
            SignupScreen(
                uiState           = uiState,
                onSignUp          = { nickname, email, password ->
                    authViewModel.signUp(nickname, email, password)
                },
                onNavigateToLogin = { navController.popBackStack() },
                onClearErrors     = { authViewModel.clearSignupErrors() }
            )
        }

        // ── Home ──────────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            // HomeViewModel is scoped to this back-stack entry for clean state
            val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
            HomeScreen(
                onMovieClick   = { movie ->
                    navController.navigate(Screen.Detail.createRoute(movie.id))
                },
                onSearchClick  = { navController.navigate(Screen.Search.route) },
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                homeViewModel  = homeViewModel
            )
        }

        // ── Profile ───────────────────────────────────────────────────────────
        composable(Screen.Profile.route) {
            ProfileScreen(
                onSignOut    = { authViewModel.signOut() },
                onBackClick  = { navController.popBackStack() },
                onMovieClick = { movieId ->
                    navController.navigate(Screen.Detail.createRoute(movieId))
                }
            )
        }

        // ── Search ────────────────────────────────────────────────────────────
        composable(Screen.Search.route) {
            val searchViewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory)
            SearchScreen(
                onMovieClick    = { movie ->
                    navController.navigate(Screen.Detail.createRoute(movie.id))
                },
                onBackClick     = { navController.popBackStack() },
                searchViewModel = searchViewModel
            )
        }

        // ── Detail ────────────────────────────────────────────────────────────
        composable(
            route     = Screen.Detail.route,
            arguments = listOf(navArgument("movieId") { type = NavType.StringType })
        ) { backStack ->
            val movieId = backStack.arguments?.getString("movieId") ?: return@composable
            DetailScreen(
                movieId     = movieId,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
