package com.rizky.screentranslator

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions

// Penerjemah Inggris -> Indonesia via ML Kit (on-device, gratis).
object TranslationHelper {

    private var translator: Translator? = null

    // Buat client sekali saja, pakai ulang untuk tiap terjemahan
    private fun dapatkanTranslator(): Translator {
        if (translator == null) {
            val opsi = TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(TranslateLanguage.INDONESIAN)
                .build()
            translator = Translation.getClient(opsi)
        }
        return translator!!
    }

    // Unduh model EN->ID. Butuh internet HANYA saat pertama kali.
    fun downloadModel(onSukses: () -> Unit, onGagal: (Exception) -> Unit) {
        val syarat = DownloadConditions.Builder().build()
        dapatkanTranslator().downloadModelIfNeeded(syarat)
            .addOnSuccessListener { onSukses() }
            .addOnFailureListener { e -> onGagal(e) }
    }

    // Terjemahkan teks Inggris ke Indonesia (callback di main thread)
    fun terjemahkan(teks: String, onSukses: (String) -> Unit, onGagal: (Exception) -> Unit) {
        if (teks.isBlank()) {
            onSukses("")
            return
        }
        dapatkanTranslator().translate(teks)
            .addOnSuccessListener { hasil -> onSukses(hasil) }
            .addOnFailureListener { e -> onGagal(e) }
    }

    // Panggil saat service dimatikan agar resource dibersihkan
    fun tutup() {
        translator?.close()
        translator = null
    }
}
