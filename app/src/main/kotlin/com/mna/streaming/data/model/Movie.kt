package com.mna.streaming.data.model

/**
 * App-side movie model — a clean, UI-friendly view of the API data.
 * Mapped from [com.mna.streaming.network.models.ApiMovie] via [toMovie()].
 */
data class Movie(
    val id: String,               // MongoDB _id (24-char hex)
    val title: String,
    val description: String,
    val genres: List<String>,     // genre names only
    val year: Int,                // releaseYear
    val rating: Double,           // 0–10, one decimal
    val ratingCount: Int,
    val durationSeconds: Int,     // raw seconds from API (e.g. 8460 = 2h 21m)
    val posterUrl: String,
    val backdropUrl: String,      // bannerUrl, falls back to posterUrl
    val trailerUrl: String?,
    val views: Int
) {
    /** Human-readable duration, e.g. "2h 21m" or "45m". */
    val durationFormatted: String
        get() {
            val h = durationSeconds / 3600
            val m = (durationSeconds % 3600) / 60
            return if (h > 0) "${h}h ${m}m" else "${m}m"
        }
}

/** Home-screen grouping of movies under a section header. */
data class Category(
    val id: String,
    val title: String,
    val movies: List<Movie>
)
