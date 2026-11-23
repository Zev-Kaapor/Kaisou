package com.kaisou.app.features.title.data

// The part of the episodes that will have the sources the user adds. It is only the look for now: nothing is loaded and nothing comes with
// the app, so there is no source to choose. Turn Enabled off to hide all of it
object EpisodeSources {
    const val Enabled = false

    // The sources of the extensions the user installed
    val installed: List<EpisodeSource> = emptyList()
}

// A source of the episodes: what it is chosen by and the name it has
class EpisodeSource(val id: String, val name: String)
