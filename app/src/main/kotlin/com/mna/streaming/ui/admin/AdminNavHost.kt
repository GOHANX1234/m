package com.mna.streaming.ui.admin

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mna.streaming.ui.admin.appversions.AdminAppVersionFormScreen
import com.mna.streaming.ui.admin.appversions.AdminAppVersionsScreen
import com.mna.streaming.ui.admin.appversions.AdminAppVersionsViewModel
import com.mna.streaming.ui.admin.episodes.AdminEpisodesScreen
import com.mna.streaming.ui.admin.episodes.AdminEpisodesViewModel
import com.mna.streaming.ui.admin.genres.AdminGenresScreen
import com.mna.streaming.ui.admin.genres.AdminGenresViewModel
import com.mna.streaming.ui.admin.movies.AdminMovieFormScreen
import com.mna.streaming.ui.admin.movies.AdminMoviesScreen
import com.mna.streaming.ui.admin.movies.AdminMoviesViewModel
import com.mna.streaming.ui.admin.requests.AdminRequestsScreen
import com.mna.streaming.ui.admin.requests.AdminRequestsViewModel
import com.mna.streaming.ui.admin.series.AdminSeriesFormScreen
import com.mna.streaming.ui.admin.series.AdminSeriesScreen
import com.mna.streaming.ui.admin.series.AdminSeriesViewModel
import com.mna.streaming.ui.admin.users.AdminUsersScreen
import com.mna.streaming.ui.admin.users.AdminUsersViewModel

// ── Route definitions ─────────────────────────────────────────────────────────

sealed class AdminScreen(val route: String) {
    object Dashboard    : AdminScreen("admin_dashboard")
    object Movies       : AdminScreen("admin_movies")
    object MovieForm    : AdminScreen("admin_movie_form?movieId={movieId}") {
        fun create()          = "admin_movie_form?movieId="
        fun edit(id: String)  = "admin_movie_form?movieId=$id"
    }
    object Series       : AdminScreen("admin_series")
    object SeriesForm   : AdminScreen("admin_series_form?seriesId={seriesId}") {
        fun create()          = "admin_series_form?seriesId="
        fun edit(id: String)  = "admin_series_form?seriesId=$id"
    }
    object Episodes     : AdminScreen("admin_episodes/{seriesId}") {
        fun route(id: String) = "admin_episodes/$id"
    }
    object Genres       : AdminScreen("admin_genres")
    object Users        : AdminScreen("admin_users")
    object Requests     : AdminScreen("admin_requests")
    object AppVersions  : AdminScreen("admin_app_versions")
    object AppVersionForm : AdminScreen("admin_app_version_form?versionId={versionId}") {
        fun create()          = "admin_app_version_form?versionId="
        fun edit(id: String)  = "admin_app_version_form?versionId=$id"
    }
}

// ── Admin nav host ────────────────────────────────────────────────────────────

