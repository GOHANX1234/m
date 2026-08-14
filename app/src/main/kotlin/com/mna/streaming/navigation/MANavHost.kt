package com.mna.streaming.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalMovies
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.LocalMovies
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mna.streaming.ui.actor.ActorMoviesScreen
import com.mna.streaming.ui.admin.AdminNavHost
import com.mna.streaming.ui.anime.AnimeDetailScreen
import com.mna.streaming.ui.anime.AnimeScreen
import com.mna.streaming.ui.anime.AnimeViewModel
import com.mna.streaming.ui.auth.AuthViewModel
import com.mna.streaming.ui.auth.LoginScreen
import com.mna.streaming.ui.auth.SignupScreen
import com.mna.streaming.ui.detail.DetailScreen
import com.mna.streaming.ui.home.HomeScreen
import com.mna.streaming.ui.home.HomeViewModel
import com.mna.streaming.ui.profile.ProfileScreen
import com.mna.streaming.ui.search.SearchScreen
import com.mna.streaming.ui.search.SearchViewModel
import com.mna.streaming.ui.theme.MADark
import com.mna.streaming.ui.theme.MARed
import com.mna.streaming.ui.theme.MASurface
import com.mna.streaming.ui.theme.MATextSecondary
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

// â”€â”€ Route definitions â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

sealed class Screen(val route: String) {
    object Login       : Screen("login")
    object Signup      : Screen("signup")
    object Main        : Screen("main")     // hosts the bottom-nav scaffold
    object Search      : Screen("search")
    object Profile     : Screen("profile")
    object Admin       : Screen("admin")
    object Detail      : Screen("detail/{movieId}") {
        fun createRoute(movieId: String) = "detail/$movieId"
    }
    object AnimeDetail : Screen("anime_detail/{animeId}") {
        fun createRoute(animeId: String) = "anime_detail/$animeId"
    }
    object ActorMovies : Screen("actor/{actorName}") {
        /** URI-encodes the actor name so spaces and special characters survive navigation. */
        fun createRoute(actorName: String) = "actor/${Uri.encode(actorName)}"
    }
}

// â”€â”€ Bottom nav tab model â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

private enum class MainTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    Movies("Movies", Icons.Filled.LocalMovies,  Icons.Outlined.LocalMovies),
    Anime ("Anime",  Icons.Filled.PlayCircle,   Icons.Outlined.PlayCircle)
}

// â”€â”€ Root nav host â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

/**
 * @param pendingDeepLink Optional notification deep link extracted from the launching Intent.
 *   A [Pair] of (contentType, contentId) where contentType is "movie", "anime", or "series".
 *   Processed exactly once after the user is confirmed to be authenticated.
 */
