package com.mna.streaming.ui.anime

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mna.streaming.MAApplication
import com.mna.streaming.data.repository.AnimeRepository
import com.mna.streaming.network.models.ApiAdminEpisode
import com.mna.streaming.network.models.ApiAnime
import com.mna.streaming.network.models.ApiReview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AnimeDetailUiState(
    val isLoading: Boolean                           = true,
    val anime: ApiAnime?                             = null,
    val episodes: List<ApiAdminEpisode>              = emptyList(),
    val episodesBySeason: Map<Int, List<ApiAdminEpisode>> = emptyMap(),
    val reviews: List<ApiReview>                     = emptyList(),
    val inWatchlist: Boolean                         = false,
    val isTogglingWatchlist: Boolean                 = false,
    val isSubmittingReview: Boolean                  = false,
    val error: String?                               = null,
    /** Populated after main detail loads — won't block screen display. */
    val similarAnime: List<ApiAnime>                 = emptyList(),
    // Review submission feedback
    val reviewSuccess: Boolean                       = false,
    val reviewError: String?                         = null
)

class AnimeDetailViewModel(
    private val animeId: String,
    private val animeRepository: AnimeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnimeDetailUiState())
    val uiState: StateFlow<AnimeDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    // ── Public API ────────────────────────────────────────────────────────────

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Try the in-memory cache first (populated when the user browses the
                // anime / series list).  On a cold-start from a notification tap the
                // cache is empty, so fall back to a real network fetch.
                val anime = animeRepository.getCachedAnime(animeId)
                    ?: animeRepository.getSeriesById(animeId)

                // Fire all remaining requests in parallel now that we have the series
                val episodesDeferred  = async { animeRepository.getEpisodesForSeries(animeId) }
                val reviewsDeferred   = async { animeRepository.getReviews(animeId) }
                val watchlistDeferred = async {
                    runCatching { animeRepository.getWatchlistStatus(animeId) }.getOrDefault(false)
                }

                val episodes    = episodesDeferred.await()
                val reviews     = reviewsDeferred.await()
                val inWatchlist = watchlistDeferred.await()

                // Group episodes by season and sort within each season
                val bySeason = episodes
                    .sortedWith(compareBy({ it.season }, { it.episodeNumber }))
                    .groupBy { it.season }

                // Record series view (best-effort, fire-and-forget)
                launch { animeRepository.trackSeriesView(animeId) }

                // Paint the screen immediately — don't wait for similar anime
                _uiState.update {
                    it.copy(
                        isLoading        = false,
                        anime            = anime,
                        episodes         = episodes,
                        episodesBySeason = bySeason,
                        reviews          = reviews,
                        inWatchlist      = inWatchlist
                    )
                }

                // Fetch similar anime in the background so the screen is already visible
                val genres = anime.genres.map { it.name }
                if (genres.isNotEmpty()) {
                    val similar = animeRepository.getSimilarAnime(animeId, genres)
                    _uiState.update { it.copy(similarAnime = similar) }
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = e.message ?: "Failed to load details")
                }
            }
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTogglingWatchlist = true) }
            try {
                val newState = animeRepository.toggleWatchlist(animeId)
                _uiState.update { it.copy(inWatchlist = newState, isTogglingWatchlist = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isTogglingWatchlist = false) }
            }
        }
    }

    fun submitReview(rating: Int, comment: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingReview = true, reviewError = null, reviewSuccess = false) }
            try {
                val review = animeRepository.submitReview(animeId, rating, comment)
                // Update the reviews list: replace existing review by this user or prepend new one
                val updatedReviews = _uiState.value.reviews.toMutableList()
                val existingIdx = updatedReviews.indexOfFirst { it.user.id == review.user.id }
                if (existingIdx >= 0) updatedReviews[existingIdx] = review else updatedReviews.add(0, review)
                _uiState.update {
                    it.copy(isSubmittingReview = false, reviewSuccess = true, reviews = updatedReviews)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSubmittingReview = false, reviewError = e.message ?: "Failed to submit review")
                }
            }
        }
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            try {
                animeRepository.deleteReview(reviewId)
                val updated = _uiState.value.reviews.filter { it.id != reviewId }
                _uiState.update { it.copy(reviews = updated) }
            } catch (_: Exception) { /* best-effort */ }
        }
    }

    fun clearReviewFeedback() {
        _uiState.update { it.copy(reviewSuccess = false, reviewError = null) }
    }

    companion object {
        fun factory(animeId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AnimeDetailViewModel(animeId, MAApplication.animeRepository)
            }
        }
    }
}
