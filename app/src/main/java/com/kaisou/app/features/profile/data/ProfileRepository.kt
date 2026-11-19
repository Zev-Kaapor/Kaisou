package com.kaisou.app.features.profile.data

// Android framework
import android.content.Context
// JSON parsing
import org.json.JSONArray
import org.json.JSONObject
// Java time and text
import java.text.Collator
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.anilist.AniDescription
import com.kaisou.app.core.anilist.AniListClient
import com.kaisou.app.core.anilist.AniShelvesParser
import com.kaisou.app.core.anilist.StaffRoles
import com.kaisou.app.core.auth.AniListSession
import com.kaisou.app.core.language.AppLanguage
import com.kaisou.app.core.translation.GoogleTranslator
import com.kaisou.app.features.home.data.CardUi
import com.kaisou.app.features.home.data.HomeMapper
import com.kaisou.app.features.title.data.TitleDatum
import com.kaisou.app.features.title.data.TitlePerson

// Asks AniList for the page of a character, of a person of the staff or of a studio and turns the answer into what the screen shows
class ProfileRepository(context: Context) {
    private val client = AniListClient()
    private val resources = context.resources
    private val mapper = HomeMapper(resources)
    private val staffRoles by lazy { StaffRoles(resources.getStringArray(R.array.title_staff_keys), resources.getStringArray(R.array.title_staff_names)) }
    // The parts of a character and the parts of the staff, in the language of the app
    private val roleNames by lazy {
        resources.getStringArray(R.array.title_role_keys).zip(resources.getStringArray(R.array.title_role_names)).toMap()
    }

    // The page, null when AniList could not be reached or does not know it
    suspend fun profile(ref: ProfileRef): ProfileUi? {
        val query = when (ref.kind) {
            ProfileKind.Character -> CharacterQuery
            ProfileKind.Staff -> StaffQuery
            ProfileKind.Studio -> StudioQuery
        }
        val root = client.query(query, JSONObject().put("id", ref.id), AniListSession.token) ?: return null
        val json = root.optJSONObject(ref.kind.path.replaceFirstChar { it.uppercase() }) ?: return null
        return when (ref.kind) {
            ProfileKind.Character -> character(ref, json)
            ProfileKind.Staff -> staff(ref, json)
            ProfileKind.Studio -> studio(ref, json)
        }
    }

    // A page of all the titles of a character, of a person of the staff or of a studio, 25 at a time, with the tab, the filter of what is on
    // the list of the user and the sort. A title says what the person is in it, and a voice role is the character with its title. Null when
    // AniList could not be reached
    suspend fun mediaPage(ref: ProfileRef, page: Int, tab: MediaTab, onList: Boolean, sort: MediaSort): ProfileMediaPage? {
        val voices = ref.kind == ProfileKind.Staff && tab == MediaTab.Voices
        val variables = JSONObject().put("id", ref.id).put("page", page).put("sort", sort.apiName)
            .put("type", tab.apiType ?: JSONObject.NULL).put("onList", if (onList) true else JSONObject.NULL)
        val query = when {
            ref.kind == ProfileKind.Character -> CharacterMediaQuery
            voices -> StaffVoicesQuery
            ref.kind == ProfileKind.Staff -> StaffMediaQuery
            else -> StudioMediaQuery
        }
        val root = client.query(query, variables, AniListSession.token) ?: return null
        val key = when {
            voices -> "characterMedia"
            ref.kind == ProfileKind.Staff -> "staffMedia"
            else -> "media"
        }
        val block = root.optJSONObject(ref.kind.path.replaceFirstChar { it.uppercase() })?.optJSONObject(key) ?: return null
        val items = when {
            ref.kind == ProfileKind.Studio -> objects(block.optJSONArray("nodes")).mapNotNull { node ->
                AniShelvesParser.card(node)?.let { card -> ProfileMediaItem("${card.id}", mapper.card(card, details = mapper.size(card)), null, card.year) }
            }
            voices -> objects(block.optJSONArray("edges")).flatMap { edge ->
                val card = edge.optJSONObject("node")?.let(AniShelvesParser::card) ?: return@flatMap emptyList()
                objects(edge.optJSONArray("characters")).mapNotNull { character ->
                    val id = character.optInt("id").takeIf { it > 0 } ?: return@mapNotNull null
                    val name = character.optJSONObject("name")?.let { text(it, "userPreferred") } ?: return@mapNotNull null
                    val person = TitlePerson(
                        name, character.optJSONObject("image")?.let { text(it, "large") }, card.title, roleNames[text(edge, "characterRole")],
                        "https://anilist.co/character/$id", ProfileRef(ProfileKind.Character, id)
                    )
                    ProfileMediaItem("$id-${card.id}", null, person, card.year)
                }
            }
            else -> objects(block.optJSONArray("edges")).mapNotNull { edge ->
                val card = edge.optJSONObject("node")?.let(AniShelvesParser::card) ?: return@mapNotNull null
                val role = role(edge)
                ProfileMediaItem("${card.id}-$role", mapper.card(card, details = role ?: mapper.size(card)), null, card.year)
            }
        }
        return ProfileMediaPage(items, block.optJSONObject("pageInfo")?.optBoolean("hasNextPage") == true)
    }

