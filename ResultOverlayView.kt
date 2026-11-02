package com.rizky.screentranslator

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

// Jendela hasil terjemahan: bisa di-drag dari bar atas, ada tombol tutup.
class ResultOverlayView(context: Context) : LinearLayout(context) {

    private val tvHasil: TextView
    private val barAtas: LinearLayout

    // Dipanggil saat tombol tutup ditekan
    var onTutup: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        setBackgroundColor(Color.parseColor("#EE111111"))
        setPadding(24, 16, 24, 24)

        // Bar atas: area drag + tombol tutup
        barAtas = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.END
        }
        val btnTutup = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(Color.TRANSPARENT)
            contentDescription = "Tutup"
            setOnClickListener { onTutup?.invoke() }
        }
        barAtas.addView(btnTutup)

        tvHasil = TextView(context).apply {
            setTextColor(Color.WHITE)
            textSize = 15f
        }
        val scroll = ScrollView(context).apply {
            addView(tvHasil)
        }

        addView(barAtas)
        addView(scroll)
    }

    fun tampilkanHasil(teks: String) {
        tvHasil.text = teks
    }

    fun tampilkanMemuat() {
        tvHasil.text = "Menerjemahkan..."
    }

    // Drag hanya dari bar atas agar tombol & teks tetap bisa disentuh normal
    fun pasangDrag(wm: WindowManager, params: WindowManager.LayoutParams) {
        var xAwal = 0
        var yAwal = 0
        var sentuhXAwal = 0f
        var sentuhYAwal = 0f
        barAtas.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    xAwal = params.x
                    yAwal = params.y
                    sentuhXAwal = event.rawX
                    sentuhYAwal = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = xAwal + (event.rawX - sentuhXAwal).toInt()
                    params.y = yAwal + (event.rawY - sentuhYAwal).toInt()
                    wm.updateViewLayout(this, params)
                    true
                }
                else -> false
            }
        }
    }

    companion object {
        fun buatParams(lebar: Int): WindowManager.LayoutParams {
            val tipe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            return WindowManager.LayoutParams(
                lebar,
                WindowManager.LayoutParams.WRAP_CONTENT,
                tipe,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = 120
            }
        }
    }
}
