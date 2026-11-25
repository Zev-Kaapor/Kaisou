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
import com.kaisou.app.core.language.DubLanguage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import com.kaisou.app.core.anilist.AboutSnippet
import com.kaisou.app.core.cache.TimedCache

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

    // Whether a page of AniList has one more after it
    private fun hasMore(block: JSONObject?): Boolean = block?.optJSONObject("pageInfo")?.optBoolean("hasNextPage") == true

    // The page, null when AniList could not be reached or does not know it
    suspend fun profile(ref: ProfileRef): ProfileUi? {
        val query = when (ref.kind) {
            ProfileKind.Character -> CharacterQuery
            ProfileKind.Staff -> StaffQuery
            ProfileKind.Studio -> StudioQuery
        }
        pages.get(ref.hashCode())?.let { return it.value }
        val root = client.query(query, JSONObject().put("id", ref.id), AniListSession.token) ?: return null
        val json = root.optJSONObject(ref.kind.path.replaceFirstChar { it.uppercase() }) ?: return null
        val found = when (ref.kind) {
            ProfileKind.Character -> character(ref, json)
            ProfileKind.Staff -> staff(ref, json)
            ProfileKind.Studio -> studio(ref, json)
        }
        pages.put(ref.hashCode(), found)
        return found
    }

    // A page of all the titles of a person, 25 at a time, with the tab, the list filter and the sort, null when AniList failed
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

    // What a person is in a title: the part of a character or what the staff did, in the language of the app when known
    private fun role(edge: JSONObject): String? {
        val raw = text(edge, "characterRole") ?: text(edge, "staffRole") ?: return null
        return roleNames[raw] ?: staffRoles.translate(raw)
    }

    // The beginning of what each person of the staff says of itself, 50 at a time, only for the page of all the voices
    suspend fun aboutOf(ids: List<Int>): Map<Int, String> {
        val found = LinkedHashMap<Int, String>()
        ids.distinct().chunked(AboutBatch).forEach { batch ->
            val root = client.query(AboutQuery, JSONObject().put("ids", JSONArray(batch)), AniListSession.token) ?: return@forEach
            objects(root.optJSONObject("Page")?.optJSONArray("staff")).forEach { person ->
                AboutSnippet.of(text(person, "description"))?.let { found[person.optInt("id")] = it }
            }
        }
        return found.keys.zip(AboutSnippet.translated(found.values.toList())).toMap()
    }

    // The favourite of the page, which AniList turns on or off. True when it did
    suspend fun toggleFavourite(ref: ProfileRef): Boolean =
        (client.query(ToggleQuery, JSONObject().put(ref.kind.path + "Id", ref.id), AniListSession.token) != null).also { if (it) pages.remove(ref.hashCode()) }

    // Builds the page of a character
    private suspend fun character(ref: ProfileRef, json: JSONObject): ProfileUi = coroutineScope {
        val edges = objects(json.optJSONObject("media")?.optJSONArray("edges"))
        val collator = Collator.getInstance(Locale.getDefault())

        // Each voice once, with the language and, when AniList says, the note of what part of the story it is
        val heard = edges.flatMap { objects(it.optJSONArray("voiceActorRoles")) }.distinctBy { it.optJSONObject("voiceActor")?.optInt("id") }.mapNotNull { role ->
            val voice = role.optJSONObject("voiceActor") ?: return@mapNotNull null
            val id = voice.optInt("id").takeIf { it > 0 } ?: return@mapNotNull null
            val name = voice.optJSONObject("name")?.let { text(it, "userPreferred") } ?: return@mapNotNull null
            val line = text(role, "roleNotes")
            TitlePerson(name, voice.optJSONObject("image")?.let { text(it, "large") }, line, null, "https://anilist.co/staff/$id", ProfileRef(ProfileKind.Staff, id), text(voice, "languageV2"))
        }.sortedWith { a, b -> DubLanguage.compare(a.language.orEmpty(), b.language.orEmpty()).takeIf { it != 0 } ?: collator.compare(a.name, b.name) }

        // The age, the notes of the voices and the text are translated at the same time
        val ageJob = async {
            text(json, "age")?.let { raw ->
                if (AppLanguage.needsTranslation && raw.any(Char::isLetter)) GoogleTranslator.translate(raw, AppLanguage.translationCode) ?: raw else raw
            }
        }

        // The note of what part of the story it is comes in English, as Child or Young, so the ones that came are translated together
        val notesJob = async {
            val notes = heard.mapNotNull { it.role }.distinct()
            if (AppLanguage.needsTranslation && notes.isNotEmpty()) notes.zip(GoogleTranslator.translateLines(notes, AppLanguage.translationCode)).toMap() else emptyMap()
        }
        val about = about(
            text(json, "description"),
            json.optJSONObject("name")?.optJSONArray("alternativeSpoiler")?.let(::strings).orEmpty(),
            known = listOf(R.string.profile_gender, R.string.profile_age, R.string.profile_birthday, R.string.profile_blood, R.string.profile_nicknames).map(resources::getString)
        )
        val age = ageJob.await()
        val translatedNotes = notesJob.await()
        val voices = heard.map { person -> person.copy(role = person.role?.let { translatedNotes[it] ?: it }) }
        ProfileUi(
            ref = ref,
            name = json.optJSONObject("name")?.let { text(it, "userPreferred") }.orEmpty(),
            nativeName = json.optJSONObject("name")?.let { text(it, "native") },
            image = json.optJSONObject("image")?.let { text(it, "large") },
            favourites = json.optInt("favourites"),
            facts = facts(
                R.string.profile_gender to listOfNotNull(gender(text(json, "gender"))),
                R.string.profile_age to listOfNotNull(age),
                R.string.profile_birthday to listOfNotNull(json.optJSONObject("dateOfBirth")?.let(::dateText)),
                R.string.profile_blood to listOfNotNull(text(json, "bloodType")),
                R.string.profile_nicknames to strings(json.optJSONObject("name")?.optJSONArray("alternative"))
            ) + about.facts,
            description = about.text,
            isFavourite = json.optBoolean("isFavourite"),
            siteUrl = text(json, "siteUrl"),
            sections = listOf(
                ProfileSection(R.string.profile_voices, people = voices),
                ProfileSection(R.string.profile_appears, cards = roleCards(edges), tab = MediaTab.Anime, more = hasMore(json.optJSONObject("media")))
            )
        )
    }

    // Builds the page of a person of the staff
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
                ProfileSection(R.string.profile_voiced, people = voiced, tab = MediaTab.Voices, more = hasMore(json.optJSONObject("characters"))),
                ProfileSection(R.string.profile_works, cards = roleCards(works), tab = MediaTab.Anime, more = hasMore(json.optJSONObject("staffMedia")))
            )
        )
    }

    // Builds the page of a studio
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

    // The description in the language of the app, with the spoilers kept between their marks and the lines of facts moved to the data
    private suspend fun about(raw: String?, hiddenNames: List<String>, known: List<String>): About {
        var text = raw.orEmpty()

        // A cover that crosses a break of paragraph is one piece, so it is covered whole
        text = Spoiler.replace(text) { "~!" + it.groupValues[1].replace(Paragraphs, "\n") + "!~" }
        if (AppLanguage.needsTranslation && text.isNotBlank()) text = AniDescription.tidy(translated(text))
        val lines = text.lines()
        val facts = mutableListOf<TitleDatum>()
        var used = 0

        // A cover can open on a line of data and close some lines under it, and all of them in between are covered
        var covered = false
        while (used < lines.size) {
            val line = lines[used]
            if (line.isBlank()) {
                used++
                continue
            }
            var body = line.trim()
            if (body.startsWith("~!")) {
                covered = true
                body = body.removePrefix("~!")
            }
            val closes = body.endsWith("!~") && !body.contains("~!")
            if (closes) body = body.removeSuffix("!~")
            val match = FactLine.matchEntire(body) ?: break
            val label = match.groupValues[1].trim().trimEnd(':').trim()
            val value = plain(match.groupValues[2]).let { if (covered && it.isNotBlank()) "~!$it!~" else it }
            if (closes) covered = false

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

    // A long text in pieces of whole paragraphs, since the translator takes a bit at a time, what is past the pieces staying as it was
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
        return coroutineScope {
            pieces.mapIndexed { index, piece ->
                async { if (index >= MaxPieces) piece else GoogleTranslator.translate(piece, AppLanguage.translationCode) ?: piece }
            }.awaitAll()
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

    // Builds the data lines of a page from labels and values, leaving out the empty ones
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

    // A day as written in the language of the phone, in full or only the month and the day when there is no year
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

    // Reads a JSON array as objects
    private fun objects(list: JSONArray?): List<JSONObject> = if (list == null) emptyList() else (0 until list.length()).map { list.getJSONObject(it) }

    // Reads a JSON array as texts
    private fun strings(list: JSONArray?): List<String> =
        if (list == null) emptyList() else (0 until list.length()).mapNotNull { list.optString(it).takeIf { text -> text.isNotBlank() } }

    // Reads a text of a key, null when it is missing or empty
    private fun text(json: JSONObject, key: String): String? = if (json.isNull(key)) null else json.optString(key).takeIf { it.isNotBlank() && it != "null" }

    // Constants of the repository
    private companion object {
        // A leap year for a birthday without a year, and the line of a description that gives a fact, written as AniList writes it
        val FactLine = Regex("^\\s*(?:__|\\*\\*)\\s*([^_*\\n]{1,40}?)\\s*:?\\s*(?:__|\\*\\*)\\s*:?\\s*(\\S.*)$")
        val Spoiler = Regex("~!(.+?)!~", RegexOption.DOT_MATCHES_ALL)
        val Paragraphs = Regex("\n{2,}")

        // How long a piece for the translator can be and how many of them are asked
        const val PieceLength = 1_500
        const val MaxPieces = 4
        const val YEAR_FOR_DAY = 2000
        const val DAY_PATTERN = "d MMMM"
        const val MONTH_PATTERN = "MMMM yyyy"

        // How many people of the staff are asked at once for what they say of themselves
        const val AboutBatch = 50

        // The pages that were opened, kept for two minutes, so going to the voices and back does not ask again
        private const val PagesMillis = 2L * 60L * 1000L
        private const val MaxPages = 20
        private val pages = TimedCache<ProfileUi>(PagesMillis, MaxPages)

        // Queries of the beginning of what the staff says and of a character
        const val AboutQuery = "query (\$ids: [Int]) { Page(perPage: 50) { staff(id_in: \$ids) { id description(asHtml: false) } } }"
        const val CharacterQuery = "query (\$id: Int) { Character(id: \$id) { id name { userPreferred native alternative alternativeSpoiler } image { large } description(asHtml: false) " +
            "gender age dateOfBirth { year month day } bloodType favourites isFavourite siteUrl " +
            "media(sort: POPULARITY_DESC, perPage: 25) { pageInfo { hasNextPage } edges { characterRole voiceActorRoles(sort: [RELEVANCE, ID]) { roleNotes voiceActor { id name { userPreferred } image { large } languageV2 } } " +
            "node { ${AniShelvesParser.CardFields} } } } } }"

        // Query of a person of the staff
        const val StaffQuery = "query (\$id: Int) { Staff(id: \$id) { id name { userPreferred native } image { large } description(asHtml: false) " +
            "gender age dateOfBirth { year month day } dateOfDeath { year month day } yearsActive homeTown primaryOccupations languageV2 favourites isFavourite siteUrl " +
            "characters(sort: RELEVANCE, perPage: 25) { pageInfo { hasNextPage } nodes { id name { userPreferred } image { large } } } " +
            "staffMedia(sort: POPULARITY_DESC, perPage: 25) { pageInfo { hasNextPage } edges { staffRole node { ${AniShelvesParser.CardFields} } } } } }"

        // Query of a studio
        const val StudioQuery = "query (\$id: Int) { Studio(id: \$id) { id name favourites isFavourite siteUrl " +
            "media(sort: POPULARITY_DESC, perPage: 25) { nodes { ${AniShelvesParser.CardFields} } } } }"

        // Query of the titles of a character
        const val CharacterMediaQuery = "query (\$id: Int, \$page: Int, \$sort: [MediaSort], \$type: MediaType, \$onList: Boolean) { Character(id: \$id) { " +
            "media(page: \$page, perPage: 25, sort: \$sort, type: \$type, onList: \$onList) { pageInfo { hasNextPage } edges { characterRole node { ${AniShelvesParser.CardFields} } } } } }"

        // Query of the titles of a person of the staff
        const val StaffMediaQuery = "query (\$id: Int, \$page: Int, \$sort: [MediaSort], \$type: MediaType, \$onList: Boolean) { Staff(id: \$id) { " +
            "staffMedia(page: \$page, perPage: 25, sort: \$sort, type: \$type, onList: \$onList) { pageInfo { hasNextPage } edges { staffRole node { ${AniShelvesParser.CardFields} } } } } }"

        // Query of the voices of a person of the staff
        const val StaffVoicesQuery = "query (\$id: Int, \$page: Int, \$sort: [MediaSort], \$onList: Boolean) { Staff(id: \$id) { " +
            "characterMedia(page: \$page, perPage: 25, sort: \$sort, onList: \$onList) { pageInfo { hasNextPage } edges { characterRole " +
            "characters { id name { userPreferred } image { large } } node { ${AniShelvesParser.CardFields} } } } } }"

        // Query of the titles of a studio
        const val StudioMediaQuery = "query (\$id: Int, \$page: Int, \$sort: [MediaSort], \$onList: Boolean) { Studio(id: \$id) { " +
            "media(page: \$page, perPage: 25, sort: \$sort, onList: \$onList) { pageInfo { hasNextPage } nodes { ${AniShelvesParser.CardFields} } } } }"

        // Mutation that sets a favourite
        const val ToggleQuery = "mutation (\$characterId: Int, \$staffId: Int, \$studioId: Int) { " +
            "ToggleFavourite(characterId: \$characterId, staffId: \$staffId, studioId: \$studioId) { anime { pageInfo { total } } } }"
    }
}