    // What a person is in a title: the part of a character, or what a person of the staff did, in the language of the app when it is known
    private fun role(edge: JSONObject): String? {
        val raw = text(edge, "characterRole") ?: text(edge, "staffRole") ?: return null
        return roleNames[raw] ?: staffRoles.translate(raw)
    }

    // The favourite of the page, which AniList turns on or off. True when it did
    suspend fun toggleFavourite(ref: ProfileRef): Boolean =
        client.query(ToggleQuery, JSONObject().put(ref.kind.path + "Id", ref.id), AniListSession.token) != null

    private suspend fun character(ref: ProfileRef, json: JSONObject): ProfileUi {
        val edges = objects(json.optJSONObject("media")?.optJSONArray("edges"))
        // Each voice once, with the language and, when AniList says, the note of what part of the story it is
        val voices = edges.flatMap { objects(it.optJSONArray("voiceActorRoles")) }.distinctBy { it.optJSONObject("voiceActor")?.optInt("id") }.mapNotNull { role ->
            val voice = role.optJSONObject("voiceActor") ?: return@mapNotNull null
            val id = voice.optInt("id").takeIf { it > 0 } ?: return@mapNotNull null
            val name = voice.optJSONObject("name")?.let { text(it, "userPreferred") } ?: return@mapNotNull null
            val line = text(role, "roleNotes")
            TitlePerson(name, voice.optJSONObject("image")?.let { text(it, "large") }, line, null, "https://anilist.co/staff/$id", ProfileRef(ProfileKind.Staff, id), text(voice, "languageV2"))
        }.sortedWith(compareBy(Collator.getInstance(Locale.getDefault())) { it.name })
        val about = about(
            text(json, "description"),
            json.optJSONObject("name")?.optJSONArray("alternativeSpoiler")?.let(::strings).orEmpty(),
            known = listOf(R.string.profile_gender, R.string.profile_age, R.string.profile_birthday, R.string.profile_blood, R.string.profile_nicknames).map(resources::getString)
        )
        return ProfileUi(
            ref = ref,
            name = json.optJSONObject("name")?.let { text(it, "userPreferred") }.orEmpty(),
            nativeName = json.optJSONObject("name")?.let { text(it, "native") },
            image = json.optJSONObject("image")?.let { text(it, "large") },
            favourites = json.optInt("favourites"),
            facts = facts(
                R.string.profile_gender to listOfNotNull(gender(text(json, "gender"))),
                R.string.profile_age to listOfNotNull(text(json, "age")),
                R.string.profile_birthday to listOfNotNull(json.optJSONObject("dateOfBirth")?.let(::dateText)),
                R.string.profile_blood to listOfNotNull(text(json, "bloodType")),
                R.string.profile_nicknames to strings(json.optJSONObject("name")?.optJSONArray("alternative"))
            ) + about.facts,
            description = about.text,
            isFavourite = json.optBoolean("isFavourite"),
            siteUrl = text(json, "siteUrl"),
            sections = listOf(
                ProfileSection(R.string.profile_voices, people = voices),
                ProfileSection(R.string.profile_appears, cards = roleCards(edges), tab = MediaTab.Anime)
            )
        )
    }

