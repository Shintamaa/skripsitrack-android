# SkripsiTrack (Android)

**Versi terbaru: v1.1**

Aplikasi Android **SkripsiTrack**, Sistem Monitoring Progres Tugas Akhir Berbasis Android. Project UAS mata kuliah Pengembangan Aplikasi Perangkat Bergerak (PAPB).

Aplikasi ini berpasangan dengan backend [`skripsitrack-api`](https://github.com/Shintamaa/skripsitrack-api) (Laravel REST API + MySQL). **Backend harus berjalan agar aplikasi dapat dipakai.**

---

## Mulai Cepat

Ikuti empat langkah ini secara berurutan.

### Prasyarat

- [Laragon](https://laragon.org/) (PHP 8.2 atau lebih baru, MySQL, Composer) dan Git terpasang di laptop
- HP Android (Android 7.0 atau lebih baru)
- HP dan laptop berada di **jaringan Wi-Fi yang sama**, atau HP terhubung ke hotspot laptop

### Langkah 1: Jalankan backend (di laptop)

**a. Pemasangan pertama kali (cukup sekali)**

Buka Laragon, klik **Start All**, lalu buka terminal:

```bash
cd C:\laragon\www
git clone https://github.com/Shintamaa/skripsitrack-api.git
cd skripsitrack-api
composer install
copy .env.example .env
php artisan key:generate
```

Buat database kosong bernama `skripsitrack` (lewat HeidiSQL atau phpMyAdmin). Pastikan isi `.env` sesuai:

```env
DB_CONNECTION=mysql
DB_HOST=127.0.0.1
DB_PORT=3306
DB_DATABASE=skripsitrack
DB_Shintamaa=root
DB_PASSWORD=

DOSEN_INVITE_CODE=DOSEN2026
```

Lalu siapkan tabel, data demo, dan penyimpanan berkas:

```bash
php artisan migrate:fresh --seed
php artisan storage:link
```

Agar unggahan berkas sampai 5 MB bisa berhasil, ubah `php.ini` (lokasinya terlihat dari `php --ini`):

```ini
upload_max_filesize = 10M
post_max_size = 12M
```

> `migrate:fresh` menghapus seluruh data di database. Jalankan hanya saat pertama kali atau saat ingin mereset data demo.

**b. Menjalankan server (setiap kali akan memakai aplikasi)**

Pastikan Laragon aktif (**Start All**), lalu di folder backend:

```bash
cd C:\laragon\www\skripsitrack-api
php artisan serve --host=0.0.0.0 --port=8000
```

Biarkan jendela terminal tetap terbuka selama aplikasi dipakai. Opsi `--host=0.0.0.0` membuat server dapat diakses dari HP.

**c. Cari alamat IP laptop**

```bash
ipconfig
```

Catat **IPv4 Address** pada adapter Wi-Fi, misalnya `192.168.1.110`. Alamat ini bisa berubah setiap berpindah jaringan.

**d. Cek koneksi dari HP**

Buka `http://192.168.1.110:8000/api/me` di browser HP (ganti dengan IP Anda). Jika muncul pesan `Unauthenticated`, server sudah terjangkau. Jika tidak terbuka, izinkan port 8000 di Windows Firewall.

### Langkah 2: Pasang APK dari Releases (di HP)

1. Buka halaman rilis terbaru: **[Releases → latest](https://github.com/Shintamaa/skripsitrack-android/releases/latest)**.
2. Unduh file `SkripsiTrack-v1.1.apk`.
3. Buka file tersebut. Jika diminta, izinkan **instalasi dari sumber tidak dikenal** untuk browser atau pengelola file
