# SkripsiTrack (Android)

Aplikasi Android **SkripsiTrack**, Sistem Monitoring Progres Tugas Akhir Berbasis Android. Project UAS mata kuliah Pengembangan Aplikasi Perangkat Bergerak (PAPB).

Aplikasi ini berpasangan dengan backend [`skripsitrack-api`](https://github.com/USERNAME/skripsitrack-api) (Laravel REST API + MySQL). Backend harus dijalankan lebih dulu.

## Tentang aplikasi

SkripsiTrack membantu mahasiswa dan dosen pembimbing memantau progres tugas akhir. Fitur unggulannya adalah **timeline tahapan penelitian** dan **riwayat revisi berversi**. Setiap unggahan menjadi versi baru (v1, v2, v3, dst.). Dosen meninjau versi terbaru dengan menyetujui atau meminta revisi, dan status tahapan di timeline berubah otomatis mengikuti hasil peninjauan.

## Fitur

**Umum**
- Dua splash screen animasi sebelum masuk aplikasi
- Registrasi, login, dan logout dengan dua role: **mahasiswa** dan **dosen** (pendaftaran dosen memakai kode undangan)
- Dashboard: persentase progres, jumlah menunggu review dan perlu revisi, deadline terdekat, aktivitas terbaru
- Riwayat aktivitas dengan filter jenis aksi dan paginasi (muat otomatis saat menggulir)
- Pengaturan alamat server langsung dari layar Login
- Penanganan loading, data kosong, kegagalan jaringan (dengan tombol *Coba lagi*), dan sesi berakhir

**Mahasiswa**
- Tambah, ubah, dan hapus skripsi, dengan 7 tahapan penelitian dibuat otomatis
- Pencarian judul dan filter status
- Timeline tahapan dengan status berwarna dan peringatan keterlambatan
- Unggah berkas (JPG, PNG, PDF, DOC, DOCX; maksimal 5 MB) dengan pratinjau gambar
- Riwayat versi per tahapan, komentar, hapus satu unggahan atau hapus massal dengan konfirmasi

**Dosen**
- Daftar skripsi bimbingan, pencarian berdasarkan judul atau nama mahasiswa
- Meninjau versi terbaru: **Setujui** atau **Minta revisi** (disertai catatan)
- Memberi komentar pada setiap versi

## Teknologi

| Komponen | Teknologi |
|---|---|
| Bahasa | Kotlin |
| Antarmuka | XML dengan ViewBinding, Material Components |
| Jaringan | Retrofit 2.11 dan OkHttp 4.12 (JSON dengan Gson) |
| Asinkron | Kotlin Coroutines dan lifecycleScope |
| Gambar | Glide |
| Penyimpanan sesi | SharedPreferences |
| Minimum SDK | API 24 (Android 7.0) |

## Arsitektur

```
Android (Kotlin + XML + Retrofit)
        |  HTTP request / JSON response
        v
REST API (Laravel di Laragon, autentikasi Sanctum)
        |  Eloquent ORM
        v
MySQL
```

## Prasyarat

- Android Studio versi terbaru
- HP Android (USB debugging aktif) atau emulator
- Backend `skripsitrack-api` sudah berjalan (lihat README repo tersebut)
- HP dan laptop berada di **jaringan Wi-Fi yang sama**, atau HP terhubung ke hotspot laptop

## Menjalankan dari source code

1. Jalankan backend dengan `--host=0.0.0.0`:
```bash
   php artisan serve --host=0.0.0.0 --port=8000
```
2. Clone repo ini dan buka di Android Studio:
```bash
   git clone https://github.com/USERNAME/skripsitrack-android.git
```
3. Tunggu Gradle sync selesai, lalu klik **Run** pada HP atau emulator.
4. Atur alamat server (lihat bagian berikut), lalu login.

## Mengatur alamat server

Alamat server tidak ditanam permanen di aplikasi. Atur lewat layar Login:

1. Ketuk **Atur alamat server**.
2. Isi alamat laptop yang menjalankan backend, tanpa `http://` dan tanpa `/api` pun boleh:

   | Perangkat | Contoh isian |
   |---|---|
   | HP fisik (satu Wi-Fi) | `192.168.1.110:8000` |
   | Emulator Android Studio | `10.0.2.2:8000` |

3. Ketuk **Simpan**. Tombol **Reset** mengembalikan ke alamat bawaan.

Cari IP laptop dengan perintah `ipconfig` (IPv4 pada adapter Wi-Fi). Alamat ini bisa berubah setiap kali berpindah jaringan, jadi periksa ulang bila aplikasi tiba-tiba gagal terhubung. Alamat yang disimpan tidak terhapus saat logout.

Untuk mengubah alamat bawaan di kode, edit `DEFAULT_BASE_URL` di `data/remote/RetrofitClient.kt`.

## Memasang APK

1. Unduh `SkripsiTrack-v1.0.apk` dari halaman **Releases** repo ini.
2. Izinkan instalasi dari sumber tidak dikenal, lalu instal.
3. Buka aplikasi, atur alamat server seperti di atas, lalu login.

## Membuat APK sendiri

**APK debug:** menu **Build → Build Bundle(s) / APK(s) → Build APK(s)**. Hasilnya di `app/build/outputs/apk/debug/app-debug.apk`.

**APK rilis bertanda tangan:** menu **Build → Generate Signed App Bundle / APK → APK**, buat keystore `.jks` dan simpan di luar folder project. Jangan meng-commit keystore. Biarkan `isMinifyEnabled = false` pada build `release`, karena minifikasi dapat merusak pembacaan model oleh Gson dan Retrofit.

## Akun demo

Semua akun memakai password `password`, tersedia setelah backend dijalankan dengan `php artisan migrate:fresh --seed`.

| Role | Email | Keterangan |
|---|---|---|
| Dosen | `dosen@test.com` | Membimbing 2 mahasiswa |
| Dosen | `dosen2@test.com` | Belum membimbing (uji data kosong) |
| Mahasiswa | `mahasiswa@test.com` | BAB I dalam proses revisi |
| Mahasiswa | `mahasiswa2@test.com` | 3 tahapan selesai, 1 tahapan terlambat |
| Mahasiswa | `mahasiswa3@test.com` | Belum punya skripsi (uji data kosong) |

## Mendaftar akun baru

Di layar Login, ketuk **Belum punya akun? Daftar**.

- **Mahasiswa:** isi nama, NIM, email, dan password, lalu pilih *Mahasiswa*.
- **Dosen:** pilih *Dosen*, lalu isi **kode undangan dosen**. Kodenya sama dengan nilai `DOSEN_INVITE_CODE` di file `.env` backend (bawaan demo: `DOSEN2026`).

Setelah berhasil mendaftar, pengguna langsung masuk ke aplikasi.

## Alur penggunaan singkat

**Mahasiswa**
1. Tab **Skripsi**, ketuk tombol **+**, isi judul, dosen pembimbing, dan tanggal.
2. Ketuk kartu skripsi untuk membuka **timeline tahapan**.
3. Ketuk satu tahapan, pilih berkas, tulis catatan bila perlu, lalu **Unggah**.
4. Bila dosen meminta revisi, perbaiki berkas dan unggah versi baru. Komentar dosen tampil pada kartu versi.
5. Untuk menghapus beberapa unggahan sekaligus, ketuk **Pilih**, tandai unggahan, lalu **Hapus**. Unggahan yang sudah disetujui tidak dapat dipilih.

**Dosen**
1. Tab **Bimbingan**, pilih skripsi mahasiswa, lalu pilih tahapan.
2. Buka berkas pada versi terbaru, lalu pilih **Setujui** atau **Minta revisi** (catatan revisi wajib diisi).
3. Pantau perkembangan seluruh bimbingan lewat **Dashboard** dan **Riwayat**.

## Struktur project

```
app/src/main/java/com/skripsitrack/app/
├── SkripsiApp.kt                 Application class (menyimpan SessionManager)
├── data/
│   ├── model/Models.kt           Data class respons dan request API
│   ├── remote/                   ApiService dan RetrofitClient
│   └── local/SessionManager.kt   Token, data pengguna, alamat server
├── util/                         Resource/safeApiCall, DateUtil, FileUtil, UiUtil
└── ui/
    ├── splash/                   Dua splash screen animasi
    ├── auth/                     LoginActivity dan RegisterActivity
    ├── main/                     MainActivity, Dashboard, Riwayat, Profil
    └── thesis/                   Daftar skripsi, form, detail/timeline, tahapan dan unggahan
```

## Pemecahan masalah

| Gejala | Penyebab dan solusi |
|---|---|
| `CLEARTEXT communication not permitted` | Server memakai HTTP lokal. Pastikan `android:usesCleartextTraffic="true"` ada pada tag `<application>` di `AndroidManifest.xml` |
| "Gagal terhubung ke server" atau timeout | Beda jaringan Wi-Fi, firewall memblokir port 8000, server tidak dijalankan dengan `--host=0.0.0.0`, atau IP laptop berubah. Cek `ipconfig`, lalu perbarui lewat **Atur alamat server** |
| Toast "Sesi berakhir, silakan login kembali" | Token tidak valid lagi (misalnya database di-reset). Login ulang |
| Unggah gagal atau ditolak | Berkas lebih dari 5 MB atau formatnya tidak didukung. Bila server menolak berkas kecil, cek batas `upload_max_filesize` di `php.ini` backend |
| Gambar pratinjau tidak muncul | Backend belum menjalankan `php artisan storage:link`, atau tautan berkas tidak dapat dibuka dari browser HP |
| `Unresolved reference` pada kelas `...Binding` | Pastikan `viewBinding = true` di `build.gradle.kts`, lalu **Build → Rebuild Project** |
| Toolbar atau bilah bawah tertutup bilah sistem (Android 15+) | Tambahkan `android:fitsSystemWindows="true"` pada layout root yang bersangkutan |
| Aplikasi crash pada APK rilis, tetapi normal saat debug | Pastikan `isMinifyEnabled = false` pada build `release` |

## Dokumentasi terkait

- Backend dan daftar 21 endpoint: repo [`skripsitrack-api`](https://github.com/USERNAME/skripsitrack-api)

## Keperluan

Proyek ini dibuat untuk keperluan akademik (UAS PAPB).