    private suspend fun staff(ref: ProfileRef, json: JSONObject): ProfileUi {
        val voiced = objects(json.optJSONObject("characters")?.optJSONArray("nodes")).mapNotNull { node ->
            val id = node.optInt("id").takeIf { it > 0 } ?: return@mapNotNull null
            val name = node.optJSONObject("name")?.let { text(it, "userPreferred") } ?: return@mapNotNull null
            TitlePerson(name, node.optJSONObject("image")?.let { text(it, "large") }, null, null, "https://anilist.co/character/$id", ProfileRef(ProfileKind.Character, id))
        }
        val works = objects(json.optJSONObject("staffMedia")?.optJSONArray("edges"))
        val about = about(
            text(json, "description"),
            emptyList(),
            known = listOf(R.string.profile_gender, R.string.profile_age, R.string.profile_birthday, R.string.profile_died, R.string.profile_home, R.string.profile_years, R.string.profile_occupations, R.string.profile_language).map(resources::getString)
        )
        return ProfileUi(
            ref = ref,
            name = json.optJSONObject("name")?.let { text(it, "userPreferred") }.orEmpty(),
            nativeName = json.optJSONObject("name")?.let { text(it, "native") },
            image = json.optJSONObject("image")?.let { text(it, "large") },
            favourites = json.optInt("favourites"),
            facts = facts(
                R.string.profile_occupations to strings(json.optJSONArray("primaryOccupations")),
                R.string.profile_gender to listOfNotNull(gender(text(json, "gender"))),
                R.string.profile_age to listOfNotNull(text(json, "age")),
                R.string.profile_birthday to listOfNotNull(json.optJSONObject("dateOfBirth")?.let(::dateText)),
                R.string.profile_died to listOfNotNull(json.optJSONObject("dateOfDeath")?.let(::dateText)),
                R.string.profile_home to listOfNotNull(text(json, "homeTown")),
                R.string.profile_years to years(json.optJSONArray("yearsActive")),
                R.string.profile_language to listOfNotNull(text(json, "languageV2"))
            ) + about.facts,
            description = about.text,
            isFavourite = json.optBoolean("isFavourite"),
            siteUrl = text(json, "siteUrl"),
            sections = listOf(
                ProfileSection(R.string.profile_voiced, people = voiced, tab = MediaTab.Voices),
                ProfileSection(R.string.profile_works, cards = roleCards(works), tab = MediaTab.Anime)
            )
        )
    }

    private fun studio(ref: ProfileRef, json: JSONObject): ProfileUi =
        ProfileUi(
            ref = ref,
            name = text(json, "name").orEmpty(),
            nativeName = null,
            image = null,
            favourites = json.optInt("favourites"),
            facts = emptyList(),
            description = "",
            isFavourite = json.optBoolean("isFavourite"),
            siteUrl = text(json, "siteUrl"),
            sections = listOf(ProfileSection(R.string.profile_productions, cards = cards(objects(json.optJSONObject("media")?.optJSONArray("nodes")))))
        )

    // What a description says of itself: the text ready to show and the lines of data that opened it
    private class About(val text: String, val facts: List<TitleDatum>)

    // The description in the language of the app, with what gives away the story still between its marks. The lines that open it with a name
    // in bold and a value, as __Height:__ 170 cm, are data and go to the data, and the names that give the story away go there too, covered
    private suspend fun about(raw: String?, hiddenNames: List<String>, known: List<String>): About {
        var text = raw.orEmpty()
        // A cover that crosses a break of paragraph is one piece, so it is covered whole
        text = Spoiler.replace(text) { "~!" + it.groupValues[1].replace(Paragraphs, "\n") + "!~" }
        if (AppLanguage.needsTranslation && text.isNotBlank()) text = AniDescription.tidy(translated(text))
        val lines = text.lines()
        val facts = mutableListOf<TitleDatum>()
        var used = 0
        while (used < lines.size) {
            val line = lines[used]
            if (line.isBlank()) {
                used++
                continue
            }
            val match = FactLine.matchEntire(line) ?: break
            val label = match.groupValues[1].trim().trimEnd(':').trim()
            val value = plain(match.groupValues[2])
            // The ones that AniList already gave in its own fields are not said twice
            if (known.none { it.equals(label, ignoreCase = true) } && value.isNotBlank()) facts += TitleDatum(label, listOf(value))
            used++
        }
        if (hiddenNames.isNotEmpty()) facts += TitleDatum(resources.getString(R.string.profile_nicknames_spoiler), listOf("~!${hiddenNames.joinToString(", ")}!~"))
        return About(AniDescription.markdown(lines.drop(used).joinToString("\n")), facts)
    }

