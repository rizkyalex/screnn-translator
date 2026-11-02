package com.rizky.screentranslator

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

// ForegroundService inti:
// bubble melayang -> tap -> capture layar -> OCR (ML Kit) ->
// terjemahkan EN->ID -> tampilkan di overlay.
class ScreenTranslatorService : Service() {

    private lateinit var wm: WindowManager
    private var bubble: BubbleView? = null
    private var overlayHasil: ResultOverlayView? = null
    private var mediaProjection: MediaProjection? = null
    private val handlerUtama = Handler(Looper.getMainLooper())

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        jalankanForeground()
        tampilkanBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ambil hasil izin MediaProjection yang disimpan MainActivity
        val data = MediaProjectionHolder.data
        if (data != null) {
            val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mpm.getMediaProjection(MediaProjectionHolder.resultCode, data)
            MediaProjectionHolder.data = null // pakai sekali saja
        }
        return START_STICKY
    }

    // Notifikasi wajib agar service boleh jalan di background
    private fun jalankanForeground() {
        val channelId = "screen_translator"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Screen Translator", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notif: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Screen Translator aktif")
            .setContentText("Ketuk bubble untuk menerjemahkan layar")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                1, notif,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(1, notif)
        }
    }

    private fun tampilkanBubble() {
        if (bubble != null) return
        val params = BubbleView.buatParams()
        bubble = BubbleView(this).also { b ->
            b.onTap = { tangkapDanTerjemahkan() }
            b.pasangGestur(wm, params)
            wm.addView(b, params)
        }
    }

    // Alur utama saat bubble di-tap
    private fun tangkapDanTerjemahkan() {
        val proyeksi = mediaProjection
        if (proyeksi == null) {
            Toast.makeText(
                this,
                "Izin screen capture belum ada. Aktifkan ulang dari aplikasi.",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        tampilkanOverlayMemuat()
        // Kerja berat di thread background agar UI tidak macet
        Thread {
            try {
                val bitmap = ambilScreenshot(proyeksi)
                if (bitmap == null) {
                    tampilkanError("Gagal mengambil screenshot")
                    return@Thread
                }
                jalankanOcr(bitmap)
            } catch (e: Exception) {
                tampilkanError("Gagal: ${e.message}")
            }
        }.start()
    }

    // Ambil satu frame layar via VirtualDisplay + ImageReader
    private fun ambilScreenshot(proyeksi: MediaProjection): Bitmap? {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(metrics)
        val lebar = metrics.widthPixels
        val tinggi = metrics.heightPixels
        val kepadatan = metrics.densityDpi

        val reader = ImageReader.newInstance(lebar, tinggi, PixelFormat.RGBA_8888, 2)
        val display: VirtualDisplay = proyeksi.createVirtualDisplay(
            "ScreenTranslator",
            lebar, tinggi, kepadatan,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface, null, null
        )
        return try {
            // Tunggu satu frame tersedia (maks 2x percobaan)
            val image = reader.acquireLatestImage()
                ?: run { Thread.sleep(500); reader.acquireLatestImage() }
                ?: return null
            val bitmap = imageKeBitmap(image)
            image.close()
            bitmap
        } finally {
            display.release()
            reader.close()
        }
    }

    // Konversi Image (RGBA) menjadi Bitmap, potong padding baris bila ada
    private fun imageKeBitmap(image: Image): Bitmap {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        return if (rowPadding == 0) {
            bitmap
        } else {
            Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
        }
    }

    // OCR Inggris on-device via ML Kit
    private fun jalankanOcr(bitmap: Bitmap) {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val gambar = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(gambar)
            .addOnSuccessListener { hasil ->
                val teks = hasil.text.trim()
                if (teks.isEmpty()) {
                    tampilkanError("Tidak ada teks terdeteksi di layar")
                } else {
                    terjemahkanTeks(teks)
                }
            }
            .addOnFailureListener { e -> tampilkanError("OCR gagal: ${e.message}") }
    }

    private fun terjemahkanTeks(teksInggris: String) {
        TranslationHelper.terjemahkan(
            teksInggris,
            onSukses = { hasil -> tampilkanHasil(hasil) },
            onGagal = { e ->
                tampilkanError(
                    "Terjemahan gagal: ${e.message}. " +
                        "Pastikan model sudah diunduh lewat tombol \"Download Model\"."
                )
            }
        )
    }

    // --- Overlay hasil: semua update UI lewat main thread ---

    private fun tampilkanOverlayMemuat() {
        handlerUtama.post { dapatkanOverlay().tampilkanMemuat() }
    }

    private fun tampilkanHasil(teks: String) {
        handlerUtama.post { dapatkanOverlay().tampilkanHasil(teks) }
    }

    private fun tampilkanError(pesan: String) {
        handlerUtama.post {
            dapatkanOverlay().tampilkanHasil(pesan)
            Toast.makeText(this, pesan, Toast.LENGTH_SHORT).show()
        }
    }

    // Buat overlay sekali, pakai ulang untuk hasil berikutnya
    private fun dapatkanOverlay(): ResultOverlayView {
        overlayHasil?.let { return it }
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getMetrics(metrics)
        val params = ResultOverlayView.buatParams((metrics.widthPixels * 0.9).toInt())
        val view = ResultOverlayView(this).also {
            it.onTutup = { tutupOverlay() }
            it.pasangDrag(wm, params)
            wm.addView(it, params)
        }
        overlayHasil = view
        return view
    }

    private fun tutupOverlay() {
        overlayHasil?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }
        overlayHasil = null
    }

    override fun onDestroy() {
        bubble?.let {
            try {
                wm.removeView(it)
            } catch (_: Exception) {
            }
        }
        bubble = null
        tutupOverlay()
        mediaProjection?.stop()
        TranslationHelper.tutup()
        super.onDestroy()
    }
}
