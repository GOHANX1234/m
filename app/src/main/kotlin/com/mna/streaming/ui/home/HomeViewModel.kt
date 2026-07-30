package com.mna.streaming.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mna.streaming.MAApplication
import com.mna.streaming.data.model.ActorItem
import com.mna.streaming.data.model.Category
import com.mna.streaming.data.model.FeaturedItem
import com.mna.streaming.data.model.Movie
import com.mna.streaming.data.repository.AnimeRepository
import com.mna.streaming.data.repository.MovieRepository
import com.mna.streaming.network.models.ApiAnime
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean              = true,
    val featuredItems: List<FeaturedItem> = emptyList(),
    val categories: List<Category>      = emptyList(),
    /** Shuffled actor list — rank numbers change on every load. */
    val actors: List<ActorItem>     = emptyList(),
    /** Genre names derived from all loaded movies. */
    val availableGenres: List<String> = emptyList(),
    /** Currently selected genre chip; null = "All". */
    val selectedGenre: String?      = null,
    /** Movies fetched for the active genre filter. */
    val genreMovies: List<Movie>    = emptyList(),
    val isGenreLoading: Boolean     = false,
    /** Web series fetched from /api/series for the "Web Series" row. */
    val webSeries: List<ApiAnime>   = emptyList(),
    val error: String?              = null
)

class HomeViewModel(
    private val movieRepository: MovieRepository,
    private val animeRepository: AnimeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    fun loadHome() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Fetch home categories, actors, and web series concurrently.
                val categoriesDeferred = async { movieRepository.getHomeCategories() }
                val actorsDeferred     = async { movieRepository.getActors(limit = 9) }
                val webSeriesDeferred  = async {
                    try { animeRepository.getWebSeries(sort = "latest", limit = 20).series }
                    catch (_: Exception) { emptyList() }
                }

                val (featuredMovies, categories) = categoriesDeferred.await()
                val movieActors = actorsDeferred.await()
                val webSeries   = webSeriesDeferred.await()

                // Featured: interleave latest movies and latest web series, shuffle, cap at 6.
                val featuredItems: List<FeaturedItem> = buildList {
                    addAll(featuredMovies.map { FeaturedItem.MovieFeatured(it) })
                    addAll(webSeries.take(3).map { FeaturedItem.SeriesFeatured(it) })
                }.shuffled().take(6)

                // Top Actors: merge web-series cast into the movie-actors pool, re-shuffle.
                val seen = movieActors
                    .map { it.name.lowercase(java.util.Locale.ROOT) }
                    .toMutableSet()
                val combined = movieActors.toMutableList()
                for (series in webSeries) {
                    for (cast in series.cast.orEmpty()) {
                        if (cast.name.isNotBlank() &&
                            seen.add(cast.name.lowercase(java.util.Locale.ROOT))
                        ) {
                            combined += ActorItem(
                                name  = cast.name,
                                image = cast.image?.takeIf { it.isNotBlank() },
                                rank  = 0
                            )
                        }
                    }
                }
                val actors = combined.shuffled().take(9)
                    .mapIndexed { i, a -> a.copy(rank = i + 1) }

                // Derive available genres from all loaded movies (no extra network call).
                val genres = categories
                    .flatMap { it.movies }
                    .flatMap { it.genres }
                    .groupBy { it }
                    .entries
                    .sortedByDescending { it.value.size }
                    .map { it.key }
                    .distinct()

                _uiState.update {
                    it.copy(
                        isLoading       = false,
                        featuredItems   = featuredItems,
                        categories      = categories,
                        actors          = actors,
                        webSeries       = webSeries,
                        availableGenres = genres,
                        selectedGenre   = null,
                        genreMovies     = emptyList()
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error     = e.message ?: "Failed to load movies"
                    )
                }
            }
        }
    }

    /**
     * Select a genre filter chip.  Pass null to clear the filter ("All").
     * The genre row is fetched fresh on every selection so it always reflects
     * the latest content.
     */
    fun selectGenre(genre: String?) {
        _uiState.update {
            it.copy(
                selectedGenre  = genre,
                genreMovies    = emptyList(),
                isGenreLoading = genre != null
            )
        }
        if (genre == null) return
        viewModelScope.launch {
            try {
                val movies = movieRepository.getMoviesByGenre(genre)
                _uiState.update { it.copy(genreMovies = movies, isGenreLoading = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isGenreLoading = false) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    MAApplication.movieRepository,
                    MAApplication.animeRepository
                )
            }
        }
    }
}
