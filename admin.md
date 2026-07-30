# Sarrows — Admin Panel and Native Admin API

This is the implementation reference for building a native Android/iOS admin
client for Sarrows. It describes the admin panel, its API endpoints, request
and response shapes, validation rules, and the recommended content-management
workflows.

The source of truth for this document is the current Next.js API route and
model implementation. All paths are relative to the deployed Sarrows base URL.

---

## Table of Contents

1. [Admin panel map](#1-admin-panel-map)
2. [Authentication and authorization](#2-authentication-and-authorization)
3. [Base URL and request conventions](#3-base-url-and-request-conventions)
4. [Movies](#4-movies)
5. [Anime and web series](#5-anime-and-web-series)
6. [Episodes](#6-episodes)
7. [Genres](#7-genres)
8. [Users](#8-users)
9. [Content requests](#9-content-requests)
10. [TMDB metadata](#10-tmdb-metadata)
11. [AniList metadata](#11-anilist-metadata)
12. [Native app version management](#12-native-app-version-management)
13. [Data models](#13-data-models)
14. [Error reference](#14-error-reference)
15. [Recommended native admin workflows](#15-recommended-native-admin-workflows)
16. [Public app API and routes](#16-public-app-api-and-routes)

---

## 1. Admin panel map

The web admin panel is available at `/admin` and is protected by the same
server-side admin-role check as the API.

| Web screen | Route | Purpose |
|---|---|---|
| Dashboard | `/admin` | Counts for movies, series, users, published content, and pending requests |
| Content | `/admin/content` | Manage movies, anime, web series, genres, and episodes |
| Requests | `/admin/requests` | Review and update user content requests |
| Users | `/admin/users` | Search users and grant/revoke admin role |
| App updates | `/admin/updates` | Create and manage native app versions and rollout rules |

The Content screen has separate content types:

- **Movies** use the `Movie` collection and `status`.
- **Anime** use the `Series` collection with `type: "anime"` and
  `publishStatus`.
- **Web series** use the same `Series` collection with `type: "series"` and
  `publishStatus`.
- Anime and web series use the same episode endpoints.

The API does not have separate CRUD routes for anime and web series. The
`type` field is what distinguishes them.

---

## 2. Authentication and authorization

### 2.1 Admin requirement

Every endpoint under `/api/admin/*` requires an authenticated NextAuth session
whose database user has:

```json
{ "role": "admin" }
```

The check is performed on the server for every request. A missing session,
normal user session, or expired session returns:

```http
403 Forbidden
```

Do not rely only on hiding admin controls in the native UI.

### 2.2 Session mechanism

Sarrows uses NextAuth v5 with credentials authentication and JWT sessions.
The web app sends the session as a cookie. A native client must preserve and
send the authenticated session cookie on every admin request.

The current authentication endpoints are:

```http
POST /api/auth/signin
GET|POST /api/auth/*
GET /api/me
```

Credentials are:

| Field | Type | Required |
|---|---|---:|
| `email` | string | yes |
| `password` | string | yes |

The web client uses NextAuth's credentials provider rather than a bearer-token
API. Do not send an invented `Authorization: Bearer ...` token and expect the
admin routes to accept it.

The session role is re-read from MongoDB approximately every 60 seconds, so a
role change normally propagates without a fresh login.

### 2.3 Login security behavior

- Email is normalized to lowercase.
- After 10 consecutive failed attempts, the account is locked for 15 minutes.
- A locked account is reported like invalid credentials.
- A successful login resets the failed-attempt counter.

### 2.4 Session check

```http
GET /api/me
```

Example response:

```json
{
  "user": {
    "id": "6849a1c3f2e4b12d5e8f0123",
    "nickname": "sarrows_admin",
    "email": "admin@example.com",
    "image": null,
    "role": "admin",
    "joinedAt": "2025-01-01T00:00:00.000Z"
  },
  "stats": {
    "watchedCount": 0,
    "watchlistCount": 0
  }
}
```

Use this before opening a native admin session. The admin endpoints themselves
remain the final authorization check.

---

## 3. Base URL and request conventions

All examples use paths relative to the deployed Sarrows URL:

```text
https://your-deployed-domain.example
```

The native app should make the base URL configurable per environment.

### 3.1 Headers and cookies

For JSON requests:

```http
Content-Type: application/json
Cookie: <NextAuth session cookie>
```

Use a cookie jar or an HTTP client cookie store. Do not log session cookies or
video URLs.

### 3.2 IDs and timestamps

- MongoDB document IDs are 24-character hexadecimal ObjectIds.
- `createdAt` and `updatedAt` are ISO 8601 timestamps.
- Admin mutations identify resources by `_id`.
- Public page routes use a generated `slug`.
- Series episode IDs are MongoDB Episode `_id` values.

### 3.3 Pagination

Paginated responses use the endpoint-specific resource key plus:

```json
{
  "total": 120,
  "page": 1,
  "totalPages": 5
}
```

The default and maximum limits differ by resource:

| Endpoint | Default `limit` | Maximum |
|---|---:|---:|
| `/api/admin/movies` | 24 | 100 |
| `/api/admin/series` | 24 | 100 |
| `/api/admin/users` | 50 | 100 |
| `/api/admin/app-versions` | 50 | 100 |

Invalid or unsupported filter values are generally ignored and the default
filter is used; numeric page values are clamped to at least 1.

### 3.4 Common status codes

| Status | Meaning |
|---:|---|
| `200` | Successful read, update, or delete |
| `201` | Resource created |
| `400` | Invalid request, ID, query, or validation failure |
| `403` | Not authenticated as an admin |
| `404` | Resource or metadata record not found |
| `409` | Duplicate content, version code, or episode number |
| `500` | Server-side or upstream service error |

Error responses normally have this shape:

```json
{ "error": "Human-readable error message" }
```

---

## 4. Movies

Movies are stored in the `Movie` collection. Movie publication is controlled
by `status`, not `publishStatus`.

### 4.1 List movies for the admin panel

```http
GET /api/admin/movies
```

**Auth:** admin

Query parameters:

| Parameter | Type | Default | Values / notes |
|---|---|---|---|
| `page` | integer | `1` | Page number |
| `limit` | integer | `24` | Maximum 100 |
| `sort` | string | `latest` | `latest`, `oldest`, `views`, `rating`, `year`, `title` |
| `status` | string | all | `published` or `draft` |
| `q` | string | none | Case-insensitive title search |

Response:

```json
{
  "movies": [
    {
      "_id": "6849a1c3f2e4b12d5e8f0123",
      "title": "Example Movie",
      "slug": "example-movie",
      "description": "Description",
      "posterUrl": "https://cdn.example/poster.jpg",
      "bannerUrl": "https://cdn.example/banner.jpg",
      "trailerUrl": "https://cdn.example/trailer.mp4",
      "videoUrl": "https://cdn.example/movie.m3u8",
      "videoType": "hls",
      "externalId": "12345",
      "duration": 124,
      "releaseYear": 2025,
      "genres": [{ "_id": "6849...", "name": "Action" }],
      "cast": [],
      "rating": 8.5,
      "ratingCount": 10,
      "views": 0,
      "status": "draft",
      "createdAt": "2025-01-01T00:00:00.000Z",
      "updatedAt": "2025-01-01T00:00:00.000Z"
    }
  ],
  "total": 1,
  "page": 1,
  "totalPages": 1
}
```

Unlike public movie responses, this admin list explicitly includes
`videoUrl` and `videoType` so an admin can edit them.

### 4.2 Create a movie

```http
POST /api/admin/movies
```

**Auth:** admin

Request body:

| Field | Type | Required | Constraints |
|---|---|---:|---|
| `title` | string | yes | 1–200 characters |
| `description` | string | no | Maximum 2,000 characters |
| `posterUrl` | URL string | no | URL or `""` |
| `bannerUrl` | URL string | no | URL or `""` |
| `trailerUrl` | URL string | no | URL or `""` |
| `videoUrl` | URL string | no | URL or `""` |
| `videoType` | enum | no | `auto`, `hls`, `direct`, `embed` |
| `externalId` | string | no | TMDB ID when applicable |
| `duration` | positive integer | no | Runtime, usually minutes |
| `releaseYear` | integer | no | 1888 through current year + 5 |
| `genres` | string[] | no | Genre ObjectIds |
| `cast` | CastMember[] | no | See [cast model](#castmember) |
| `status` | enum | no | `published` or `draft`; defaults to `draft` |
| `rating` | number | no | 0–10 |

Example:

```json
{
  "title": "Example Movie",
  "description": "A movie description.",
  "posterUrl": "https://cdn.example/poster.jpg",
  "bannerUrl": "https://cdn.example/banner.jpg",
  "videoUrl": "https://cdn.example/movie.m3u8",
  "videoType": "hls",
  "releaseYear": 2025,
  "genres": ["6849a1c3f2e4b12d5e8f0123"],
  "status": "draft",
  "rating": 8.2
}
```

Response `201`:

```json
{ "movie": { "...": "created movie, including generated slug" } }
```

The server generates a unique slug. If the title slug already exists, a
timestamp suffix is used. A database duplicate can still return `409`.

### 4.3 Get one movie as an admin

```http
GET /api/movies/{id}
```

An authenticated admin receives drafts and the explicitly selected
`videoUrl`/`videoType` fields. A non-admin receives only published movies and
does not receive the real CDN video URL.

Response:

```json
{ "movie": { "...": "movie object" } }
```

### 4.4 Update a movie

```http
PATCH /api/movies/{id}
```

**Auth:** admin

The body accepts any subset of the create fields. The same validation rules
apply, but no field is required:

```json
{
  "status": "published",
  "videoUrl": "https://cdn.example/movie.m3u8",
  "videoType": "hls"
}
```

Response:

```json
{ "movie": { "...": "updated movie" } }
```

### 4.5 Delete a movie

```http
DELETE /api/movies/{id}
```

**Auth:** admin

Response:

```json
{ "success": true }
```

### 4.6 Movie metadata autofill

See [TMDB metadata](#10-tmdb-metadata). The admin UI searches TMDB, fills the
movie fields, resolves genre names to local Genre IDs, and can populate cast.

---

## 5. Anime and web series

Anime and web series share the `Series` collection and all series CRUD routes.
Use the `type` field exactly:

| `type` | Meaning | Metadata source | Public section |
|---|---|---|---|
| `anime` | Anime | AniList | `/anime` |
| `series` | Web series / TV series | TMDB TV | `/series` |

Series publication is controlled by `publishStatus`. This is intentionally
different from the movie field `status`.

### 5.1 List series for the admin panel

```http
GET /api/admin/series
```

**Auth:** admin

Query parameters:

| Parameter | Type | Default | Values / notes |
|---|---|---|---|
| `page` | integer | `1` | Page number |
| `limit` | integer | `24` | Maximum 100 |
| `sort` | string | `latest` | `latest`, `oldest`, `views`, `rating`, `title` |
| `type` | string | all | `anime` or `series` |
| `publishStatus` | string | all | `published` or `draft` |
| `status` | string | all | `ongoing` or `completed` |
| `q` | string | none | Case-insensitive title search |

Response:

```json
{
  "series": [
    {
      "_id": "6849a1c3f2e4b12d5e8f0123",
      "title": "Example Show",
      "slug": "example-show",
      "description": "Description",
      "posterUrl": "https://cdn.example/poster.jpg",
      "bannerUrl": "https://cdn.example/banner.jpg",
      "externalId": "1396",
      "totalSeasons": 5,
      "releaseYear": 2008,
      "genres": [{ "_id": "6849...", "name": "Drama" }],
      "cast": [],
      "status": "completed",
      "type": "series",
      "rating": 9,
      "ratingCount": 0,
      "views": 0,
      "publishStatus": "draft",
      "createdAt": "2025-01-01T00:00:00.000Z",
      "updatedAt": "2025-01-01T00:00:00.000Z"
    }
  ],
  "total": 1,
  "page": 1,
  "totalPages": 1
}
```

This endpoint includes drafts. Filter web series with:

```text
GET /api/admin/series?type=series
```

### 5.2 Create anime or web series

```http
POST /api/admin/series
```

**Auth:** admin

Request body:

| Field | Type | Required | Constraints |
|---|---|---:|---|
| `title` | string | yes | 1–200 characters |
| `description` | string | no | Maximum 2,000 characters |
| `posterUrl` | URL string | no | URL or `""` |
| `bannerUrl` | URL string | no | URL or `""` |
| `externalId` | string | no | AniList ID for anime, TMDB TV ID for web series |
| `totalSeasons` | positive integer | no | Number of seasons |
| `releaseYear` | integer | no | 1888 through current year + 5 |
| `genres` | string[] | no | Genre ObjectIds |
| `cast` | CastMember[] | no | See [cast model](#castmember) |
| `status` | enum | no | `ongoing` or `completed`; defaults to `ongoing` |
| `type` | enum | no | `anime` or `series`; defaults to `anime` |
| `publishStatus` | enum | no | `published` or `draft`; defaults to `draft` |
| `rating` | number | no | 0–10 |

For a web series, explicitly send:

```json
{
  "title": "Breaking Bad",
  "type": "series",
  "externalId": "1396",
  "totalSeasons": 5,
  "releaseYear": 2008,
  "genres": ["6849a1c3f2e4b12d5e8f0123"],
  "cast": [],
  "status": "completed",
  "publishStatus": "draft",
  "rating": 8.9
}
```

Response `201`:

```json
{ "series": { "...": "created series with generated slug" } }
```

The server generates a unique slug and revalidates the home, anime, and series
pages.

### 5.3 Update anime or web series

```http
PATCH /api/admin/series/{id}
```

**Auth:** admin

The body accepts any subset of the create fields:

```json
{
  "publishStatus": "published",
  "status": "completed",
  "totalSeasons": 5
}
```

Response:

```json
{ "series": { "...": "updated series" } }
```

To publish a web series, update `publishStatus` to `published` only after its
metadata and episodes are ready.

### 5.4 Delete anime or web series

```http
DELETE /api/admin/series/{id}
```

**Auth:** admin

Response:

```json
{ "success": true }
```

Deleting a series does not provide a separate cascade API for episodes. If the
native client needs to remove episodes as well, list the series episodes and
delete them explicitly before deleting the series.

### 5.5 Web series admin flow

The web admin's Web Series form supports:

1. Search TMDB with `type=tv`.
2. Select a TV result.
3. Fill title, description, poster, banner, external ID, year, season count,
   and rating.
4. Fetch TMDB TV genres and cast.
5. Resolve genre names to local Genre ObjectIds.
6. Save the series with `type: "series"`.
7. Add and edit episodes.
8. Publish by setting `publishStatus: "published"`.

The native admin should preserve this distinction:

- **TMDB TV ID** is stored as `externalId` for web series.
- **AniList ID** is stored as `externalId` for anime.
- Web series must use `type: "series"`.
- Anime must use `type: "anime"`.

### 5.6 Public list endpoints

These are not admin endpoints, but they are useful for validating the result:

```http
GET /api/anime
GET /api/series
```

They return published content only. Use the admin list when drafts or video
fields are required.

---

## 6. Episodes

Episodes belong to a Series document, whether that series is anime or a web
series. Episode video fields are hidden from ordinary public queries but are
returned by the admin list and admin mutation responses.

### 6.1 Add an episode

```http
POST /api/admin/episodes
```

**Auth:** admin

Request body:

| Field | Type | Required | Constraints |
|---|---|---:|---|
| `series` | ObjectId string | yes | Parent Series `_id` |
| `season` | positive integer | no | Defaults to `1` |
| `episodeNumber` | positive integer | yes | Unique within series + season |
| `title` | string | no | Maximum 200 characters |
| `videoUrl` | URL string | no | URL or `""` |
| `videoType` | enum | no | `auto`, `hls`, `direct`, `embed` |

Example:

```json
{
  "series": "6849a1c3f2e4b12d5e8f0123",
  "season": 1,
  "episodeNumber": 1,
  "title": "Pilot",
  "videoUrl": "https://cdn.example/episode-1.m3u8",
  "videoType": "hls"
}
```

Response `201`:

```json
{ "episode": { "...": "created episode, including video fields" } }
```

`409` means another episode already uses that episode number in the same
series and season.

### 6.2 List episodes for a series

```http
GET /api/admin/episodes?seriesId={seriesId}
```

**Auth:** admin

Episodes are sorted by season and then episode number. The response includes
the video fields:

```json
{
  "episodes": [
    {
      "_id": "6849a1c3f2e4b12d5e8f0124",
      "series": "6849a1c3f2e4b12d5e8f0123",
      "season": 1,
      "episodeNumber": 1,
      "title": "Pilot",
      "videoUrl": "https://cdn.example/episode-1.m3u8",
      "videoType": "hls",
      "createdAt": "2025-01-01T00:00:00.000Z",
      "updatedAt": "2025-01-01T00:00:00.000Z"
    }
  ]
}
```

`seriesId` is required and must be a valid ObjectId.

### 6.3 Update an episode

```http
PATCH /api/admin/episodes/{id}
```

**Auth:** admin

Accepted body fields:

```json
{
  "season": 1,
  "episodeNumber": 2,
  "title": "New title",
  "videoUrl": "https://cdn.example/episode-2.m3u8",
  "videoType": "hls"
}
```

The `series` field cannot be changed. To move an episode to another series,
create a new episode and delete the old one.

Response:

```json
{ "episode": { "...": "updated episode" } }
```

### 6.4 Delete an episode

```http
DELETE /api/admin/episodes/{id}
```

**Auth:** admin

Response:

```json
{ "success": true }
```

### 6.5 Episode video types

| Value | Meaning |
|---|---|
| `auto` | Detect handling from the URL |
| `hls` | HLS `.m3u8` stream |
| `direct` | Direct MP4/WebM file |
| `embed` | Third-party iframe/embed URL |

Video URLs are sensitive CDN data. Only send them over HTTPS and do not expose
them in logs or analytics.

---

## 7. Genres

Genres are shared by movies, anime, and web series.

### 7.1 List genres

```http
GET /api/admin/genres
```

**Auth:** none (public endpoint)

Response:

```json
{
  "genres": [
    {
      "_id": "6849a1c3f2e4b12d5e8f0123",
      "name": "Action",
      "createdAt": "2025-01-01T00:00:00.000Z",
      "updatedAt": "2025-01-01T00:00:00.000Z"
    }
  ]
}
```

### 7.2 Create or resolve a genre

```http
POST /api/admin/genres
```

**Auth:** admin

Request:

```json
{ "name": "Drama" }
```

Names are trimmed and matched case-insensitively. If the genre already exists,
the existing document is returned with `200`. A new document returns `201`.

Both responses have:

```json
{ "genre": { "_id": "...", "name": "Drama", "...": "..." } }
```

This endpoint is intentionally idempotent and is safe to use while resolving
TMDB/AniList `genreNames`.

### 7.3 Delete a genre

```http
DELETE /api/admin/genres/{id}
```

**Auth:** admin

Response:

```json
{ "success": true }
```

Deleting a genre does not remove its ObjectId from existing movies or series.
Those references will no longer populate to a genre name.

---

## 8. Users

### 8.1 List users

```http
GET /api/admin/users
```

**Auth:** admin

Query parameters:

| Parameter | Type | Default | Notes |
|---|---|---:|---|
| `page` | integer | `1` | Page number |
| `limit` | integer | `50` | Maximum 100 |
| `role` | string | all | `user` or `admin` |
| `q` | string | none | Case-insensitive nickname or email search |

Response:

```json
{
  "users": [
    {
      "_id": "6849a1c3f2e4b12d5e8f0123",
      "nickname": "john_doe",
      "email": "john@example.com",
      "image": null,
      "role": "user",
      "loginAttempts": 0,
      "lockedUntil": null,
      "createdAt": "2025-01-01T00:00:00.000Z"
    }
  ],
  "total": 1,
  "page": 1,
  "totalPages": 1
}
```

`passwordHash` is never returned.

### 8.2 Update a user's role

```http
PATCH /api/admin/users/{id}
```

**Auth:** admin

Request:

```json
{ "role": "admin" }
```

Allowed roles are `user` and `admin`.

Response:

```json
{ "user": { "...": "updated user" } }
```

Role changes are reflected in the user's session during the next role
re-check, approximately within 60 seconds.

---

## 9. Content requests

### 9.1 List requests

```http
GET /api/admin/requests
```

**Auth:** admin

Response:

```json
{
  "requests": [
    {
      "_id": "6849a1c3f2e4b12d5e8f0123",
      "title": "Breaking Bad",
      "type": "series",
      "note": "Please add this.",
      "status": "pending",
      "adminNote": "",
      "user": {
        "_id": "6849...",
        "nickname": "john_doe",
        "email": "john@example.com"
      },
      "createdAt": "2025-01-01T00:00:00.000Z",
      "updatedAt": "2025-01-01T00:00:00.000Z"
    }
  ]
}
```

The list is sorted newest first and is not paginated.

### 9.2 Update request status

```http
PATCH /api/admin/requests/{id}
```

**Auth:** admin

Request:

```json
{
  "status": "in_progress",
  "adminNote": "Metadata is being prepared."
}
```

`status` must be one of:

- `pending`
- `in_progress`
- `fulfilled`
- `rejected`

`adminNote` is optional and has a maximum length of 500 characters.

Response:

```json
{ "request": { "...": "updated request with populated user" } }
```

### 9.3 Delete a request

```http
DELETE /api/admin/requests/{id}
```

**Auth:** admin

Response:

```json
{ "success": true }
```

---

## 10. TMDB metadata

TMDB endpoints require an admin session and require the server's TMDB
configuration. The native client should call these endpoints rather than
calling TMDB directly.

### 10.1 Search movies or TV series

```http
GET /api/admin/tmdb/search?q={query}&type={type}
```

**Auth:** admin

Parameters:

| Parameter | Required | Values |
|---|---:|---|
| `q` | yes | Search text |
| `type` | no | `movie` or `tv`; defaults to `movie` |

An empty query returns `{ "results": [] }`. Results are limited to 10.

Movie result example:

```json
{
  "results": [
    {
      "externalId": "603",
      "title": "The Matrix",
      "description": "Description",
      "posterUrl": "https://image.tmdb.org/t/p/w500/...",
      "bannerUrl": "https://image.tmdb.org/t/p/original/...",
      "releaseYear": 1999,
      "rating": 8.2
    }
  ]
}
```

TV results use the same general shape and also include `totalSeasons` when
available.

### 10.2 Get TMDB movie details

```http
GET /api/admin/tmdb/movie/{tmdbId}
```

**Auth:** admin

The response is the metadata object directly, not wrapped in `movie`.
It includes the search fields plus:

```json
{
  "duration": 136,
  "genreNames": ["Action", "Science Fiction"],
  "cast": [
    {
      "name": "Keanu Reeves",
      "character": "Neo",
      "image": "https://image.tmdb.org/t/p/w500/...",
      "order": 0
    }
  ]
}
```

`duration` is returned in seconds because the movie schema stores runtime in
seconds. For example, a TMDB runtime of 136 minutes is returned as
`duration: 8160`.

Cast is limited to the first 15 billed members.

### 10.3 Get TMDB TV details for a web series

```http
GET /api/admin/tmdb/tv/{tmdbId}
```

**Auth:** admin

Response:

```json
{
  "genreNames": ["Crime", "Drama"],
  "cast": [
    {
      "name": "Bryan Cranston",
      "character": "Walter White",
      "image": "https://image.tmdb.org/t/p/w500/...",
      "order": 0
    }
  ]
}
```

The web-series form uses the TV search result for the base fields, then calls
this endpoint to fill genres and cast. Resolve every returned genre name with
`POST /api/admin/genres` before sending ObjectIds to the series create/update
endpoint.

---

## 11. AniList metadata

AniList is the metadata provider for anime. The canonical routes are below.
No AniList API key is required by the route.

### 11.1 Search anime

```http
GET /api/admin/anilist/search?q={query}
```

**Auth:** admin

Returns up to 10 results:

```json
{
  "results": [
    {
      "externalId": "16498",
      "title": "Attack on Titan",
      "description": "Description",
      "posterUrl": "https://s4.anilist.co/file/anilistcdn/media/anime/cover/...",
      "bannerUrl": "https://s4.anilist.co/file/anilistcdn/media/anime/banner/...",
      "releaseYear": 2013,
      "rating": 8.7,
      "episodes": 25,
      "genreNames": ["Action", "Drama", "Fantasy"]
    }
  ]
}
```

`externalId` is the numeric AniList ID for the selected anime.

### 11.2 Get anime characters

```http
GET /api/admin/anilist/anime/{anilistId}/characters
```

**Auth:** admin

The path ID must contain digits only. Response:

```json
{
  "cast": [
    {
      "name": "Kaji Yuki",
      "character": "Eren Yeager",
      "image": "https://s4.anilist.co/file/anilistcdn/staff/medium/...",
      "order": 0
    }
  ]
}
```

The result is limited to 15 relevant cast members.

The AniList search result also includes:

- `episodes`: the number of episodes reported by AniList
- `genreNames`: the source genre names, which must be resolved to local Genre
  ObjectIds before saving

If AniList has no voice actor for a character, the integration may use the
character role as the cast `character` value. Native clients should treat
`character` and `image` as optional display fields.

### 11.3 Episode title lookup

```http
GET /api/admin/anilist/anime/{anilistId}/episodes/{episodeNumber}
```

**Auth:** admin

AniList does not expose per-episode titles through this integration. The
endpoint intentionally returns:

```json
{
  "title": null,
  "note": "AniList does not expose per-episode titles — enter manually."
}
```

The native admin must allow the episode title to remain blank or be entered
manually.

---

## 12. Native app version management

The web admin's **App Updates** screen manages `AppVersion` records. These
records control the public native-app version check endpoint.

### 12.1 List version records

```http
GET /api/admin/app-versions
```

**Auth:** admin

Query parameters:

| Parameter | Type | Default | Values |
|---|---|---:|---|
| `page` | integer | `1` | Page number |
| `limit` | integer | `50` | Maximum 100 |
| `platform` | string | all | `android`, `ios`, or `all` |
| `channel` | string | all | `stable` or `beta` |

Response:

```json
{
  "versions": [
    {
      "_id": "6849a1c3f2e4b12d5e8f0123",
      "versionName": "1.2.3",
      "versionCode": 123,
      "platform": "android",
      "channel": "stable",
      "downloadUrl": "https://downloads.example/app.apk",
      "releaseNotes": "Bug fixes and performance improvements.",
      "forceUpdate": false,
      "minSupportedVersionCode": 100,
      "rolloutPercentage": 100,
      "isActive": true,
      "adminNotes": "Internal note",
      "createdAt": "2025-01-01T00:00:00.000Z",
      "updatedAt": "2025-01-01T00:00:00.000Z"
    }
  ],
  "total": 1,
  "page": 1,
  "totalPages": 1
}
```

Records are sorted by descending `versionCode`.

### 12.2 Create a version record

```http
POST /api/admin/app-versions
```

**Auth:** admin

Request body:

| Field | Type | Required | Constraints / default |
|---|---|---:|---|
| `versionName` | string | yes | Non-empty display version, e.g. `1.2.3` |
| `versionCode` | positive integer | yes | Globally unique; cannot be reused |
| `platform` | enum | yes | `android`, `ios`, or `all` |
| `channel` | enum | yes | `stable` or `beta` |
| `downloadUrl` | string | yes | APK, IPA, Play Store, or App Store URL |
| `releaseNotes` | string | no | Defaults to `""`; may contain Markdown |
| `forceUpdate` | boolean | no | Defaults to `false` |
| `minSupportedVersionCode` | integer | no | Defaults to `1` |
| `rolloutPercentage` | number | no | Clamped to 0–100; defaults to `100` |
| `isActive` | boolean | no | Defaults to `true` |
| `adminNotes` | string | no | Internal note; not returned to native version check |

Example:

```json
{
  "versionName": "1.2.3",
  "versionCode": 123,
  "platform": "android",
  "channel": "stable",
  "downloadUrl": "https://downloads.example/sarrows-1.2.3.apk",
  "releaseNotes": "- Faster playback\n- Bug fixes",
  "forceUpdate": false,
  "minSupportedVersionCode": 100,
  "rolloutPercentage": 50,
  "isActive": true,
  "adminNotes": "Initial 50% rollout"
}
```

Response `201`:

```json
{ "version": { "...": "created version record" } }
```

`409` means that `versionCode` already exists.

### 12.3 Activation behavior

Only active records are considered by the native version-check endpoint.

When a new record is created with `isActive` not equal to `false`:

- An `all` platform record deactivates every active record on that channel.
- An Android/iOS-specific record deactivates active records for that platform
  and active `all` records on that channel.

This prevents an older active record from overriding the new target.

### 12.4 Get one version record

```http
GET /api/admin/app-versions/{id}
```

**Auth:** admin

Response:

```json
{ "version": { "...": "version record" } }
```

### 12.5 Update a version record

```http
PATCH /api/admin/app-versions/{id}
```

**Auth:** admin

Allowed fields:

```text
versionName
platform
channel
downloadUrl
releaseNotes
forceUpdate
minSupportedVersionCode
rolloutPercentage
isActive
adminNotes
```

`versionCode` is deliberately not editable. To change the build code, create a
new version record.

Example:

```json
{
  "rolloutPercentage": 100,
  "isActive": true,
  "forceUpdate": true
}
```

When `isActive` is set to `true`, the same platform/channel deactivation rules
from [12.3](#123-activation-behavior) apply.

Response:

```json
{ "version": { "...": "updated version record" } }
```

### 12.6 Delete a version record

```http
DELETE /api/admin/app-versions/{id}
```

**Auth:** admin

Response:

```json
{ "ok": true }
```

### 12.7 Native version check

This endpoint is public and does not require admin authentication:

```http
POST /api/app/version/check
```

Request:

```json
{
  "versionCode": 123,
  "platform": "android",
  "channel": "stable"
}
```

Rules:

- `versionCode` must be an integer.
- `platform` must be `android` or `ios`.
- Any channel other than exactly `beta` resolves to `stable`.
- The server selects the highest `versionCode` active record matching the
  platform or `all` and the resolved channel.

Response when a matching active version exists:

```json
{
  "updateAvailable": true,
  "forceUpdate": false,
  "currentVersionSupported": true,
  "latestVersionCode": 123,
  "latestVersionName": "1.2.3",
  "downloadUrl": "https://downloads.example/sarrows-1.2.3.apk",
  "releaseNotes": "Bug fixes",
  "channel": "stable",
  "rolloutPercentage": 50,
  "minSupportedVersionCode": 100
}
```

Response when no active record exists:

```json
{
  "updateAvailable": false,
  "forceUpdate": false,
  "currentVersionSupported": true,
  "latestVersionCode": null,
  "latestVersionName": null,
  "downloadUrl": null,
  "releaseNotes": null,
  "channel": "stable",
  "rolloutPercentage": null,
  "minSupportedVersionCode": null
}
```

The native client should apply the response as follows:

1. If `currentVersionSupported` is `false`, show a blocking “App too old”
   screen and use `downloadUrl` to update.
2. If `updateAvailable` is `false`, continue normally.
3. If `updateAvailable` is `true` and `forceUpdate` is `true`, show a
   non-dismissible update flow.
4. If the update is optional, use `rolloutPercentage` for the device rollout
   decision before showing the prompt.

The server calculates `forceUpdate` as:

```text
(updateAvailable && configured forceUpdate) ||
(client versionCode < minSupportedVersionCode)
```

Therefore, a client below `minSupportedVersionCode` is unsupported even when
the configured release is not explicitly marked as a forced update.

`platform: "all"` is valid for admin version records and matches both Android
and iOS clients. The public check request itself must still send
`platform: "android"` or `platform: "ios"`.

The server returns `downloadUrl` and `releaseNotes` as `null` when there is no
newer version, even if an active version record exists.

---

## 13. Data models

### 13.1 Movie

| Field | Type | Notes |
|---|---|---|
| `_id` | ObjectId | Generated |
| `title` | string | Required |
| `slug` | string | Generated and unique |
| `description` | string | Optional |
| `posterUrl` | string | Optional |
| `bannerUrl` | string | Optional |
| `trailerUrl` | string | Optional |
| `videoUrl` | string | Sensitive; hidden by default |
| `videoType` | enum | `auto`, `hls`, `direct`, `embed` |
| `externalId` | string | Optional external provider ID |
| `duration` | number | Optional runtime |
| `releaseYear` | number | Optional |
| `genres` | ObjectId[] | References Genre |
| `cast` | CastMember[] | Embedded |
| `rating` | number | Default 0 |
| `ratingCount` | number | Default 0 |
| `views` | number | Default 0 |
| `status` | enum | `published` or `draft` |
| `createdAt`, `updatedAt` | date | Timestamps |

### 13.2 Series

| Field | Type | Notes |
|---|---|---|
| `_id` | ObjectId | Generated |
| `title` | string | Required |
| `slug` | string | Generated and unique |
| `description` | string | Optional |
| `posterUrl` | string | Optional |
| `bannerUrl` | string | Optional |
| `externalId` | string | AniList ID or TMDB TV ID |
| `totalSeasons` | number | Optional |
| `releaseYear` | number | Optional |
| `genres` | ObjectId[] | References Genre |
| `cast` | CastMember[] | Embedded |
| `status` | enum | `ongoing` or `completed` |
| `type` | enum | `anime` or `series` |
| `rating` | number | Default 0 |
| `ratingCount` | number | Default 0 |
| `views` | number | Default 0 |
| `publishStatus` | enum | `published` or `draft` |
| `createdAt`, `updatedAt` | date | Timestamps |

### 13.3 Episode

| Field | Type | Notes |
|---|---|---|
| `_id` | ObjectId | Generated |
| `series` | ObjectId | Required parent Series |
| `season` | positive number | Defaults to 1 |
| `episodeNumber` | positive number | Required |
| `title` | string | Optional |
| `videoUrl` | string | Sensitive; hidden by default |
| `videoType` | enum | `auto`, `hls`, `direct`, `embed` |
| `createdAt`, `updatedAt` | date | Timestamps |

The combination of `series`, `season`, and `episodeNumber` is unique.

### 13.4 Genre

| Field | Type |
|---|---|
| `_id` | ObjectId |
| `name` | string |
| `createdAt`, `updatedAt` | date |

Genre names are unique case-insensitively.

### 13.5 CastMember

```json
{
  "name": "Actor or voice actor",
  "character": "Character name",
  "image": "https://cdn.example/person.jpg",
  "order": 0
}
```

Rules:

- `name` is required and maximum 150 characters.
- `character` is optional and maximum 150 characters.
- `image` is optional but must be a URL when present.
- `order` is optional and must be an integer.

### 13.6 AppVersion

See [Native app version management](#12-native-app-version-management). The
`adminNotes` field is internal and is never returned by the public version
check response.

### 13.7 User

| Field | Type | Notes |
|---|---|---|
| `_id` | ObjectId | Generated |
| `nickname` | string | Required, unique, 3–20 characters |
| `email` | string | Required, unique, lowercased |
| `passwordHash` | string | Sensitive; never returned |
| `image` | string | Optional |
| `role` | enum | `user` or `admin`; defaults to `user` |
| `emailVerified` | date | Optional |
| `loginAttempts` | number | Internal failed-login counter |
| `lockedUntil` | date | Internal temporary lock expiry |
| `createdAt`, `updatedAt` | date | Timestamps |

### 13.8 Content Request

| Field | Type | Notes |
|---|---|---|
| `_id` | ObjectId | Generated |
| `user` | ObjectId | Required User reference |
| `title` | string | Required, maximum 200 characters |
| `type` | enum | `movie`, `series`, or `anime` |
| `note` | string | Optional, maximum 500 characters |
| `status` | enum | `pending`, `in_progress`, `fulfilled`, or `rejected` |
| `adminNote` | string | Optional, maximum 500 characters |
| `createdAt`, `updatedAt` | date | Timestamps |

---

## 14. Error reference

### 14.1 Authentication and authorization

```json
{ "error": "Forbidden" }
```

Status `403` means the request did not have an admin session. Re-authenticate
and verify that the cookie jar is being used.

### 14.2 Validation

Status `400` is returned for:

- Missing required fields.
- Invalid enum values.
- Invalid URLs.
- Invalid ObjectIds.
- Invalid numeric values.
- Invalid AniList IDs.
- Missing `seriesId` when listing episodes.
- No valid fields in an app-version PATCH.

### 14.3 Not found

Status `404` is returned for a missing movie, series, episode, user, request,
version record, or upstream metadata record.

### 14.4 Conflicts

Status `409` is returned for:

- Duplicate movie or series slug/title.
- Duplicate episode number within a series and season.
- Duplicate global app `versionCode`.
- A genre creation race that cannot resolve to the existing document.

### 14.5 Upstream metadata failures

TMDB or AniList failures normally surface as `500`. The native admin should
allow manual entry when metadata autofill is unavailable.

---

## 15. Recommended native admin workflows

### 15.1 Start an admin session

1. Authenticate with the credentials provider and store the returned session
   cookie.
2. Call `GET /api/me`.
3. Continue only when `user.role` is `admin`.
4. Send the cookie with every `/api/admin/*` request.

### 15.2 Add a movie

1. Optionally call `GET /api/admin/tmdb/search?q=...&type=movie`.
2. Optionally call `GET /api/admin/tmdb/movie/{tmdbId}`.
3. Resolve each returned `genreNames` value using
   `POST /api/admin/genres`.
4. Create with `POST /api/admin/movies`.
5. Keep it in `status: "draft"` while checking metadata and playback.
6. Add or update the video URL and `videoType`.
7. Publish with `PATCH /api/movies/{id}` and `{ "status": "published" }`.

### 15.3 Add a web series

1. Search with `GET /api/admin/tmdb/search?q=...&type=tv`.
2. Save the selected result's `externalId` as the TMDB TV ID.
3. Fetch TV genres and cast with `GET /api/admin/tmdb/tv/{tmdbId}`.
4. Resolve each returned genre name with `POST /api/admin/genres`.
5. Create with `POST /api/admin/series` and **`type: "series"`**.
6. Keep `publishStatus: "draft"` while adding episodes.
7. Add episodes with `POST /api/admin/episodes`.
8. Correct episode titles or video settings with
   `PATCH /api/admin/episodes/{episodeId}`.
9. Verify episodes with `GET /api/admin/episodes?seriesId={seriesId}`.
10. Publish with `PATCH /api/admin/series/{seriesId}` and
    `{ "publishStatus": "published" }`.

### 15.4 Add anime

1. Search with `GET /api/admin/anilist/search?q=...`.
2. Save the result's AniList `externalId`.
3. Fetch cast with
   `GET /api/admin/anilist/anime/{anilistId}/characters`.
4. Resolve each genre name with `POST /api/admin/genres`.
5. Create with `POST /api/admin/series` and **`type: "anime"`**.
6. Add episodes with `POST /api/admin/episodes`.
7. Enter episode titles manually; AniList title lookup returns `null`.
8. Publish with `PATCH /api/admin/series/{seriesId}` and
   `{ "publishStatus": "published" }`.

### 15.5 Manage a content request

1. List with `GET /api/admin/requests`.
2. Set `{ "status": "in_progress" }` with
   `PATCH /api/admin/requests/{id}`.
3. Add the requested movie, anime, or web series.
4. Set `{ "status": "fulfilled", "adminNote": "Added." }`.
5. Use `rejected` with an explanatory `adminNote` when it cannot be added.

### 15.6 Publish a native app update

1. Create an app-version record with `POST /api/admin/app-versions`.
2. Use a new, globally unique, higher `versionCode`.
3. Set the target `platform` and `channel`.
4. Set `downloadUrl`, release notes, minimum supported version, and rollout.
5. Keep `isActive: false` for a staged record, or activate it immediately.
6. Increase `rolloutPercentage` with PATCH as rollout progresses.
7. Set `forceUpdate: true` only when every affected client must update.
8. Validate behavior through `POST /api/app/version/check`.

---

## 16. Public app API and routes

These are useful when a native admin client links to or previews managed
content. They are not replacements for admin CRUD endpoints.

### 16.1 Public content list endpoints

```http
GET /api/movies
GET /api/anime
GET /api/series
GET /api/episodes?seriesId={seriesId}
```

Public list/detail responses do not expose the stored CDN video URL. Playback
uses the protected stream endpoints:

```http
GET /api/stream/movie/{id}
GET /api/stream/movie/{id}/embed
GET /api/stream/episode/{id}
GET /api/stream/episode/{id}/embed
```

### 16.2 Web routes and deep links

| Content | List | Detail | Episode |
|---|---|---|---|
| Movies | `/movies` | `/movies/{slug}` | — |
| Anime | `/anime` | `/anime/{slug}` | `/anime/{slug}/episode/{episodeId}` |
| Web series | `/series` | `/series/{slug}` | `/series/{slug}/episode/{episodeId}` |

The web detail and episode pages require login. A native app should use the
JSON APIs and its own authenticated playback flow instead of scraping these
HTML routes.