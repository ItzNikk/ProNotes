package com.pronotes.app

import android.app.PictureInPictureParams
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.VideoView
import androidx.activity.ComponentActivity

/** Picture-in-Picture capable local video player for study lectures alongside notes. */
class VideoActivity : ComponentActivity() {
    private var videoView: VideoView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent?.data
        if (uri == null) {
            finish()
            return
        }
        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.BLACK)
        }
        val vv = VideoView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                Gravity.CENTER,
            )
            setMediaController(MediaController(this@VideoActivity))
            setVideoURI(uri)
            setOnPreparedListener { mp ->
                mp.isLooping = false
                start()
                if (packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                    runCatching { enterPictureInPictureMode(PictureInPictureParams.Builder().build()) }
                }
            }
            setOnErrorListener { _, _, _ ->
                android.widget.Toast.makeText(this@VideoActivity, "Use a direct video or stream URL.", android.widget.Toast.LENGTH_LONG).show()
                finish()
                true
            }
        }
        videoView = vv
        root.addView(vv)
        setContentView(root)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            runCatching { enterPictureInPictureMode(params) }
        }
    }

    override fun onDestroy() {
        videoView?.stopPlayback()
        videoView = null
        super.onDestroy()
    }
}