@Composable
fun MANavHost(
    authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory),
    pendingDeepLink: Pair<String, String>? = null
) {
    val navController = rememberNavController()
    val uiState by authViewModel.uiState.collectAsState()

    val startDestination = remember {
        if (uiState.currentUser != null) Screen.Main.route else Screen.Login.route
    }

    // â”€â”€ Auth-driven navigation â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    LaunchedEffect(uiState.isSessionChecked, uiState.currentUser) {
        if (!uiState.isSessionChecked) return@LaunchedEffect

        val currentRoute = navController.currentBackStackEntry?.destination?.route

        if (uiState.currentUser != null) {
            if (currentRoute == Screen.Login.route || currentRoute == Screen.Signup.route) {
                navController.navigate(Screen.Main.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        } else {
            val authRoutes = setOf(Screen.Login.route, Screen.Signup.route)
            if (currentRoute != null && currentRoute !in authRoutes) {
                navController.navigate(Screen.Login.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    // â”€â”€ Notification deep-link handler â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    //
    // Fires once after both conditions are true:
    //   1. A pending deep link exists (app was launched by tapping a notification).
    //   2. The user is confirmed authenticated and the main screen is in the back-stack.
    //
    // The deepLinkConsumed flag prevents re-navigation on recomposition.

    var deepLinkConsumed by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSessionChecked, uiState.currentUser, deepLinkConsumed) {
        if (pendingDeepLink == null) return@LaunchedEffect
        if (deepLinkConsumed) return@LaunchedEffect
        if (!uiState.isSessionChecked) return@LaunchedEffect
        if (uiState.currentUser == null) return@LaunchedEffect

        // Ensure the main screen (bottom-nav host) is visible first, then push the
        // detail screen on top so the user can navigate back naturally.
        val currentRoute = navController.currentBackStackEntry?.destination?.route
        if (currentRoute != Screen.Main.route) {
            navController.navigate(Screen.Main.route) {
                popUpTo(0) { inclusive = true }
            }
        }

        val (contentType, contentId) = pendingDeepLink
        when (contentType) {
            "movie"  -> navController.navigate(Screen.Detail.createRoute(contentId))
            "anime"  -> navController.navigate(Screen.AnimeDetail.createRoute(contentId))
            "series" -> navController.navigate(Screen.AnimeDetail.createRoute(contentId))
        }
        deepLinkConsumed = true
    }

    // â”€â”€ Nav graph â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    NavHost(
        navController    = navController,
        startDestination = startDestination
    ) {

        // â”€â”€ Login â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        composable(Screen.Login.route) {
            LoginScreen(
                uiState            = uiState,
                onLogin            = { email, password -> authViewModel.login(email, password) },
                onNavigateToSignup = { navController.navigate(Screen.Signup.route) },
                onClearErrors      = { authViewModel.clearLoginErrors() }
            )
        }

        // â”€â”€ Signup â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

        // â”€â”€ Main (bottom nav scaffold) â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        composable(Screen.Main.route) {
            MainScreen(
                onMovieClick   = { movieId -> navController.navigate(Screen.Detail.createRoute(movieId)) },
                onAnimeClick   = { animeId -> navController.navigate(Screen.AnimeDetail.createRoute(animeId)) },
                onSearchClick  = { navController.navigate(Screen.Search.route) },
                onProfileClick = { navController.navigate(Screen.Profile.route) },
                onActorClick   = { actorName -> navController.navigate(Screen.ActorMovies.createRoute(actorName)) }
            )
        }

        // â”€â”€ Profile â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        composable(Screen.Profile.route) {
            ProfileScreen(
                onSignOut    = { authViewModel.signOut() },
                onBackClick  = { navController.popBackStack() },
                onMovieClick = { movieId ->
                    navController.navigate(Screen.Detail.createRoute(movieId))
                },
                onAnimeClick = { animeId ->
                    navController.navigate(Screen.AnimeDetail.createRoute(animeId))
                },
                onAdminClick = { navController.navigate(Screen.Admin.route) }
            )
        }

        // â”€â”€ Admin panel â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        composable(Screen.Admin.route) {
            AdminNavHost(onExit = { navController.popBackStack() })
        }

        // â”€â”€ Search â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        composable(Screen.Search.route) {
            val searchViewModel: SearchViewModel = viewModel(factory = SearchViewModel.Factory)
            SearchScreen(
                onMovieClick    = { movieId ->
                    navController.navigate(Screen.Detail.createRoute(movieId))
                },
                onAnimeClick    = { animeId ->
                    navController.navigate(Screen.AnimeDetail.createRoute(animeId))
                },
                onBackClick     = { navController.popBackStack() },
                searchViewModel = searchViewModel
            )
        }

        // â”€â”€ Movie detail â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        composable(
            route     = Screen.Detail.route,
            arguments = listOf(navArgument("movieId") { type = NavType.StringType })
        ) { backStack ->
            val movieId = backStack.arguments?.getString("movieId") ?: return@composable
            DetailScreen(
                movieId      = movieId,
                onBackClick  = { navController.popBackStack() },
                onMovieClick = { id -> navController.navigate(Screen.Detail.createRoute(id)) }
            )
        }

        // â”€â”€ Anime / web-series detail â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        composable(
            route     = Screen.AnimeDetail.route,
            arguments = listOf(navArgument("animeId") { type = NavType.StringType })
        ) { backStack ->
            val animeId = backStack.arguments?.getString("animeId") ?: return@composable
            AnimeDetailScreen(
                animeId      = animeId,
                onBackClick  = { navController.popBackStack() },
                onAnimeClick = { id -> navController.navigate(Screen.AnimeDetail.createRoute(id)) }
            )
        }

        // â”€â”€ Actor filmography â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        composable(
            route     = Screen.ActorMovies.route,
            arguments = listOf(navArgument("actorName") { type = NavType.StringType })
        ) { backStack ->
            val actorName = backStack.arguments?.getString("actorName") ?: return@composable
            ActorMoviesScreen(
                actorName    = actorName,
                onBackClick  = { navController.popBackStack() },
                onMovieClick = { movieId -> navController.navigate(Screen.Detail.createRoute(movieId)) },
                onAnimeClick = { animeId -> navController.navigate(Screen.AnimeDetail.createRoute(animeId)) }
            )
        }
    }
}

// â”€â”€ Main screen with bottom nav â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

