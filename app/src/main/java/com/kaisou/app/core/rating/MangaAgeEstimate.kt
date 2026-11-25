package com.kaisou.app.core.rating

// A tag of a title as AniList gives it: its name, how much people say it applies, whether it spoils and whether it is adult
class AgeTag(val name: String, val rank: Int, val spoiler: Boolean, val adult: Boolean)

// The age a manga is for, worked out from its tags and genres, in the steps of the Brazilian rating: L, 10, 12, 14, 16 and 18
object MangaAgeEstimate {
    // The thresholds of the estimate of the age of a manga
    private const val SpoilerShare = 0.9
    private const val AdultMinRank = 30
    private const val ShounenDark = 62
    private const val Light = 10

    // The age each tag or genre gives. The ones that give none are not here, and a genre counts as a tag with all the votes
    private val Levels = mapOf(
        "Ecchi12" to 12, "Ecchi" to 14, "Horror" to 14, "Thriller" to 12, "Hentai" to 18,
        "Bullying" to 12, "Martial Arts" to 12, "Swordplay" to 12, "Survival" to 12, "Tragedy" to 12, "War" to 12, "Pregnancy" to 12,
        "Slavery" to 12, "Kidnapping" to 12, "Action" to 12,
        "Nudity" to 14, "Delinquents" to 12, "Revenge" to 12, "Crime" to 12, "Guns" to 12, "Battle Royale" to 14, "Terrorism" to 12,
        "Gambling" to 16, "Gangs" to 12, "Military" to 12, "Cult" to 12, "Assassins" to 12, "Cheating" to 14, "Conspiracy" to 12,
        "Espionage" to 12, "Blackmail" to 14, "Brainwashing" to 14, "Death Game" to 14, "Prostitution" to 16, "Incest" to 16,
        "Gore" to 16, "Suicide" to 16, "Drugs" to 16, "Body Horror" to 16, "Human Experimentation" to 16, "Cosmic Horror" to 12,
        "Yakuza" to 16, "Criminal Organization" to 12, "Mafia" to 14, "Triads" to 12, "Eco-Horror" to 12, "Psychosexual" to 16,
        "Ero Guro" to 16, "Zombie" to 12, "Necromancy" to 12, "Vore" to 18, "Torture" to 18, "Cannibalism" to 18
    )

    // What AniList marks as adult but only says fan service, or a fetish without the act: they have an age of their own, not the top
    private val MildAdult = mapOf(
        "Large Breasts" to 14, "Flat Chest" to 14, "Armpits" to 14, "Feet" to 14, "Sweat" to 14, "Virginity" to 14, "MILF" to 14, "DILF" to 14,
        "Tanned Skin" to 14, "Pregnancy" to 12, "Netorare" to 16, "Netorase" to 16, "Netori" to 16, "Lactation" to 16, "Hypersexuality" to 16,
        "Sex Toys" to 16, "Swapping" to 16, "Male Pregnancy" to 16, "Voyeur" to 16, "Exhibitionism" to 16, "Bondage" to 16, "Femdom" to 16,
        "Human Pet" to 16, "Pet Play" to 16, "Masochism" to 16, "Sadism" to 16, "Amputation" to 16, "Asphyxiation" to 16, "Omegaverse" to 16
    )

    // How much of the votes a tag needs for its age to count, by age, more for the tags where a few votes are common
    private val MinRank = mapOf(10 to 60, 12 to 40, 14 to 40, 16 to 30, 18 to 50)
    private val MinRankOf = mapOf(
        "Bullying" to 70, "Tragedy" to 65, "Martial Arts" to 60, "Drugs" to 70, "Gore" to 60, "Body Horror" to 75, "Battle Royale" to 60,
        "Suicide" to 55, "Human Experimentation" to 70, "Cannibalism" to 70, "Torture" to 70, "Incest" to 60, "Yakuza" to 55, "Terrorism" to 60,
        "Nudity" to 65, "Rape" to 52, "Gambling" to 85
    )

    // What is graphic: with two of them together a series is 16, and with two that nearly everyone votes on it is 18
    private val Graphic = setOf("Gore", "Body Horror", "Human Experimentation", "Torture", "Cannibalism", "Suicide")

