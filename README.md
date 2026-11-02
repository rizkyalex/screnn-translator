# Screen Translator

Aplikasi Android sederhana untuk menerjemahkan teks **Inggris → Indonesia** langsung dari layar.
Cara pakai: aktifkan sekali, lalu ketuk **bubble melayang** kapan saja untuk memotret layar,
membaca teksnya (OCR), dan menampilkan terjemahannya di jendela overlay.

- Bahasa: Kotlin, minSdk 26 (Android 8.0+), targetSdk 34
- OCR: ML Kit Text Recognition (on-device)
- Terjemahan: ML Kit Translate EN → ID (on-device)

## Cara buka di Android Studio

1. Buka Android Studio → **Open** → pilih folder `android-screen-translator`.
2. Tunggu Gradle sync selesai (butuh internet untuk unduh dependencies pertama kali).
3. Jalankan di HP via **Run** (atau pakai emulator).

## Cara build APK

Di Android Studio: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**.
Hasilnya ada di `app/build/outputs/apk/debug/app-debug.apk`.
Via terminal (butuh Gradle 8.7+): `./gradlew assembleDebug`.

## Izin yang harus diberikan di HP

1. **Tampil di atas aplikasi lain (overlay):** buka aplikasi → tekan
   "Aktifkan Terjemahan" → izinkan di Settings yang muncul.
2. **Screen capture:** tekan lagi "Aktifkan Terjemahan" → pilih
   **"Mulai sekarang"** pada dialog bawaan Android.
3. Setelah itu bubble melayang muncul. Buka layar berisi teks Inggris,
   ketuk bubble → hasil terjemahan muncul di overlay yang bisa di-drag
   dan di-tutup.

## Catatan model ML Kit

- **Download model butuh internet sekali saja:** tekan tombol
  **"Download Model"** di aplikasi saat pertama kali (file ±30 MB).
- Setelah itu OCR & terjemahan **100% offline**, tidak ada data yang
  dikirim ke server.

## Struktur kode

- `MainActivity.kt` — UI utama, alur perizinan
- `ScreenTranslatorService.kt` — ForegroundService: bubble, capture, OCR, overlay
- `BubbleView.kt` — tombol melayang (drag + tap)
- `ResultOverlayView.kt` — jendela hasil (drag + tombol tutup)
- `TranslationHelper.kt` — wrapper ML Kit Translate EN→ID
- `MediaProjectionHolder.kt` — penampung hasil izin screen capture
