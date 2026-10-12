package com.cinaywave.app

import android.app.Application
import com.cinaywave.app.data.Repository
import com.cinaywave.app.playback.PlaybackManager

class WaveApplication : Application() {
    lateinit var repository: Repository; private set
    lateinit var playback: PlaybackManager; private set
    override fun onCreate() { super.onCreate(); repository = Repository(this); playback = PlaybackManager(this, repository) }
}
