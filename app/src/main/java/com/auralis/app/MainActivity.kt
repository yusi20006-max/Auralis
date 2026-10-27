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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutionException

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var selectedTitle by mutableStateOf("هنوز فایلی انتخاب نشده")
    private var isPlaying by mutableStateOf(false)
    private var status by mutableStateOf("یک فایل صوتی از حافظهٔ گوشی انتخاب کنید.")

    private val openAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (_: SecurityException) { }
            selectedTitle = uri.lastPathSegment?.substringAfterLast('/')?.substringAfter(':')?.takeIf { it.isNotBlank() } ?: "فایل صوتی انتخاب‌شده"
            controller?.let {
                it.setMediaItem(MediaItem.fromUri(uri))
                it.prepare()
                it.play()
                status = "در حال پخش"
            } ?: run { status = "پخش‌کننده هنوز آماده نیست؛ فایل را دوباره انتخاب کنید." }
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
                        status = if (playing) "در حال پخش" else "پخش متوقف است"
                    }
                })
            } catch (_: ExecutionException) { status = "اتصال به پخش‌کننده ناموفق بود." }
            catch (_: InterruptedException) { Thread.currentThread().interrupt(); status = "آماده‌سازی پخش‌کننده متوقف شد." }
        }, mainExecutor)

        setContent {
            val background = Brush.verticalGradient(listOf(Color(0xFF070A12), Color(0xFF11172A), Color(0xFF080B13)))
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF080B13)) {
                    Column(Modifier.fillMaxSize().background(background).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text("AURALIS", color = Color(0xFF73E7FF), fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 5.sp)
                        Spacer(Modifier.height(6.dp))
                        Text("OFFLINE AUDIO", color = Color(0xFF9A8BFF), fontSize = 11.sp, letterSpacing = 3.sp)
                        Spacer(Modifier.height(40.dp))
                        Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color(0xFF151B2C)) {
                            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("پخش‌کنندهٔ آفلاین", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(16.dp))
                                Text(selectedTitle, color = Color(0xFFD9DEEF), textAlign = TextAlign.Center, fontSize = 15.sp)
                                Spacer(Modifier.height(10.dp))
                                Text(status, color = Color(0xFF9CA8C7), textAlign = TextAlign.Center, fontSize = 13.sp)
                                Spacer(Modifier.height(24.dp))
                                Button(onClick = { openAudio.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3E8DFF))) { Text("انتخاب فایل صوتی") }
                                Spacer(Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Button(onClick = { controller?.let { if (it.isPlaying) it.pause() else it.play() } }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF725BFF))) { Text(if (isPlaying) "مکث" else "ادامهٔ پخش") }
                                    Button(onClick = { controller?.stop(); isPlaying = false; status = "پخش متوقف شد" }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF30384F))) { Text("توقف") }
                                }
                            }
                        }
                        Spacer(Modifier.height(22.dp))
                        Text("نسخهٔ آزمایشی · بدون سرور و حساب کاربری", color = Color(0xFF747F9E), fontSize = 12.sp, textAlign = TextAlign.Center)
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