@Composable
private fun MainScreen(
    onMovieClick:   (String) -> Unit,
    onAnimeClick:   (String) -> Unit,
    onSearchClick:  () -> Unit,
    onProfileClick: () -> Unit,
    onActorClick:   (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(MainTab.Movies) }

    // Keep ViewModel instances alive across tab switches
    val homeViewModel:  HomeViewModel  = viewModel(factory = HomeViewModel.Factory)
    val animeViewModel: AnimeViewModel = viewModel(factory = AnimeViewModel.Factory)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MADark)
    ) {
        // â”€â”€ Tab content â€” fills the full screen; pill floats over it â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        // Movies tab
        AnimatedVisibility(
            visible = selectedTab == MainTab.Movies,
            enter   = fadeIn(tween(280)) + scaleIn(initialScale = 0.96f, animationSpec = tween(280)),
            exit    = fadeOut(tween(200)) + scaleOut(targetScale = 0.96f, animationSpec = tween(200))
        ) {
            HomeScreen(
                onMovieClick   = { movie -> onMovieClick(movie.id) },
                onSearchClick  = onSearchClick,
                onProfileClick = onProfileClick,
                onActorClick   = onActorClick,
                onSeriesClick  = { seriesId -> onAnimeClick(seriesId) },
                homeViewModel  = homeViewModel
            )
        }

        // Anime tab
        AnimatedVisibility(
            visible = selectedTab == MainTab.Anime,
            enter   = fadeIn(tween(280)) + scaleIn(initialScale = 0.96f, animationSpec = tween(280)),
            exit    = fadeOut(tween(200)) + scaleOut(targetScale = 0.96f, animationSpec = tween(200))
        ) {
            AnimeScreen(
                onAnimeClick   = { anime -> onAnimeClick(anime.id) },
                onSearchClick  = onSearchClick,
                animeViewModel = animeViewModel
            )
        }

        // â”€â”€ Floating pill nav bar â€” compact, spring-driven for a tactile,
        // "butter smooth" feel instead of the previous linear tween motion.
        LiquidMainNav(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

/** Floating glass navigation with a long-press-to-drag liquid indicator. */
@Composable
private fun LiquidMainNav(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = MainTab.entries
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val animationScope = rememberCoroutineScope()
    val settledIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    val position = remember { Animatable(settledIndex.toFloat()) }
    LaunchedEffect(settledIndex) {
        position.animateTo(
            settledIndex.toFloat(),
            spring(dampingRatio = 0.78f, stiffness = 520f)
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 14.dp)
            .width(204.dp)
            .height(64.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val longPress = awaitLongPressOrCancellation(down.id)

                    if (longPress == null) {
                        val x = down.position.x / size.width
                        onTabSelected(tabs[(x * tabs.size).toInt().coerceIn(tabs.indices)])
                        return@awaitEachGesture
                    }

                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    position.stop()
                    drag(longPress.id) { change ->
                        val fraction = (change.position.x / size.width * tabs.size - 0.5f)
                            .coerceIn(0f, (tabs.size - 1).toFloat())
                        launch { position.snapTo(fraction) }
                        change.consume()
                    }
                    val target = position.value.roundToInt().coerceIn(tabs.indices)
                    onTabSelected(tabs[target])
                    animationScope.launch {
                        position.animateTo(
                            target.toFloat(),
                            spring(dampingRatio = 0.78f, stiffness = 520f)
                        )
                    }
                }
            }
    ) {
        val cellWidth = (constraints.maxWidth - with(density) { 10.dp.roundToPx() })
            .toFloat() / tabs.size
        val x = position.value * cellWidth
        val stretch = 1f + (position.value - position.value.roundToInt()).absoluteValue * 0.10f

        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MASurface.copy(alpha = 0.72f),
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxSize().shadow(12.dp, RoundedCornerShape(32.dp),
                ambientColor = Color.Black.copy(alpha = 0.28f), spotColor = Color.Black.copy(alpha = 0.35f))
        ) {
            Box(Modifier.fillMaxSize().padding(5.dp)) {
                Box(
                    Modifier
                        .offset { IntOffset(x.roundToInt(), 0) }
                        .width(94.dp)
                        .fillMaxHeight()
                        .graphicsLayer { scaleX = stretch; alpha = 0.96f }
                        .background(MARed.copy(alpha = 0.18f), RoundedCornerShape(27.dp))
                )
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    tabs.forEachIndexed { index, tab ->
                        val distance = (position.value - index).absoluteValue
                        val active = distance < 0.55f
                        val scale by animateFloatAsState(
                            targetValue = if (active) 1f else 0.88f,
                            animationSpec = spring(dampingRatio = 0.72f, stiffness = 520f),
                            label = "liquidNavIcon$index"
                        )
                        Box(
                            Modifier.weight(1f).fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (active) tab.selectedIcon else tab.unselectedIcon,
                                contentDescription = tab.label,
                                tint = if (active) MARed else MATextSecondary,
                                modifier = Modifier.size(21.dp).graphicsLayer { scaleX = scale; scaleY = scale }
                            )
                        }
                    }
                }
            }
        }
    }
}
