package com.rizky.screentranslator

import android.content.Intent

// Penampung sekali-pakai untuk hasil izin MediaProjection.
// MainActivity mengisinya, ScreenTranslatorService membacanya sesaat setelah start.
object MediaProjectionHolder {
    var resultCode: Int = 0
    var data: Intent? = null
}