    // A value of data without the marks of the format, which the data does not show, and with the covers kept
    private fun plain(value: String): String =
        value.replace(Regex("(\\*\\*|__|~~)(.+?)\\1"), "$2").replace(Regex("(?<![\\w*_])([*_])([^*_\\n]+)\\1(?![\\w*_])"), "$2").trim()

    // A long text in pieces of whole paragraphs, since the translator takes a bit at a time. What goes past the pieces that are asked stays
    // as it was
    private suspend fun translated(text: String): String {
        val pieces = mutableListOf<String>()
        var current = StringBuilder()
        text.split(Paragraphs).forEach { paragraph ->
            if (current.isNotEmpty() && current.length + paragraph.length > PieceLength) {
                pieces += current.toString()
                current = StringBuilder()
            }
            if (current.isNotEmpty()) current.append("\n\n")
            current.append(paragraph)
        }
        if (current.isNotEmpty()) pieces += current.toString()
        return pieces.mapIndexed { index, piece ->
            if (index >= MaxPieces) piece else GoogleTranslator.translate(piece, AppLanguage.translationCode) ?: piece
        }.joinToString("\n\n")
    }

    // The cards of the titles, each once, with what the person is in each under it
    private fun roleCards(edges: List<JSONObject>): List<CardUi> =
        edges.mapNotNull { edge ->
            val card = edge.optJSONObject("node")?.let(AniShelvesParser::card) ?: return@mapNotNull null
            mapper.card(card, details = role(edge) ?: mapper.size(card))
        }.distinctBy { it.anilistId }

    // The cards of the titles, each once, with the kind of title and its year in the line under it
    private fun cards(nodes: List<JSONObject>) =
        nodes.mapNotNull { AniShelvesParser.card(it) }.distinctBy { it.id }.map { card -> mapper.card(card, details = mapper.size(card)) }

    private fun facts(vararg lines: Pair<Int, List<String>>): List<TitleDatum> =
        lines.filter { it.second.isNotEmpty() }.map { (label, values) -> TitleDatum(resources.getString(label), values) }

    // The words AniList uses for a gender, in the language of the app for the two that are the most
    private fun gender(value: String?): String? =
        when (value?.lowercase()) {
            "female" -> resources.getString(R.string.profile_female)
            "male" -> resources.getString(R.string.profile_male)
            else -> value
        }

    // The years a person worked: the ones that are given, a start and an end or only a start, as 2010 to 2020 or 2010 to today
    private fun years(list: JSONArray?): List<String> {
        val years = (0 until (list?.length() ?: 0)).map { list!!.optInt(it) }
        val first = years.firstOrNull() ?: return emptyList()
        return listOf(resources.getString(R.string.profile_years_range, first, years.getOrNull(1)?.toString() ?: resources.getString(R.string.profile_today)))
    }

    // A day as it is written in the language of the phone, all of it, the month and the year, or only the month and the day when there
    // is no year
    private fun dateText(json: JSONObject): String? {
        val year = json.optInt("year").takeIf { it > 0 }
        val month = json.optInt("month").takeIf { it > 0 }
        val day = json.optInt("day").takeIf { it > 0 }
        val locale = Locale.getDefault()
        return runCatching {
            when {
                year != null && month != null && day != null -> LocalDate.of(year, month, day).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
                month != null && day != null -> LocalDate.of(YEAR_FOR_DAY, month, day).format(DateTimeFormatter.ofPattern(DAY_PATTERN, locale))
                year != null && month != null -> YearMonth.of(year, month).format(DateTimeFormatter.ofPattern(MONTH_PATTERN, locale))
                year != null -> NumberFormat.getIntegerInstance(locale).apply { isGroupingUsed = false }.format(year)
                else -> null
            }
        }.getOrNull()
    }

    private fun objects(list: JSONArray?): List<JSONObject> = if (list == null) emptyList() else (0 until list.length()).map { list.getJSONObject(it) }

    private fun strings(list: JSONArray?): List<String> =
        if (list == null) emptyList() else (0 until list.length()).mapNotNull { list.optString(it).takeIf { text -> text.isNotBlank() } }

