package com.synap.music.utils

import com.synap.pulse.SynapService
import com.synap.pulse.requests.lyrics
import com.synap.pulse.models.bodies.NextBody
import com.synap.pulse.models.NavigationEndpoint
import com.synap.pulse.models.Thumbnail

/**
 * A buffer used to pre-populate discovery results for faster initial load.
 */
val synapStream = listOf(
    SynapService.SongItem(
        info = SynapService.Info("Dracula", NavigationEndpoint.Endpoint.Watch(videoId = "6KjVYeQ9SRw")),
        authors = listOf(SynapService.Info("Tame Impala", NavigationEndpoint.Endpoint.Browse(browseId = ""))),
        album = null,
        durationText = "",
        thumbnail = Thumbnail("https://i.ytimg.com/vi/6KjVYeQ9SRw/maxresdefault.jpg", 1080, 1920)
    ),
    SynapService.SongItem(
        info = SynapService.Info("Patient Zero", NavigationEndpoint.Endpoint.Watch(videoId = "mw3kSNIxjqo")),
        authors = listOf(SynapService.Info("Taylor Swift", NavigationEndpoint.Endpoint.Browse(browseId = ""))),
        album = null,
        durationText = "",
        thumbnail = Thumbnail("https://i.ytimg.com/vi/mw3kSNIxjqo/maxresdefault.jpg", 1080, 1920)
    ),
    SynapService.SongItem(
        info = SynapService.Info("Strings of the Invisible", NavigationEndpoint.Endpoint.Watch(videoId = "ASCtP9-TBv8")),
        authors = listOf(SynapService.Info("Synapsonix", NavigationEndpoint.Endpoint.Browse(browseId = "UC3y0fT-F_z3-xxxxxxxx"))),
        album = null,
        durationText = "",
        thumbnail = Thumbnail("https://i.ytimg.com/vi/ASCtP9-TBv8/maxresdefault.jpg", 1080, 1920)
    ),
    SynapService.SongItem(
        info = SynapService.Info("Fanged Instinct", NavigationEndpoint.Endpoint.Watch(videoId = "IqGnRHKgqhg")),
        authors = listOf(SynapService.Info("Synapsonix", NavigationEndpoint.Endpoint.Browse(browseId = "UC3y0fT-F_z3-xxxxxxxx"))),
        album = null,
        durationText = "",
        thumbnail = Thumbnail("https://i.ytimg.com/vi/IqGnRHKgqhg/maxresdefault.jpg", 1080, 1920)
    ),
    SynapService.SongItem(
        info = SynapService.Info("Thrift Shop feat. Wanz", NavigationEndpoint.Endpoint.Watch(videoId = "QK8mJJJvaes")),
        authors = listOf(SynapService.Info("Macklemore & Ryan Lewis", NavigationEndpoint.Endpoint.Browse(browseId = ""))),
        album = null,
        durationText = "",
        thumbnail = Thumbnail("https://i.ytimg.com/vi/QK8mJJJvaes/maxresdefault.jpg", 1080, 1920)
    ),
    SynapService.SongItem(
        info = SynapService.Info("Espresso", NavigationEndpoint.Endpoint.Watch(videoId = "51zjlMhdSTE")),
        authors = listOf(SynapService.Info("Sabrina Carpenter", NavigationEndpoint.Endpoint.Browse(browseId = ""))),
        album = null,
        durationText = "",
        thumbnail = Thumbnail("https://i.ytimg.com/vi/51zjlMhdSTE/maxresdefault.jpg", 1080, 1920)
    ),
    SynapService.SongItem(
        info = SynapService.Info("Neon Spies", NavigationEndpoint.Endpoint.Watch(videoId = "uXKrNNEgka8")),
        authors = listOf(SynapService.Info("SynapSonix", NavigationEndpoint.Endpoint.Browse(browseId = "UC3y0fT-F_z3-xxxxxxxx"))),
        album = null,
        durationText = "",
        thumbnail = Thumbnail("https://i.ytimg.com/vi/uXKrNNEgka8/maxresdefault.jpg", 1080, 1920)
    ),
    SynapService.SongItem(
        info = SynapService.Info("Thick Smoke", NavigationEndpoint.Endpoint.Watch(videoId = "PAwoDqwVSuY")),
        authors = listOf(SynapService.Info("SynapSonix", NavigationEndpoint.Endpoint.Browse(browseId = "UC3y0fT-F_z3-xxxxxxxx"))),
        album = null,
        durationText = "",
        thumbnail = Thumbnail("https://i.ytimg.com/vi/PAwoDqwVSuY/maxresdefault.jpg", 1080, 1920)
    ),
    SynapService.SongItem(
        info = SynapService.Info("Blinding Lights", NavigationEndpoint.Endpoint.Watch(videoId = "4NRXx6U8ABQ")),
        authors = listOf(SynapService.Info("The Weeknd", NavigationEndpoint.Endpoint.Browse(browseId = ""))),
        album = null,
        durationText = "",
        thumbnail = Thumbnail("https://i.ytimg.com/vi/4NRXx6U8ABQ/maxresdefault.jpg", 1080, 1920)
    ),
    SynapService.SongItem(
        info = SynapService.Info("Make It Bun Dem", NavigationEndpoint.Endpoint.Watch(videoId = "BGpzGu9Yp6Y")),
        authors = listOf(SynapService.Info("Skrillex & Damian \"Jr. Gong\" Marley", NavigationEndpoint.Endpoint.Browse(browseId = ""))),
        album = null,
        durationText = "",
        thumbnail = Thumbnail("https://i.ytimg.com/vi/BGpzGu9Yp6Y/maxresdefault.jpg", 1080, 1920)
    ),
    SynapService.SongItem(
        info = SynapService.Info("Hypnotize", NavigationEndpoint.Endpoint.Watch(videoId = "eaPzCHEQExs")),
        authors = listOf(SynapService.Info("The Notorious B.I.G.", NavigationEndpoint.Endpoint.Browse(browseId = ""))),
        album = null,
        durationText = "",
        thumbnail = Thumbnail("https://i.ytimg.com/vi/eaPzCHEQExs/maxresdefault.jpg", 1080, 1920)
    ),
    SynapService.SongItem(
        info = SynapService.Info("Lose Yourself", NavigationEndpoint.Endpoint.Watch(videoId = "xFYQQPAOz7Y")),
        authors = listOf(SynapService.Info("Eminem", NavigationEndpoint.Endpoint.Browse(browseId = ""))),
        album = null,
        durationText = "",
        thumbnail = Thumbnail("https://i.ytimg.com/vi/xFYQQPAOz7Y/maxresdefault.jpg", 1080, 1920)
    ),
    SynapService.SongItem(
        info = SynapService.Info("Big Dawgs", NavigationEndpoint.Endpoint.Watch(videoId = "hOHKltAiKXQ")),
        authors = listOf(SynapService.Info("Hanumankind", NavigationEndpoint.Endpoint.Browse(browseId = ""))),
        album = null,
        durationText = "",
        thumbnail = Thumbnail("https://i.ytimg.com/vi/hOHKltAiKXQ/maxresdefault.jpg", 1080, 1920)
    ),
    SynapService.SongItem(
        info = SynapService.Info("Run It Up", NavigationEndpoint.Endpoint.Watch(videoId = "MbJ72KO5khs")),
        authors = listOf(SynapService.Info("Hanumankind", NavigationEndpoint.Endpoint.Browse(browseId = ""))),
        album = null,
        durationText = "",
        thumbnail = Thumbnail("https://i.ytimg.com/vi/MbJ72KO5khs/maxresdefault.jpg", 1080, 1920)
    ),
    SynapService.SongItem(
        info = SynapService.Info("Aathma Raama", NavigationEndpoint.Endpoint.Watch(videoId = "pxCWiYFkvTg")),
        authors = listOf(SynapService.Info("Brodha V", NavigationEndpoint.Endpoint.Browse(browseId = ""))),
        album = null,
        durationText = "",
        thumbnail = Thumbnail("https://i.ytimg.com/vi/pxCWiYFkvTg/maxresdefault.jpg", 1080, 1920)
    ),
    SynapService.SongItem(
        info = SynapService.Info("promise", NavigationEndpoint.Endpoint.Watch(videoId = "KIdFeby__VQ")),
        authors = listOf(SynapService.Info("広瀬香美", NavigationEndpoint.Endpoint.Browse(browseId = ""))),
        album = null,
        durationText = "",
        thumbnail = Thumbnail("https://i.ytimg.com/vi/KIdFeby__VQ/maxresdefault.jpg", 1080, 1920)
    ),
    SynapService.SongItem(
        info = SynapService.Info("Наливное яблочко", NavigationEndpoint.Endpoint.Watch(videoId = "4TkiPqRX6Us")),
        authors = listOf(SynapService.Info("Татьяна Куртукова", NavigationEndpoint.Endpoint.Browse(browseId = ""))),
        album = null,
        durationText = "",
        thumbnail = Thumbnail("https://i.ytimg.com/vi/4TkiPqRX6Us/maxresdefault.jpg", 1080, 1920)
    ),
    SynapService.SongItem(
        info = SynapService.Info("大展鸿图 ft.AR刘夫阳", NavigationEndpoint.Endpoint.Watch(videoId = "mldcDidGJCg")),
        authors = listOf(SynapService.Info("揽佬SKAI ISYOURGOD", NavigationEndpoint.Endpoint.Browse(browseId = ""))),
        album = null,
        durationText = "",
        thumbnail = Thumbnail("https://i.ytimg.com/vi/mldcDidGJCg/maxresdefault.jpg", 1080, 1920)
    ),
    SynapService.SongItem(
        info = SynapService.Info("Attention", NavigationEndpoint.Endpoint.Watch(videoId = "nfs8NYg7yQM")),
        authors = listOf(SynapService.Info("Charlie Puth", NavigationEndpoint.Endpoint.Browse(browseId = ""))),
        album = null,
        durationText = "",
        thumbnail = Thumbnail("https://i.ytimg.com/vi/nfs8NYg7yQM/maxresdefault.jpg", 1080, 1920)
    ),
    SynapService.SongItem(
        info = SynapService.Info("Centuries", NavigationEndpoint.Endpoint.Watch(videoId = "x1ckRXQFvdk")),
        authors = listOf(SynapService.Info("Fall Out Boy", NavigationEndpoint.Endpoint.Browse(browseId = ""))),
        album = null,
        durationText = "",
        thumbnail = Thumbnail("https://i.ytimg.com/vi/x1ckRXQFvdk/maxresdefault.jpg", 1080, 1920)
    ),
    SynapService.SongItem(
        info = SynapService.Info("drop dead", NavigationEndpoint.Endpoint.Watch(videoId = "78wrful9cVU")),
        authors = listOf(SynapService.Info("Olivia Rodrigo", NavigationEndpoint.Endpoint.Browse(browseId = ""))),
        album = null,
        durationText = "",
        thumbnail = Thumbnail("https://i.ytimg.com/vi/78wrful9cVU/maxresdefault.jpg", 1080, 1920)
    )
)

