package com.synap.music.enums

sealed class DiscoveryFilter(val label: String, val querySuffix: String) {
    sealed class Language(label: String, querySuffix: String) : DiscoveryFilter(label, querySuffix) {
        object None : Language("All Languages", "")
        object Hindi : Language("Hindi", "hindi")
        object English : Language("English", "english")
        object Punjabi : Language("Punjabi", "punjabi")
        object Telugu : Language("Telugu", "telugu")
        object Spanish : Language("Spanish", "spanish")
        object Korean : Language("Korean", "kpop")
        object Japanese : Language("Japanese", "japanese")
        object Marathi : Language("Marathi", "marathi")
        object Bengali : Language("Bengali", "bengali")
        object French : Language("French", "french")
        object German : Language("German", "german")
        object Portuguese : Language("Portuguese", "portuguese")
        object Chinese : Language("Chinese", "chinese")
        object Russian : Language("Russian", "russian")
        object Italian : Language("Italian", "italian")
        object Vietnamese : Language("Vietnamese", "vietnamese")
        object Indonesian : Language("Indonesian", "indonesian")
        object Thai : Language("Thai", "thai")
        object Dutch : Language("Dutch", "dutch")
        object Polish : Language("Polish", "polish")
        object Persian : Language("Persian", "persian")
        object Greek : Language("Greek", "greek")
        object Hebrew : Language("Hebrew", "hebrew")
        object Swedish : Language("Swedish", "swedish")
        object Norwegian : Language("Norwegian", "norwegian")
        object Danish : Language("Danish", "danish")
        object Finnish : Language("Finnish", "finnish")
        object Hungarian : Language("Hungarian", "hungarian")
        object Czech : Language("Czech", "czech")
        object Romanian : Language("Romanian", "romanian")
        object Ukrainian : Language("Ukrainian", "ukrainian")
        object Malay : Language("Malay", "malay")
        object Gujarati : Language("Gujarati", "gujarati")
        object Odia : Language("Odia", "odia")
        object Bhojpuri : Language("Bhojpuri", "bhojpuri")
        object Assamese : Language("Assamese", "assamese")
        object Maithili : Language("Maithili", "maithili")
        object Sindhi : Language("Sindhi", "sindhi")
        object Nepali : Language("Nepali", "nepali")
        object Sanskrit : Language("Sanskrit", "sanskrit")
        object Konkani : Language("Konkani", "konkani")
        object Manipuri : Language("Manipuri", "manipuri")
        object Kashmiri : Language("Kashmiri", "kashmiri")
        object Dogri : Language("Dogri", "dogri")
        object Bodo : Language("Bodo", "bodo")
        object Santali : Language("Santali", "santali")
        object Haryanvi : Language("Haryanvi", "haryanvi")
        object Rajasthani : Language("Rajasthani", "rajasthani")
        object Marwari : Language("Marwari", "marwari")
        object Garhwali : Language("Garhwali", "garhwali")
        object Kumaoni : Language("Kumaoni", "kumaoni")
        object Nagpuri : Language("Nagpuri", "nagpuri")
        object Chhattisgarhi : Language("Chhattisgarhi", "chhattisgarhi")
        object Zulu : Language("Zulu", "zulu")
        object Swahili : Language("Swahili", "swahili")
        object Yoruba : Language("Yoruba", "yoruba")
        object Igbo : Language("Igbo", "igbo")
        object Amharic : Language("Amharic", "amharic")
        object Tagalog : Language("Tagalog", "tagalog")
        object Afrikaans : Language("Afrikaans", "afrikaans")
        object Albanian : Language("Albanian", "albanian")
        object Armenian : Language("Armenian", "armenian")
        object Azerbaijani : Language("Azerbaijani", "azerbaijani")
        object Basque : Language("Basque", "basque")
        object Belarusian : Language("Belarusian", "belarusian")
        object Bosnian : Language("Bosnian", "bosnian")
        object Bulgarian : Language("Bulgarian", "bulgarian")
        object Burmese : Language("Burmese", "burmese")
        object Catalan : Language("Catalan", "catalan")
        object Croatian : Language("Croatian", "croatian")
        object Estonian : Language("Estonian", "estonian")
        object Georgian : Language("Georgian", "georgian")
        object Icelandic : Language("Icelandic", "icelandic")
        object Kazakh : Language("Kazakh", "kazakh")
        object Khmer : Language("Khmer", "khmer")
        object Lao : Language("Lao", "lao")
        object Latvian : Language("Latvian", "latvian")
        object Lithuanian : Language("Lithuanian", "lithuanian")
        object Macedonian : Language("Macedonian", "macedonian")
        object Mongolian : Language("Mongolian", "mongolian")
        object Serbian : Language("Serbian", "serbian")
        object Slovak : Language("Slovak", "slovak")
        object Slovenian : Language("Slovenian", "slovenian")
        object Uzbek : Language("Uzbek", "uzbek")
        object Welsh : Language("Welsh", "welsh")
        
