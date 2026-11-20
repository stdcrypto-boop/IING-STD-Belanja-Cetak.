# IING STD — Belanja & Cetak Struk

Aplikasi Android native Java, offline. Fitur: tambah/edit/hapus barang, jumlah/satuan, harga opsional, checklist, simpan otomatis dengan SharedPreferences JSON, pilih perangkat Bluetooth yang sudah dipasangkan, dan kirim struk ESC/POS 58 mm melalui Bluetooth Classic SPP.

## Build
GitHub Actions -> Build Belanja Cetak APK -> Run workflow -> download artifact APK. Atau jalankan `gradle assembleDebug` dengan Android SDK (compileSdk 35) dan Gradle 8.9.

## Cetak
1. Pair printer di Pengaturan Bluetooth Android.
2. Buka aplikasi, izinkan Nearby devices (Android 12+).
3. Tambah barang, pilih printer, tekan CETAK.

Catatan: kompatibilitas printer Digit UP tertentu belum diuji fisik; protokol ESC/POS Bluetooth SPP merupakan asumsi yang harus diuji. Harga pada daftar hanya opsional; jumlah biaya tidak dicetak sebagai total karena tujuan utama adalah daftar titipan belanja. Data tersimpan lokal di aplikasi. Menghapus data aplikasi akan menghapus daftar.
