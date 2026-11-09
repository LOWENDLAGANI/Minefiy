package com.minetallest.minefiy.ytmusic

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parses JSON captured from the running yt-proxy (ytmusicapi 1.12.3) to lock
 * in the wire contract documented in docs/04-stage-1-yt-proxy.md.
 */
class YtDtoParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun `search songs map to YtSong with videoId and duration`() {
        val payload = """
            [{"category":"Songs","resultType":"song","title":"Viva La Vida",
              "album":{"name":"Viva La Vida","id":"MPREb_eWjia3Zrd7e"},
              "videoId":"ALsvdSA9tOU","duration":"4:03",
              "artists":[{"name":"Coldplay","id":"UCIaFw5VBEK8qaW6nRpx_qnw"}],
              "duration_seconds":243,"isExplicit":false,
              "thumbnails":[{"url":"https://example.test/60.jpg","width":60,"height":60},
                            {"url":"https://example.test/544.jpg","width":544,"height":544}]}]
        """.trimIndent()

        val songs = json.decodeFromString<List<YtSongDto>>(payload).mapNotNull { it.toYtSong() }

        assertEquals(1, songs.size)
        val song = songs.first()
        assertEquals("ALsvdSA9tOU", song.videoId)
        assertEquals("Viva La Vida", song.title)
        assertEquals("Coldplay", song.artist)
        assertEquals("Viva La Vida", song.album)
        assertEquals(243, song.durationSeconds)
        // Highest-resolution thumbnail wins.
        assertEquals("https://example.test/544.jpg", song.artworkUrl)
        assertFalse(song.isExplicit)
        assertEquals("yt:ALsvdSA9tOU", song.mediaId)
        assertEquals("yt:ALsvdSA9tOU", song.toSong().key)
    }

    @Test
    fun `album detail tracks tolerate null thumbnails and string album`() {
        val payload = """
            {"title":"Viva La Vida","type":"Single","year":"2008","description":"desc",
             "artists":[{"name":"Coldplay","id":"UCIaFw5VBEK8qaW6nRpx_qnw"}],
             "thumbnails":[{"url":"https://example.test/alb.jpg","width":544,"height":544}],
             "trackCount":2,
             "tracks":[{"videoId":"dvgZkm1xWPE","title":"Viva La Vida",
                        "artists":[{"name":"Coldplay","id":"UCIaFw5VBEK8qaW6nRpx_qnw"}],
                        "album":"Viva La Vida","thumbnails":null,"isExplicit":false,
                        "trackNumber":1,"duration":"4:03","duration_seconds":243}]}
        """.trimIndent()

        val album = json.decodeFromString<YtAlbumDetailDto>(payload).toYtAlbum()

        assertEquals("Viva La Vida", album.title)
        assertEquals("2008", album.year)
        assertEquals(2, album.trackCount)
        assertEquals(1, album.tracks.size)
        val track = album.tracks.first()
        assertEquals("dvgZkm1xWPE", track.videoId)
        assertEquals("Viva La Vida", track.album)
        assertEquals(243, track.durationSeconds)
        // Falls back to the album artwork when the track has none.
        assertEquals("https://example.test/alb.jpg", track.artworkUrl)
    }

    @Test
    fun `artist search uses the artist key rather than title`() {
        val payload = """
            [{"category":"Artists","resultType":"artist","artist":"Coldplay",
              "browseId":"UCIaFw5VBEK8qaW6nRpx_qnw",
              "thumbnails":[{"url":"https://example.test/ar.jpg","width":544,"height":544}]}]
        """.trimIndent()

        val artists = json.decodeFromString<List<YtArtistSummaryDto>>(payload)
            .mapNotNull { it.toYtArtistSummary() }

        assertEquals(1, artists.size)
        assertEquals("Coldplay", artists.first().name)
        assertEquals("UCIaFw5VBEK8qaW6nRpx_qnw", artists.first().channelId)
    }

    @Test
    fun `artist detail exposes top songs and albums`() {
        val payload = """
            {"name":"Coldplay","channelId":"UCIaFw5VBEK8qaW6nRpx_qnw",
             "description":"desc","subscribers":"81.9M",
             "thumbnails":[{"url":"https://example.test/ard.jpg","width":544,"height":544}],
             "songs":{"browseId":"VLPLx","results":[
                {"videoId":"gm-Y9idMMQ4","title":"The Scientist",
                 "artists":[{"name":"Coldplay","id":"x"}],
                 "album":{"name":"A Rush of Blood to the Head","id":"MPREb_U4m9ZEeYxdi"},
                 "duration_seconds":309}]},
             "albums":{"browseId":"MPAD_x","params":"x","results":[
                {"title":"Moon Music","artists":[],"browseId":"MPREb_pnVvyDhXXd0",
                 "year":"2024","thumbnails":[{"url":"https://example.test/m.jpg","width":544,"height":544}]}]}}
        """.trimIndent()

        val artist = json.decodeFromString<YtArtistDetailDto>(payload).toYtArtist()

        assertEquals("Coldplay", artist.name)
        assertEquals("81.9M", artist.subscribers)
        assertEquals(1, artist.topSongs.size)
        assertEquals("The Scientist", artist.topSongs.first().title)
        assertEquals(1, artist.albums.size)
        assertEquals("Moon Music", artist.albums.first().title)
        // No artist refs on the album card -> placeholder, never "null".
        assertEquals("Unknown artist", artist.albums.first().artist)
    }

    @Test
    fun `queue tracks use length and singular thumbnail`() {
        val payload = """
            {"tracks":[{"videoId":"ALsvdSA9tOU","title":"Viva La Vida","length":"4:03",
                        "artists":[{"name":"Coldplay","id":"x"}],
                        "thumbnail":[{"url":"https://example.test/w.jpg","width":544,"height":544}]}],
             "playlistId":"RDAMVM","lyrics":"MPLYt_eWjia3Zrd7e-1"}
        """.trimIndent()

        val watch = json.decodeFromString<YtWatchDto>(payload)
        val queue = watch.tracks.mapNotNull { it.toYtSong() }

        assertEquals(1, queue.size)
        assertEquals(243, queue.first().durationSeconds)
        assertEquals("https://example.test/w.jpg", queue.first().artworkUrl)
        assertEquals("MPLYt_eWjia3Zrd7e-1", watch.lyrics)
    }

    @Test
    fun `lyrics and error envelopes parse`() {
        val lyrics = json.decodeFromString<YtLyricsDto>(
            """{"lyrics":"I used to rule the world","source":null}"""
        )
        assertEquals("I used to rule the world", lyrics.lyrics)
        assertNull(lyrics.source)

        val error = json.decodeFromString<YtErrorDto>(
            """{"detail":"Unsupported filter 'bogus'."}"""
        )
        assertEquals("Unsupported filter 'bogus'.", error.detail)

        assertTrue(
            json.decodeFromString<YtHealthDto>("""{"ok":true,"ytmusicapi":"1.12.3"}""").ok
        )
    }

    @Test
    fun `duration parsing handles mm ss and hh mm ss`() {
        assertEquals(243, parseDurationToSeconds("4:03"))
        assertEquals(3723, parseDurationToSeconds("1:02:03"))
        assertNull(parseDurationToSeconds(null))
        assertNull(parseDurationToSeconds("nonsense"))
    }

    @Test
    fun `shelf items without ids are dropped`() {
        assertNull(json.decodeFromString<YtSongDto>("""{"title":"ghost"}""").toYtSong())
        assertNull(json.decodeFromString<YtAlbumSummaryDto>("""{"title":"ghost"}""").toYtAlbumSummary())
        assertNull(json.decodeFromString<YtArtistSummaryDto>("""{"artist":"ghost"}""").toYtArtistSummary())
    }
}
