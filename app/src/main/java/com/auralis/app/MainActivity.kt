package com.auralis.app

import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException
import com.auralis.app.ui.theme.AuralisColors
import com.auralis.app.ui.theme.AuralisSpacing
import com.auralis.app.ui.theme.AuralisTheme
import com.auralis.app.ui.theme.AuralisTypeScale

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var pendingUri: Uri? = null
    private var selectedTitle by mutableStateOf("هنوز فایلی انتخاب نشده")
    private var isPlaying by mutableStateOf(false)
    private var status by mutableStateOf("یک فایل صوتی از حافظهٔ گوشی انتخاب کنید.")

    private val openAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: SecurityException) {
                // The temporary read grant from OpenDocument remains usable for this activity session.
            } catch (_: IllegalArgumentException) {
                // Some document providers do not offer persistable grants.
            }
            pendingUri = uri
            selectedTitle = uri.lastPathSegment
                ?.substringAfterLast('/')
                ?.substringAfter(':')
                ?.takeIf { it.isNotBlank() }
                ?: "فایل صوتی انتخاب‌شده"
            startSelectedFileIfReady()
        }
    }

    private fun startSelectedFileIfReady() {
        val player = controller
        val uri = pendingUri
        if (player == null || uri == null) {
            status = "فایل انتخاب شد؛ در انتظار آماده‌شدن پخش‌کننده…"
            return
        }
        try {
            status = "در حال آماده‌سازی فایل…"
            isPlaying = false
            player.pause()
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.playWhenReady = true
            player.play()
        } catch (error: RuntimeException) {
            isPlaying = false
            status = "شروع پخش ناموفق بود: ${error.message ?: error.javaClass.simpleName}"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                controller = future.get()
                controller?.addListener(object : Player.Listener {
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                        if (playing) {
                            status = "در حال پخش"
                        } else if (controller?.playWhenReady == true &&
                            controller?.playbackState == Player.STATE_BUFFERING
                        ) {
                            status = "در حال بارگذاری صدا…"
                        } else if (controller?.playbackState == Player.STATE_ENDED) {
                            status = "پخش فایل به پایان رسید."
                        } else if (controller?.playWhenReady == false && status == "در حال پخش") {
                            status = "پخش متوقف است"
                        }
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_BUFFERING -> if (controller?.playWhenReady == true) {
                                status = "در حال بارگذاری صدا…"
                            }
                            Player.STATE_READY -> if (controller?.playWhenReady == true && !isPlaying) {
                                status = "فایل آماده است؛ منتظر شروع صدا…"
                            }
                            Player.STATE_ENDED -> {
                                isPlaying = false
                                status = "پخش فایل به پایان رسید."
                            }
                        }
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        isPlaying = false
                        status = "خطای پخش (${error.errorCodeName}): ${error.message ?: "قالب فایل پشتیبانی نمی‌شود"}"
                    }
                })
                startSelectedFileIfReady()
            } catch (error: ExecutionException) {
                status = "اتصال به پخش‌کننده ناموفق بود: ${error.cause?.message ?: error.message}"
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                status = "آماده‌سازی پخش‌کننده متوقف شد."
            }
        }, mainExecutor)

        setContent {
            val background = Brush.verticalGradient(listOf(AuralisColors.BackgroundDeep, AuralisColors.Surface, AuralisColors.BackgroundDeep))
            AuralisTheme {
                Surface(Modifier.fillMaxSize(), color = AuralisColors.BackgroundDeep) {
                    Column(Modifier.fillMaxSize().background(background).padding(AuralisSpacing.X6), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("AURALIS", color = AuralisColors.AccentCyan, fontSize = AuralisTypeScale.HeroMax, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("OFFLINE AUDIO", color = AuralisColors.PrimaryViolet, fontSize = AuralisTypeScale.Overline, letterSpacing = 3.sp)
                        Spacer(Modifier.height(AuralisSpacing.X8 + AuralisSpacing.X2))
                        Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge, color = AuralisColors.Surface) {
                            Column(Modifier.padding(AuralisSpacing.X6), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("پخش‌کنندهٔ آفلاین", color = AuralisColors.TextPrimary, fontSize = AuralisTypeScale.TitleLargeMin, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(16.dp))
                                Text(selectedTitle, color = AuralisColors.TextPrimary, textAlign = TextAlign.Center, fontSize = AuralisTypeScale.BodyMedium)
                                Spacer(Modifier.height(10.dp))
                                Text(status, color = AuralisColors.TextSecondary, textAlign = TextAlign.Center, fontSize = AuralisTypeScale.BodyMedium)
                                Spacer(Modifier.height(24.dp))
                                Button(onClick = { openAudio.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = AuralisColors.PrimaryBlue, contentColor = AuralisColors.BackgroundDeep)) { Text("انتخاب فایل صوتی") }
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Button(onClick = {
                                        controller?.let {
                                            if (it.isPlaying || it.playWhenReady) {
                                                it.pause()
                                                status = "پخش مکث شد"
                                            } else if (it.currentMediaItem != null) {
                                                it.play()
                                                status = "درخواست ادامهٔ پخش ارسال شد…"
                                            } else {
                                                startSelectedFileIfReady()
                                            }
                                        } ?: run { status = "پخش‌کننده هنوز متصل نشده است." }
                                    }, colors = ButtonDefaults.buttonColors(containerColor = AuralisColors.PrimaryViolet, contentColor = AuralisColors.BackgroundDeep)) { Text(if (isPlaying) "مکث" else "ادامهٔ پخش") }
                                    Button(onClick = {
                                        controller?.stop()
                                        isPlaying = false
                                        status = "پخش متوقف شد"
                                    }, colors = ButtonDefaults.buttonColors(containerColor = AuralisColors.SurfaceElevated)) { Text("توقف") }
                                }
                            }
                        }
                        Spacer(Modifier.height(22.dp))
                        Text("نسخهٔ آزمایشی · بدون سرور و حساب کاربری", color = AuralisColors.TextSecondary, fontSize = AuralisTypeScale.Caption, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        controller?.release()
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        super.onDestroy()
    }
}
