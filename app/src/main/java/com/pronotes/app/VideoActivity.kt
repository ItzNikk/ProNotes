package com.pronotes.app

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import android.widget.Toast
import android.widget.VideoView

class VideoActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent.data?.takeIf { it.scheme == "https" && !it.host.isNullOrBlank() }
        if (uri == null) { finish(); return }
        val video = VideoView(this)
        setContentView(video)
        video.setMediaController(MediaController(this).also { it.setAnchorView(video) })
        video.setOnErrorListener { _, _, _ ->
            Toast.makeText(this, "Use a direct video file or stream URL.", Toast.LENGTH_LONG).show()
            finish()
            true
        }
        video.setOnPreparedListener {
            video.start()
            if (packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                runCatching { enterPictureInPictureMode(PictureInPictureParams.Builder().build()) }
            }
        }
        video.setVideoURI(uri)
    }
}
