package com.minetallest.minefiy.data

/**
 * Primary music source for the app.
 *
 * - B2: User's own MP3 library (Backblaze B2 + Firebase Firestore).
 *   Search is local (Firestore songs/artists), playback via B2 presigned URLs.
 *
 * - YOUTUBE_MUSIC: YouTube Music via Python yt-proxy (ytmusicapi for
 *   metadata) + Android InnerTube StreamResolver for playback.
 *   Search is remote, playback via resolved audio URL.
 */
enum class DataSource {
    B2,
    YOUTUBE_MUSIC
}
