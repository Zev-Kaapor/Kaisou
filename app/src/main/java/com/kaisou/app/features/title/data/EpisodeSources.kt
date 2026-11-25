package com.kaisou.app.features.title.data

// The part of the episodes for the sources the user adds, only the look for now, with nothing loaded
object EpisodeSources {
    // Turn it off to hide all of the sources
    const val Enabled = false

    // The sources of the extensions the user installed
    val installed: List<EpisodeSource> = emptyList()
}

// A source of the episodes: what it is chosen by and the name it has
class EpisodeSource(val id: String, val name: String)