@Composable
fun AdminNavHost(onExit: () -> Unit) {
    val navController = rememberNavController()

    NavHost(
        navController    = navController,
        startDestination = AdminScreen.Dashboard.route
    ) {

        composable(AdminScreen.Dashboard.route) {
            AdminDashboardScreen(
                onMoviesClick     = { navController.navigate(AdminScreen.Movies.route) },
                onAnimeClick      = { navController.navigate(AdminScreen.Series.route) },
                onGenresClick     = { navController.navigate(AdminScreen.Genres.route) },
                onUsersClick      = { navController.navigate(AdminScreen.Users.route) },
                onRequestsClick   = { navController.navigate(AdminScreen.Requests.route) },
                onAppUpdatesClick = { navController.navigate(AdminScreen.AppVersions.route) },
                onBackClick       = onExit
            )
        }

        // ── Movies ────────────────────────────────────────────────────────────

        composable(AdminScreen.Movies.route) {
            val vm: AdminMoviesViewModel = viewModel(factory = AdminMoviesViewModel.Factory)
            AdminMoviesScreen(
                viewModel     = vm,
                onCreateClick = { navController.navigate(AdminScreen.MovieForm.create()) },
                onEditClick   = { id -> navController.navigate(AdminScreen.MovieForm.edit(id)) },
                onBackClick   = { navController.popBackStack() }
            )
        }

        composable(
            route     = AdminScreen.MovieForm.route,
            arguments = listOf(navArgument("movieId") {
                type         = NavType.StringType
                nullable     = true
                defaultValue = null
            })
        ) { back ->
            val movieId = back.arguments?.getString("movieId")?.takeIf { it.isNotBlank() }
            val vm: AdminMoviesViewModel = viewModel(factory = AdminMoviesViewModel.Factory)
            AdminMovieFormScreen(
                viewModel   = vm,
                editMovieId = movieId,
                onSaved     = { navController.popBackStack() },
                onBackClick = { navController.popBackStack() }
            )
        }

        // ── Series ────────────────────────────────────────────────────────────

        composable(AdminScreen.Series.route) {
            val vm: AdminSeriesViewModel = viewModel(factory = AdminSeriesViewModel.Factory)
            AdminSeriesScreen(
                viewModel       = vm,
                onCreateClick   = { navController.navigate(AdminScreen.SeriesForm.create()) },
                onEditClick     = { id -> navController.navigate(AdminScreen.SeriesForm.edit(id)) },
                onEpisodesClick = { id -> navController.navigate(AdminScreen.Episodes.route(id)) },
                onBackClick     = { navController.popBackStack() }
            )
        }

        composable(
            route     = AdminScreen.SeriesForm.route,
            arguments = listOf(navArgument("seriesId") {
                type         = NavType.StringType
                nullable     = true
                defaultValue = null
            })
        ) { back ->
            val seriesId = back.arguments?.getString("seriesId")?.takeIf { it.isNotBlank() }
            val vm: AdminSeriesViewModel = viewModel(factory = AdminSeriesViewModel.Factory)
            AdminSeriesFormScreen(
                viewModel    = vm,
                editSeriesId = seriesId,
                onSaved      = { navController.popBackStack() },
                onBackClick  = { navController.popBackStack() }
            )
        }

        // ── Episodes ──────────────────────────────────────────────────────────

        composable(
            route     = AdminScreen.Episodes.route,
            arguments = listOf(navArgument("seriesId") { type = NavType.StringType })
        ) { back ->
            val seriesId = back.arguments!!.getString("seriesId")!!
            val vm: AdminEpisodesViewModel = viewModel(factory = AdminEpisodesViewModel.Factory)
            AdminEpisodesScreen(
                viewModel   = vm,
                seriesId    = seriesId,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ── Genres ────────────────────────────────────────────────────────────

        composable(AdminScreen.Genres.route) {
            val vm: AdminGenresViewModel = viewModel(factory = AdminGenresViewModel.Factory)
            AdminGenresScreen(
                viewModel   = vm,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ── Users ─────────────────────────────────────────────────────────────

        composable(AdminScreen.Users.route) {
            val vm: AdminUsersViewModel = viewModel(factory = AdminUsersViewModel.Factory)
            AdminUsersScreen(
                viewModel   = vm,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ── Requests ──────────────────────────────────────────────────────────

        composable(AdminScreen.Requests.route) {
            val vm: AdminRequestsViewModel = viewModel(factory = AdminRequestsViewModel.Factory)
            AdminRequestsScreen(
                viewModel   = vm,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ── App Versions ──────────────────────────────────────────────────────

        composable(AdminScreen.AppVersions.route) {
            val vm: AdminAppVersionsViewModel = viewModel(factory = AdminAppVersionsViewModel.Factory)
            AdminAppVersionsScreen(
                viewModel     = vm,
                onCreateClick = { navController.navigate(AdminScreen.AppVersionForm.create()) },
                onEditClick   = { id -> navController.navigate(AdminScreen.AppVersionForm.edit(id)) },
                onBackClick   = { navController.popBackStack() }
            )
        }

        composable(
            route     = AdminScreen.AppVersionForm.route,
            arguments = listOf(navArgument("versionId") {
                type         = NavType.StringType
                nullable     = true
                defaultValue = null
            })
        ) { back ->
            val versionId = back.arguments?.getString("versionId")?.takeIf { it.isNotBlank() }
            val vm: AdminAppVersionsViewModel = viewModel(factory = AdminAppVersionsViewModel.Factory)
            AdminAppVersionFormScreen(
                viewModel     = vm,
                editVersionId = versionId,
                onSaved       = { navController.popBackStack() },
                onBackClick   = { navController.popBackStack() }
            )
        }
    }
}
