package com.mna.streaming.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mna.streaming.MAApplication
import com.mna.streaming.data.model.Movie
import com.mna.streaming.data.repository.MovieRepository
import com.mna.streaming.network.models.ApiCastMember
import com.mna.streaming.network.models.ApiReview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val isLoading: Boolean = true,
    val movie: Movie? = null,
    val cast: List<ApiCastMember> = emptyList(),
    val inWatchlist: Boolean = false,
    val reviews: List<ApiReview> = emptyList(),
    val isWatchlistLoading: Boolean = false,
    // Rating dialog
    val showRatingDialog: Boolean = false,
    val userReview: ApiReview? = null,
    val currentUserId: String? = null,
    val error: String? = null
)

class DetailViewModel(
    private val movieRepository: MovieRepository,
    private val movieId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Resolve current user id once (for identifying own review)
                val currentUserId = MAApplication.sessionManager.getSavedUser()?.id

                // Fire movie detail, watchlist status, and reviews in parallel
                val movieDeferred     = async { movieRepository.getApiMovieById(movieId) }
                val watchlistDeferred = async { movieRepository.getWatchlistStatus(movieId) }
                val reviewsDeferred   = async { movieRepository.getReviews(movieId) }

                val apiMovie    = movieDeferred.await()
                val inWatchlist = watchlistDeferred.await()
                val reviews     = reviewsDeferred.await()

                // Find the signed-in user's own review so Edit/Delete can be surfaced
                val userReview = reviews.firstOrNull { it.user.id == currentUserId }

                _uiState.update {
                    it.copy(
                        isLoading     = false,
                        movie         = apiMovie.toMovie(),
                        cast          = apiMovie.cast?.sortedBy { c -> c.order } ?: emptyList(),
                        inWatchlist   = inWatchlist,
                        reviews       = reviews,
                        userReview    = userReview,
                        currentUserId = currentUserId
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error     = e.message ?: "Failed to load movie"
                    )
                }
            }
        }
    }

    fun toggleWatchlist() {
        viewModelScope.launch {
            _uiState.update { it.copy(isWatchlistLoading = true) }
            try {
                val movie = _uiState.value.movie
                val newState = if (movie != null) {
                    // Full overload: also mirrors the change in local storage
                    movieRepository.toggleWatchlist(movie)
                } else {
                    movieRepository.toggleWatchlist(movieId)
                }
                _uiState.update { it.copy(inWatchlist = newState, isWatchlistLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isWatchlistLoading = false) }
            }
        }
    }

    /**
     * Record that the user started watching this movie.
     * Writes a local watch-history entry so the Profile → Watch History tab
     * shows this film immediately, without needing a server GET endpoint.
     *
     * Uses [MAApplication.appScope] instead of [viewModelScope] so the write
     * is guaranteed to complete even if the ViewModel is cleared before the
     * coroutine finishes (e.g. rapid back-navigation after hitting Play).
     */
    fun recordWatched() {
        val movie = _uiState.value.movie ?: return
        MAApplication.appScope.launch {
            movieRepository.saveLocalWatchHistory(movie)
        }
    }

    fun showRatingDialog() {
        _uiState.update { it.copy(showRatingDialog = true) }
    }

    fun dismissRatingDialog() {
        _uiState.update { it.copy(showRatingDialog = false) }
    }

    fun submitReview(rating: Int, comment: String?) {
        viewModelScope.launch {
            try {
                val review = movieRepository.submitReview(movieId, rating, comment)
                // Refresh reviews list and close dialog
                val reviews = movieRepository.getReviews(movieId)
                _uiState.update {
                    it.copy(
                        showRatingDialog = false,
                        userReview       = review,
                        reviews          = reviews
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(showRatingDialog = false) }
            }
        }
    }

    fun deleteReview(reviewId: String) {
        viewModelScope.launch {
            try {
                movieRepository.deleteReview(reviewId)
                // Refresh the reviews list and clear the stored user review
                val reviews = movieRepository.getReviews(movieId)
                val currentUserId = _uiState.value.currentUserId
                val userReview = reviews.firstOrNull { it.user.id == currentUserId }
                _uiState.update {
                    it.copy(reviews = reviews, userReview = userReview)
                }
            } catch (_: Exception) {
                // Best-effort — swallow; the review card stays visible until next load
            }
        }
    }

    companion object {
        fun factory(movieId: String): ViewModelProvider.Factory = viewModelFactory {
            initializer { DetailViewModel(MAApplication.movieRepository, movieId) }
        }
    }
}