        companion object {
            private val rawLanguages = listOf(
                Hindi, English, Punjabi, Telugu, Spanish, Korean, Japanese, 
                Marathi, Bengali, French, German, Portuguese, Chinese, Russian,
                Italian, Vietnamese, Indonesian, Thai, Dutch, Polish, Persian, Greek, Hebrew,
                Swedish, Norwegian, Danish, Finnish, Hungarian, Czech, Romanian, Ukrainian, Malay,
                Gujarati, Odia, Bhojpuri, Assamese, Maithili, Sindhi, Nepali,
                Sanskrit, Konkani, Manipuri, Kashmiri, Dogri, Bodo, Santali, Haryanvi,
                Rajasthani, Marwari, Garhwali, Kumaoni, Nagpuri, Chhattisgarhi,
                Zulu, Swahili, Yoruba, Igbo, Amharic, Tagalog, Afrikaans, Albanian, Armenian, Azerbaijani, Basque,
                Belarusian, Bosnian, Bulgarian, Burmese, Catalan, Croatian, Estonian, Georgian,
                Icelandic, Kazakh, Khmer, Lao, Latvian, Lithuanian, Macedonian, Mongolian, Serbian,
                Slovak, Slovenian, Uzbek, Welsh,
            )

            val entries: List<Language> = listOf(None) + rawLanguages.sortedBy { it.label }
        }
    }

    sealed class Genre(label: String, querySuffix: String) : DiscoveryFilter(label, querySuffix) {
        object None : Genre("All Genres", "")
        object Pop : Genre("Pop", "pop")
        object Rock : Genre("Rock", "rock")
        object HipHop : Genre("Hip Hop", "hip hop")
        object Lofi : Genre("Lofi", "lofi")
        object Relaxing : Genre("Relaxing", "relaxing")
        object Electronic : Genre("Electronic", "electronic")
        object Jazz : Genre("Jazz", "jazz")
        object Metal : Genre("Metal", "metal")
        object Classical : Genre("Classical", "classical")
        object Country : Genre("Country", "country")
        object Blues : Genre("Blues", "blues")
        object Soul : Genre("Soul", "soul")
        object Reggae : Genre("Reggae", "reggae")
        object Punk : Genre("Punk", "punk")
        object Indie : Genre("Indie", "indie")
        object Folk : Genre("Folk", "folk")
        
        companion object {
            val entries = listOf(
                None, Pop, Rock, HipHop, Lofi, Relaxing, Electronic, Jazz, Metal, 
                Classical, Country, Blues, Soul, Reggae, Punk, Indie, Folk,
            )
        }
    }

    sealed class Decade(label: String, querySuffix: String) : DiscoveryFilter(label, querySuffix) {
        object None : Decade("All Time", "")
        object Year2020s : Decade("2020s", "2020s")
        object Year2010s : Decade("2010s", "2010s")
        object Year2000s : Decade("2000s", "2000s")
        object Year90s : Decade("90s", "90s")
        object Year80s : Decade("80s", "80s")
        object Year70s : Decade("70s", "70s")
        object Year60s : Decade("60s", "60s")
        object Year50s : Decade("50s", "50s")
        
        companion object {
            val entries = listOf(None, Year2020s, Year2010s, Year2000s, Year90s, Year80s, Year70s, Year60s, Year50s)
        }
    }

    sealed class Style(label: String, querySuffix: String) : DiscoveryFilter(label, querySuffix) {
        object Random : Style("Random", "")
        object Latest : Style("Latest", "latest")
        object Hits : Style("Hits", "hits")
        object Popular : Style("Popular", "popular")
        object Trending : Style("Trending", "trending")
        object Best : Style("Best", "best")

        companion object {
            val entries = listOf(Random, Latest, Hits, Popular, Trending, Best)
        }
    }
}