/**
 * Filters the placeholder stream based on the selected language filter.
 */
fun getSynapStreamForLanguage(language: String): List<SynapService.SongItem> {
    if (language == "None" || language.isBlank()) return synapStream

    val langLower = language.lowercase()
    return synapStream.filter { song ->
        val videoId = song.info?.endpoint?.videoId ?: ""
        when (videoId) {
            // English / Global songs
            "6KjVYeQ9SRw", "mw3kSNIxjqo", "ASCtP9-TBv8", "IqGnRHKgqhg",
            "QK8mJJJvaes", "51zjlMhdSTE", "uXKrNNEgka8", "PAwoDqwVSuY",
            "4NRXx6U8ABQ", "BGpzGu9Yp6Y", "eaPzCHEQExs", "xFYQQPAOz7Y",
            "nfs8NYg7yQM", "x1ckRXQFvdk", "78wrful9cVU" -> {
                langLower == "english"
            }
            // Indian / South Asian songs
            "hOHKltAiKXQ", "MbJ72KO5khs", "pxCWiYFkvTg" -> {
                langLower in setOf(
                    "english", "hindi", "punjabi", "tamil", "telugu",
                    "malayalam", "kannada", "marathi", "gujarati", "bhojpuri",
                    "bengali", "urdu", "assamese", "nepali"
                )
            }
            // Japanese song
            "KIdFeby__VQ" -> {
                langLower == "japanese"
            }
            // Russian song
            "4TkiPqRX6Us" -> {
                langLower in setOf("russian", "ukrainian")
            }
            // Chinese song
            "mldcDidGJCg" -> {
                langLower in setOf("chinese", "mandarin", "cantonese")
            }
            else -> false
        }
    }
}
