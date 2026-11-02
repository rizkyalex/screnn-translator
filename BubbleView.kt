package com.rizky.screentranslator

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatImageButton

// Tombol melayang (bubble): bisa di-drag ke mana saja, tap untuk menerjemahkan.
class BubbleView(context: Context) : AppCompatImageButton(context) {

    // Dipanggil saat user men-tap (bukan drag)
    var onTap: (() -> Unit)? = null

    private var xAwal = 0
    private var yAwal = 0
    private var sentuhXAwal = 0f
    private var sentuhYAwal = 0f
    private var sedangDrag = false

    init {
        setImageResource(android.R.drawable.ic_menu_edit)
        setBackgroundResource(android.R.drawable.btn_default)
        contentDescription = "Terjemahkan layar"
        alpha = 0.9f
    }

    // Bedakan tap vs drag: geser >10px dianggap drag
    fun pasangGestur(wm: WindowManager, params: WindowManager.LayoutParams) {
        setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    xAwal = params.x
                    yAwal = params.y
                    sentuhXAwal = event.rawX
                    sentuhYAwal = event.rawY
                    sedangDrag = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - sentuhXAwal).toInt()
                    val dy = (event.rawY - sentuhYAwal).toInt()
                    if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) {
                        sedangDrag = true
                    }
                    params.x = xAwal + dx
                    params.y = yAwal + dy
                    wm.updateViewLayout(v, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!sedangDrag) onTap?.invoke()
                    true
                }
                else -> false
            }
        }
    }

    companion object {
        // Parameter window standar untuk overlay di semua versi Android
        fun buatParams(): WindowManager.LayoutParams {
            val tipe = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            return WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                tipe,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.END
                x = 16
                y = 200
            }
        }
    }
}
