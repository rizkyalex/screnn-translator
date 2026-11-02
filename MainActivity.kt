package com.rizky.screentranslator

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

// Layar utama: minta izin, jalankan service, unduh model terjemahan.
class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnAktifkan: Button
    private lateinit var btnDownloadModel: Button

    // Launcher untuk dialog izin screen capture (MediaProjection)
    private val mediaProjectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            // Simpan hasil izin agar bisa dipakai oleh Service
            MediaProjectionHolder.resultCode = result.resultCode
            MediaProjectionHolder.data = result.data
            jalankanService()
        } else {
            tvStatus.text = getString(R.string.status_izin_capture_ditolak)
            Toast.makeText(this, R.string.izin_capture_dibutuhkan, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnAktifkan = findViewById(R.id.btnAktifkan)
        btnDownloadModel = findViewById(R.id.btnDownloadModel)

        btnAktifkan.setOnClickListener { aktifkanTerjemahan() }
        btnDownloadModel.setOnClickListener { downloadModel() }

        perbaruiStatus()
    }

    override fun onResume() {
        super.onResume()
        // Perbarui status karena user mungkin baru kembali dari Settings
        perbaruiStatus()
    }

    // Alur: cek izin overlay -> minta izin capture layar -> jalankan service
    private fun aktifkanTerjemahan() {
        if (!Settings.canDrawOverlays(this)) {
            // Arahkan ke Settings untuk izin "tampil di atas aplikasi lain"
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Toast.makeText(this, R.string.aktifkan_izin_overlay, Toast.LENGTH_LONG).show()
            return
        }
        // Minta izin screen capture bawaan Android
        val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher.launch(mpm.createScreenCaptureIntent())
    }

    private fun jalankanService() {
        val intent = Intent(this, ScreenTranslatorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        tvStatus.text = getString(R.string.status_aktif)
    }

    // Unduh model ML Kit EN->ID (butuh internet sekali saja)
    private fun downloadModel() {
        btnDownloadModel.isEnabled = false
        tvStatus.text = getString(R.string.status_download_model)
        TranslationHelper.downloadModel(
            onSukses = {
                tvStatus.text = getString(R.string.status_model_siap)
                btnDownloadModel.isEnabled = true
            },
            onGagal = { e ->
                tvStatus.text = getString(R.string.status_model_gagal, e.message)
                btnDownloadModel.isEnabled = true
            }
        )
    }

    private fun perbaruiStatus() {
        tvStatus.text = if (Settings.canDrawOverlays(this)) {
            getString(R.string.status_overlay_ok)
        } else {
            getString(R.string.status_butuh_izin)
        }
    }
}
