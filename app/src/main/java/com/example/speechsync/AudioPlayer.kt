package com.example.speechsync

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

class AudioPlayer(context: Context) {
    private val player = ExoPlayer.Builder(context).build()

    fun play(url: String, onComplete: () -> Unit = {}) {
        val mediaItem = MediaItem.fromUri(url)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true

        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    onComplete()
                }
            }
        })
    }

    fun pause() {
        player.pause()
    }

    fun resume() {
        player.play()
    }

    fun stop() {
        player.stop()
    }

    fun release() {
        player.release()
    }

    fun isPlaying() = player.isPlaying

    fun getDuration() = if (player.duration > 0) player.duration else 0L
}