    private fun text(json: JSONObject, key: String): String? = if (json.isNull(key)) null else json.optString(key).takeIf { it.isNotBlank() && it != "null" }

    private companion object {
        // A leap year, so the day of a birthday without a year can be 29 of February
        // The line of a description that tells the blood type, written as AniList writes them, and the covered parts and the breaks
        val FactLine = Regex("^\\s*(?:__|\\*\\*)\\s*([^_*\\n]{1,40}?)\\s*:?\\s*(?:__|\\*\\*)\\s*:?\\s*(\\S.*)$")
        val Spoiler = Regex("~!(.+?)!~", RegexOption.DOT_MATCHES_ALL)
        val Paragraphs = Regex("\n{2,}")
        // How long a piece for the translator can be and how many of them are asked
        const val PieceLength = 1_500
        const val MaxPieces = 4
        const val YEAR_FOR_DAY = 2000
        const val DAY_PATTERN = "d MMMM"
        const val MONTH_PATTERN = "MMMM yyyy"

        const val CharacterQuery = "query (\$id: Int) { Character(id: \$id) { id name { userPreferred native alternative alternativeSpoiler } image { large } description(asHtml: false) " +
            "gender age dateOfBirth { year month day } bloodType favourites isFavourite siteUrl " +
            "media(sort: POPULARITY_DESC, perPage: 25) { edges { characterRole voiceActorRoles(sort: [RELEVANCE, ID]) { roleNotes voiceActor { id name { userPreferred } image { large } languageV2 } } " +
            "node { ${AniShelvesParser.CardFields} } } } } }"
        const val StaffQuery = "query (\$id: Int) { Staff(id: \$id) { id name { userPreferred native } image { large } description(asHtml: false) " +
            "gender age dateOfBirth { year month day } dateOfDeath { year month day } yearsActive homeTown primaryOccupations languageV2 favourites isFavourite siteUrl " +
            "characters(sort: RELEVANCE, perPage: 25) { nodes { id name { userPreferred } image { large } } } " +
            "staffMedia(sort: POPULARITY_DESC, perPage: 25) { edges { staffRole node { ${AniShelvesParser.CardFields} } } } } }"
        const val StudioQuery = "query (\$id: Int) { Studio(id: \$id) { id name favourites isFavourite siteUrl " +
            "media(sort: POPULARITY_DESC, perPage: 25) { nodes { ${AniShelvesParser.CardFields} } } } }"
        const val CharacterMediaQuery = "query (\$id: Int, \$page: Int, \$sort: [MediaSort], \$type: MediaType, \$onList: Boolean) { Character(id: \$id) { " +
            "media(page: \$page, perPage: 25, sort: \$sort, type: \$type, onList: \$onList) { pageInfo { hasNextPage } edges { characterRole node { ${AniShelvesParser.CardFields} } } } } }"
        const val StaffMediaQuery = "query (\$id: Int, \$page: Int, \$sort: [MediaSort], \$type: MediaType, \$onList: Boolean) { Staff(id: \$id) { " +
            "staffMedia(page: \$page, perPage: 25, sort: \$sort, type: \$type, onList: \$onList) { pageInfo { hasNextPage } edges { staffRole node { ${AniShelvesParser.CardFields} } } } } }"
        const val StaffVoicesQuery = "query (\$id: Int, \$page: Int, \$sort: [MediaSort], \$onList: Boolean) { Staff(id: \$id) { " +
            "characterMedia(page: \$page, perPage: 25, sort: \$sort, onList: \$onList) { pageInfo { hasNextPage } edges { characterRole " +
            "characters { id name { userPreferred } image { large } } node { ${AniShelvesParser.CardFields} } } } } }"
        const val StudioMediaQuery = "query (\$id: Int, \$page: Int, \$sort: [MediaSort], \$onList: Boolean) { Studio(id: \$id) { " +
            "media(page: \$page, perPage: 25, sort: \$sort, onList: \$onList) { pageInfo { hasNextPage } nodes { ${AniShelvesParser.CardFields} } } } }"
        const val ToggleQuery = "mutation (\$characterId: Int, \$staffId: Int, \$studioId: Int) { " +
            "ToggleFavourite(characterId: \$characterId, staffId: \$staffId, studioId: \$studioId) { anime { pageInfo { total } } } }"
    }
}