    // The genres and tags that take a series out of the L, the hard themes counting at 10 with fewer votes
    private val OutOfL = setOf("Fantasy", "Supernatural", "Romance", "Mystery", "Mahou Shoujo", "Sci-Fi", "Adventure", "Mecha", "Psychological")
    private val MildTags = setOf(
        "Youkai", "Ghost", "Demons", "Curses", "Exorcism", "Monster Girl", "Monster Boy", "Super Power", "Magic", "Henshin", "Love Triangle",
        "Delinquents", "Bullying", "Tragedy", "Martial Arts"
    )
    private val MildTagRank = mapOf("Bullying" to 60, "Tragedy" to 40, "Martial Arts" to 40)
    private val Fear = setOf("Youkai", "Ghost", "Demons", "Curses", "Exorcism")

    // The age of the series, as a group. Nothing is known of it when it has no tag and no genre, and then it has none to show
    fun of(tags: List<AgeTag>, genres: List<String>, adult: Boolean): AgeGroup? {
        if (tags.isEmpty() && genres.isEmpty()) return null
        return AgeGroup.of(age(tags, genres, adult))
    }

    // Works out the age of a manga from its tags, its genres and whether AniList marks it as adult
    internal fun age(tags: List<AgeTag>, genres: List<String>, adult: Boolean): Int {
        val shounen = tags.any { it.name == "Shounen" && it.rank >= 50 }
        val kind = if ("Ecchi" in genres && shounen) genres.filter { it != "Ecchi" } + "Ecchi12" else genres

        // What each tag and genre gives: its age, its votes with the spoilers a little lighter, and whether it is an adult one
        class Entry(val age: Int, val name: String, val votes: Double, val adult: Boolean)
        val entries = buildList {
            tags.forEach { tag ->
                val votes = tag.rank * (if (tag.spoiler) SpoilerShare else 1.0)
                when {
                    tag.adult && tag.name in MildAdult -> add(Entry(MildAdult.getValue(tag.name), tag.name, votes, false))
                    tag.adult -> add(Entry(18, tag.name, votes, true))
                    tag.name in Levels -> add(Entry(Levels.getValue(tag.name), tag.name, votes, false))
                }
            }
            kind.forEach { genre -> Levels[genre]?.let { add(Entry(it, genre, 100.0, false)) } }
        }

        var base = 0

        // The public of an action series sets how low it can go
        if ("Action" in kind && tags.any { (it.name == "Shounen" || it.name == "Seinen") && it.rank >= 50 }) base = 12

        // A genre not of everyday life, or a tag of mild fear, magic or teasing, takes it out of L, unless it is for kids or calm
        val kids = tags.any { it.name == "Kids" && it.rank >= 50 }
        val calm = tags.any { it.name == "Iyashikei" && it.rank >= 60 }
        if (!kids && base < Light) {
            if (!calm && kind.any { it in OutOfL }) base = Light
            if (tags.any { it.name in MildTags && it.rank >= (MildTagRank[it.name] ?: MinRank.getValue(Light)) && (!calm || it.name in Fear) }) base = Light
        }

        // The tag that weighs most, when enough people voted on it. A shounen needs more to be taken to 16
        entries.forEach { entry ->
            var need = if (entry.adult) MinRankOf[entry.name] ?: AdultMinRank else MinRankOf[entry.name] ?: MinRank[entry.age] ?: Int.MAX_VALUE
            if (entry.age == 16 && shounen) need = maxOf(need, 75)
            if (entry.age > 0 && entry.votes >= need && entry.age > base) base = entry.age
        }

        // Four tags at 14 that most people vote on take a series to 16
        if (base == 14 && entries.count { it.age == 14 && it.votes >= 60 } >= 4) base = 16

        // Two graphic tags together, each with a good share of the votes, are 16 even when none reaches its own mark alone
        val graphicCut = if (shounen) 65 else 55
        if (base < 16 && tags.count { it.name in Graphic && it.rank >= graphicCut } >= 2) base = 16

        // A shounen with blood, a body torn or people used as experiments that many vote on, and not enough for 16, is 14
        if (shounen && base < 14 && tags.any { it.name in Graphic && it.rank >= ShounenDark }) base = 14

        // Two graphic tags that nearly everybody votes on are what the series is about, which is 18. One alone is not
        val strong = tags.filter { it.name in Graphic && it.name != "Suicide" }.map { it.rank }.sortedDescending()
        if (base < 18 && strong.size >= 2 && strong[1] >= 85) base = 18

        // A horror of staying alive among the dead, or among zombies, is 16 even with no blood in its tags
        if (base < 16 && "Horror" in kind && tags.any { (it.name == "Survival" || it.name == "Zombie") && it.rank >= 70 }) base = 16

        // A psychological thriller shows no violence in its tags, but it is not for a young reader
        if (base < 16 && "Psychological" in kind && "Thriller" in kind) base = 16

        return if (adult) 18 else base
    }
}
