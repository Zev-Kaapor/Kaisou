package com.kaisou.app.core.anilist

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AniShelvesParserTest {
    private fun media(id: Int, english: String? = null, romaji: String = "Name", score: Int? = 80, adult: Boolean = false) =
        JSONObject()
            .put("id", id)
            .put("title", JSONObject().put("english", english ?: JSONObject.NULL).put("romaji", romaji))
            .put("averageScore", score ?: JSONObject.NULL)
            .put("bannerImage", JSONObject.NULL)
            .put("isAdult", adult)
            .put("coverImage", JSONObject().put("extraLarge", "https://x/$id.jpg").put("large", "https://x/$id-small.jpg"))

    private fun list(vararg entries: JSONObject) =
        JSONObject().put("lists", JSONArray().put(JSONObject().put("entries", JSONArray(entries.toList()))))

    private fun entry(id: Int, progress: Int, english: String? = null, score: Int? = 80) =
        JSONObject().put("progress", progress).put("media", media(id, english, score = score))

    @Test
    fun theEnglishNameComesFirstAndTheRomajiIsTheFallback() {
        val data = JSONObject().put("watching", list(entry(1, 1, english = "English"), entry(2, 1)))
        val cards = AniShelvesParser.parse(data).watching.map { it.card }
        assertEquals("English", cards[0].title)
        assertEquals("Name", cards[1].title)
    }

    @Test
    fun theCoverIsTheBigOneAndAMissingScoreIsNothing() {
        val data = JSONObject().put("plannedAnime", list(entry(7, 0, score = null)))
        val card = AniShelvesParser.parse(data).plannedAnime.single().card
        assertEquals("https://x/7.jpg", card.cover)
        assertNull(card.score)
    }

    @Test
    fun theEntriesKeepTheirProgressAndOrder() {
        val data = JSONObject().put("reading", list(entry(1, 4), entry(2, 0)))
        val reading = AniShelvesParser.parse(data).reading
        assertEquals(listOf(1, 2), reading.map { it.card.id })
        assertEquals(listOf(4, 0), reading.map { it.progress })
    }

    @Test
    fun aTitleThatAiredTwiceShowsOnceWithItsNewestEpisodeAndAdultsAreLeftOut() {
        val aired = { id: Int, episode: Int, at: Long, adult: Boolean ->
            JSONObject().put("episode", episode).put("airingAt", at).put("media", media(id, adult = adult))
        }
        val schedules = JSONArray().put(aired(1, 8, 300, false)).put(aired(1, 7, 100, false)).put(aired(2, 3, 200, true))
        val airing = AniShelvesParser.parse(JSONObject().put("airing", JSONObject().put("airingSchedules", schedules))).airing
        assertEquals(1, airing.size)
        assertEquals(8, airing.single().episode)
        assertEquals(300L, airing.single().airedAt)
    }

    @Test
    fun theActivitiesOfListsAndTextsComeWithTheAdultOnesMarkedAndPrivateMessagesAndOnesWithNoUserAreLeftOut() {
        val user = JSONObject().put("id", 7).put("name", "Friend").put("avatar", JSONObject().put("large", "https://x/a7.png"))
        val list = { id: Int, adult: Boolean ->
            JSONObject().put("__typename", "ListActivity").put("id", id).put("createdAt", 1000L + id).put("status", "watched episode")
                .put("progress", "1 - 4").put("user", user).put("media", media(id, adult = adult))
        }
        val text = JSONObject().put("__typename", "TextActivity").put("id", 3).put("createdAt", 1003L).put("text", "hello").put("user", user)
        val message = JSONObject().put("__typename", "MessageActivity").put("id", 4).put("user", user)
        val noUser = JSONObject().put("__typename", "TextActivity").put("id", 5).put("text", "x")
        val items = JSONArray().put(list(1, false)).put(list(2, true)).put(text).put(message).put(noUser)
        val found = AniShelvesParser.parse(JSONObject().put("activity", JSONObject().put("activities", items))).following
        assertEquals(listOf(1, 2, 3), found.map { it.id })
        assertEquals(listOf(false, true, false), found.map { it.adult })
        assertEquals("1 - 4", found.first().progress)
        assertEquals(7, found.first().userId)
        assertEquals("https://x/a7.png", found.first().avatar)
        assertEquals("hello", found.last().text)
        assertNull(found.last().card)
    }

    @Test
    fun theMeanScoreFillsInWhenThereIsNoWeightedOne() {
        val data = JSONObject().put("plannedManga", list(JSONObject().put("progress", 0).put("media", media(3, score = null).put("meanScore", 86))))
        assertEquals(86, AniShelvesParser.parse(data).plannedManga.single().card.score)
    }

    @Test
    fun theCountOfEpisodesAndChaptersIsKnownOnlyWhenAniListHasIt() {
        val anime = media(1).put("format", "TV").put("episodes", 12).put("chapters", JSONObject.NULL)
        val data = JSONObject().put("watching", list(JSONObject().put("progress", 1).put("media", anime)))
        val card = AniShelvesParser.parse(data).watching.single().card
        assertEquals("TV", card.format)
        assertEquals(12, card.episodes)
        assertNull(card.chapters)
    }

    @Test
    fun anAiringTitleWithNoTotalKnowsHowManyEpisodesAiredFromTheNextOne() {
        val airing = media(1).put("nextAiringEpisode", JSONObject().put("episode", 8))
        val first = media(2).put("nextAiringEpisode", JSONObject().put("episode", 1))
        val data = JSONObject().put("popular", JSONObject().put("media", JSONArray().put(airing).put(first).put(media(3))))
        assertEquals(listOf<Int?>(7, null, null), AniShelvesParser.parse(data).popular.map { it.aired })
    }

    @Test
    fun recommendationsLeaveOutWhatIsOnTheListAndAdultsAndRepeatsAndSortByScore() {
        val rec = { id: Int, type: String, score: Int, onList: Boolean, adult: Boolean ->
            JSONObject().put("mediaRecommendation", media(id, score = score).put("type", type).put("isAdult", adult)
                .put("mediaListEntry", if (onList) JSONObject().put("status", "CURRENT") else JSONObject.NULL))
        }
        val byRating = JSONObject().put("recommendations", JSONArray()
            .put(rec(1, "ANIME", 70, false, false)).put(rec(2, "ANIME", 90, false, false)).put(rec(3, "ANIME", 95, true, false))
            .put(rec(4, "ANIME", 99, false, true)).put(rec(5, "MANGA", 80, false, false)))
        val byNew = JSONObject().put("recommendations", JSONArray().put(rec(2, "ANIME", 90, false, false)))
        val seeds = JSONObject().put("lists", JSONArray().put(JSONObject().put("entries", JSONArray().put(JSONObject().put("media",
            JSONObject().put("recommendations", JSONObject().put("nodes", JSONArray().put(rec(6, "MANGA", 85, false, false)))))))))
        val result = AniShelvesParser.parseRecommended(JSONObject().put("byRating", byRating).put("byNew", byNew).put("mangaSeeds", seeds))
        assertEquals(listOf(2, 1), result.anime.map { it.id })
        assertEquals(listOf(6, 5), result.manga.map { it.id })
    }

    @Test
    fun onlyTheAnimeSequelsOfWhatWasFinishedAreFoundEachOnce() {
        val edge = { type: String, kind: String, id: Int -> JSONObject().put("relationType", type).put("node", JSONObject().put("id", id).put("type", kind)) }
        val entry = { edges: JSONArray -> JSONObject().put("media", JSONObject().put("id", 1).put("relations", JSONObject().put("edges", edges))) }
        val completed = JSONObject().put("completed", JSONObject().put("lists", JSONArray().put(JSONObject().put("entries", JSONArray()
            .put(entry(JSONArray().put(edge("SEQUEL", "ANIME", 10)).put(edge("PREQUEL", "ANIME", 11)).put(edge("SEQUEL", "MANGA", 12))))
            .put(entry(JSONArray().put(edge("SEQUEL", "ANIME", 10)).put(edge("SEQUEL", "ANIME", 13))))))))
        assertEquals(listOf(10, 13), AniShelvesParser.sequelIds(completed))
    }

    @Test
    fun theSequelsFoundComeBestScoredFirst() {
        val data = JSONObject().put("sequels", JSONObject().put("media", JSONArray().put(media(1, score = 60)).put(media(2, score = 90))))
        assertEquals(listOf(2, 1), AniShelvesParser.parseSequels(data).map { it.id })
    }

    @Test
    fun theLikesAndTheCommentsOfAnActivityComeWithIt() {
        val item = JSONObject().put("__typename", "TextActivity").put("id", 9).put("createdAt", 1L).put("text", "hi")
            .put("likeCount", 4).put("replyCount", 2).put("isLiked", true).put("user", JSONObject().put("id", 1).put("name", "A"))
        val activity = AniShelvesParser.parse(JSONObject().put("mine", JSONObject().put("activities", JSONArray().put(item)))).mine.single()
        assertEquals(4, activity.likeCount)
        assertEquals(2, activity.replyCount)
        assertEquals(true, activity.isLiked)
    }

    @Test
    fun aCommentIsReadWithItsAuthorAndAnEmptyOneIsNot() {
        val json = JSONObject().put("id", 3).put("text", "nice").put("createdAt", 50L).put("likeCount", 1)
            .put("user", JSONObject().put("id", 7).put("name", "Friend").put("avatar", JSONObject().put("large", "https://x/a.png")))
        val reply = AniShelvesParser.reply(json)!!
        assertEquals("Friend", reply.userName)
        assertEquals("nice", reply.text)
        assertEquals(7, reply.userId)
        assertEquals("https://x/a.png", reply.avatar)
        assertNull(AniShelvesParser.reply(JSONObject()))
    }

    @Test
    fun twoPartsOfTheShelvesMergeIntoOneWithEachListWhole() {
        val top = AniShelvesParser.parse(JSONObject().put("watching", list(entry(1, 1))))
        val below = AniShelvesParser.parse(JSONObject().put("plannedAnime", list(entry(2, 0))))
        val merged = top.merge(below)
        assertEquals(listOf(1), merged.watching.map { it.card.id })
        assertEquals(listOf(2), merged.plannedAnime.map { it.card.id })
        assertEquals(0, merged.airing.size)
    }

    @Test
    fun theShelvesOfDiscoveryComeAsTitles() {
        val page = JSONObject().put("media", JSONArray().put(media(5)).put(media(6)))
        val shelves = AniShelvesParser.parse(JSONObject().put("trendingManga", page).put("newAnime", page))
        assertEquals(listOf(5, 6), shelves.trendingManga.map { it.id })
        assertEquals(2, shelves.newAnime.size)
        assertEquals(0, shelves.popular.size)
    }

    @Test
    fun aShelfThatIsNotThereIsEmpty() {
        val shelves = AniShelvesParser.parse(JSONObject())
        assertEquals(0, shelves.following.size + shelves.mine.size + shelves.watching.size + shelves.reading.size + shelves.plannedAnime.size + shelves.plannedManga.size + shelves.airing.size)
    }
}
