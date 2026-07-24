package com.mna.streaming

import android.app.Application
import com.mna.streaming.data.LocalProfileStore
import com.mna.streaming.data.SessionManager
import com.mna.streaming.data.repository.AuthRepository
import com.mna.streaming.data.repository.MovieRepository
import com.mna.streaming.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Application class — initialises the dependency graph exactly once.
 *
 * No DI framework is used. Components are created here and accessed through
 * the companion object by ViewModels and Activities.
 */
class MAApplication : Application() {

    companion object {
        lateinit var sessionManager: SessionManager
            private set

        lateinit var localProfileStore: LocalProfileStore
            private set

        lateinit var apiClient: ApiClient
            private set

        lateinit var authRepository: AuthRepository
            private set

        lateinit var movieRepository: MovieRepository
            private set

        /**
         * Application-level coroutine scope for fire-and-forget writes that must
         * survive ViewModel lifecycle (e.g. watch-history persistence).
         * Uses SupervisorJob so a child failure never cancels the whole scope.
         */
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    }

    override fun onCreate() {
        super.onCreate()

        sessionManager   = SessionManager(applicationContext)
        localProfileStore = LocalProfileStore(applicationContext)
        apiClient        = ApiClient(sessionManager)
        authRepository   = AuthRepository(apiClient, sessionManager, localProfileStore)
        movieRepository  = MovieRepository(apiClient, localProfileStore)
    }
}
