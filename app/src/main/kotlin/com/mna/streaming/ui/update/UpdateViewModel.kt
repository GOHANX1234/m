package com.mna.streaming.ui.update

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mna.streaming.BuildConfig
import com.mna.streaming.MAApplication
import com.mna.streaming.network.models.VersionCheckResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Possible states of the update check. */
sealed class UpdateState {
    /** Check is in progress or hasn't run yet — show nothing. */
    data object Idle : UpdateState()

    /** No update needed, or device not in rollout slice — proceed normally. */
    data object NoUpdate : UpdateState()

    /**
     * An update is available and the user may dismiss the dialog.
     * @param response Full server response, used to populate the dialog.
     */
    data class OptionalUpdate(val response: VersionCheckResponse) : UpdateState()

    /**
     * App is blocked — either [forceUpdate] is true or [currentVersionSupported] is false.
     * The dialog cannot be dismissed.
     * @param response Full server response, used to populate the dialog.
     * @param reason   "tooOld" when the version is below minSupportedVersionCode,
     *                 "forced"  when the admin set forceUpdate = true.
     */
    data class ForceUpdate(val response: VersionCheckResponse, val reason: String) : UpdateState()
}

class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MAApplication.updateRepository

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    /**
     * Runs the version check against the Sarrows update API.
     * Silently swallows network errors — the app must never be blocked due to
     * a connectivity issue; only an explicit server response can block.
     */
    fun checkForUpdate() {
        viewModelScope.launch {
            val context: Context = getApplication()
            val result = repository.checkForUpdate(BuildConfig.VERSION_CODE)

            result.onSuccess { response ->
                _state.value = when {
                    // Hard block — version below minimum supported
                    !response.currentVersionSupported ->
                        UpdateState.ForceUpdate(response, reason = "tooOld")

                    response.updateAvailable -> {
                        val inRollout = repository.isInRollout(response.rolloutPercentage, context)
                        when {
                            response.forceUpdate -> UpdateState.ForceUpdate(response, reason = "forced")
                            inRollout            -> UpdateState.OptionalUpdate(response)
                            else                 -> UpdateState.NoUpdate // not in rollout slice
                        }
                    }

                    // Up to date
                    else -> UpdateState.NoUpdate
                }
            }

            // Network / parse failure → fail silently, let the app continue
            result.onFailure {
                _state.value = UpdateState.NoUpdate
            }
        }
    }

    /** Called when the user dismisses an optional update dialog. */
    fun dismissOptionalUpdate() {
        _state.value = UpdateState.NoUpdate
    }
}
