<div align="center">

# 🏠 KosHub SuperApp

### Aplikasi Android untuk pencarian, booking, dan manajemen kos mahasiswa

KosHub adalah aplikasi mobile berbasis Android yang dirancang untuk membantu **mahasiswa/penyewa** menemukan kos dengan lebih mudah, sekaligus membantu **owner/pemilik kos** mengelola properti, booking, chat, komplain, dan laporan keuangan dalam satu aplikasi.

<br/>

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Language](https://img.shields.io/badge/Language-Java-007396?style=for-the-badge&logo=openjdk&logoColor=white)
![UI](https://img.shields.io/badge/UI-XML%20Layout-0FA958?style=for-the-badge)
![Mapbox](https://img.shields.io/badge/Maps-Mapbox%20SDK%20v11-000000?style=for-the-badge&logo=mapbox&logoColor=white)
![Status](https://img.shields.io/badge/Status-Project%20UAS-success?style=for-the-badge)

</div>

---

## 📌 Daftar Isi

- [Tentang Project](#-tentang-project)
- [Tujuan Aplikasi](#-tujuan-aplikasi)
- [Role Pengguna](#-role-pengguna)
- [Fitur Utama](#-fitur-utama)
- [Preview Alur Aplikasi](#-preview-alur-aplikasi)
- [Tech Stack](#-tech-stack)
- [Struktur Project](#-struktur-project)
- [Requirements](#-requirements)
- [Cara Menjalankan Project](#-cara-menjalankan-project)
- [Setup Mapbox Token](#-setup-mapbox-token)
- [Force Close Prevention](#-force-close-prevention)
- [Activity Utama](#-activity-utama)
- [Catatan Keamanan GitHub](#-catatan-keamanan-github)
- [Roadmap Pengembangan](#-roadmap-pengembangan)
- [Kontributor](#-kontributor)

---

## ✨ Tentang Project

**KosHub SuperApp** adalah aplikasi Android yang menggabungkan konsep:

- pencarian kos,
- marketplace properti,
- sistem booking,
- dashboard owner,
- chat transaksi,
- laporan komplain,
- dan manajemen keuangan owner.

Aplikasi ini dibuat untuk kebutuhan **Project UAS Struktur Data dan Algoritma**, dengan fokus pada implementasi UI/UX aplikasi mobile yang modern, rapi, dan fungsional.

KosHub tidak hanya menampilkan daftar kos, tetapi juga mensimulasikan alur bisnis kos mulai dari pencarian, booking, komunikasi dengan owner, pengambilan kunci, sampai laporan komplain dan pengelolaan saldo owner.

---

## 🎯 Tujuan Aplikasi

KosHub dibuat untuk menyelesaikan beberapa kebutuhan utama:

### Untuk Mahasiswa / Penyewa

- Memudahkan pencarian kos berdasarkan lokasi, harga, fasilitas, dan kategori.
- Memberikan informasi kos yang lebih jelas dan terstruktur.
- Mendukung proses booking kos secara lebih mudah.
- Menyediakan fitur riwayat booking dan status ngekos.
- Memberikan akses untuk mengirim komplain kepada pemilik kos.

### Untuk Owner / Pemilik Kos

- Membantu owner memantau performa kos.
- Mengelola booking masuk dari calon penyewa.
- Mengelola daftar kos, kamar, penyewa, dan status kamar.
- Melihat laporan keuangan, saldo tersedia, saldo pending, dan penarikan saldo.
- Menerima laporan komplain dari penyewa.
- Berkomunikasi dengan penyewa melalui chat.

---

## 👥 Role Pengguna

Aplikasi KosHub memiliki dua role utama:

| Role | Deskripsi |
|---|---|
| **Mahasiswa / Penyewa** | Pengguna yang mencari kos, melakukan booking, melihat riwayat, mengambil kunci, dan membuat komplain. |
| **Owner / Pemilik Kos** | Pengguna yang mengelola kos, booking, chat, laporan komplain, dan laporan keuangan. |

---

## 🚀 Fitur Utama

### 🧑‍🎓 Fitur Mahasiswa / Penyewa

- Login dan register.
- Home pencarian kos.
- Daftar kos dengan card informatif.
- Search kos.
- Filter dan sorting kos.
- Detail properti/kos.
- Navigasi peta menggunakan Mapbox.
- Booking kos.
- Waiting list.
- Riwayat profil dan booking.
- Konfirmasi **Sudah Ambil Kunci Kos**.
- Laporan komplain ke owner.
- Favorite kos.

---

### 🏘️ Fitur Owner / Pemilik Kos

#### Dashboard Owner

- Ringkasan total kos.
- Ringkasan kamar terisi dan kamar kosong.
- Booking masuk.
- Pendapatan bulan ini.
- Saldo tersedia.
- Saldo pending.
- Komplain baru.
- Booking siap check-in.
- Tingkat hunian.
- Aksi cepat owner.

#### Booking Owner

- Daftar booking masuk.
- Status booking.
- Summary booking.
- Search dan filter booking.
- Terima booking.
- Tolak booking.
- Lihat detail booking.
- Akses chat penyewa.

#### Chat Owner

- Inbox chat penyewa.
- Filter chat.
- Badge pesan belum dibaca.
- Status chat terkait booking/komplain.
- Chat room dengan context card.
- Bubble chat kanan/kiri.
- Quick action.
- Template pesan.

#### Finance Report

- Total pendapatan.
- Saldo tersedia.
- Saldo pending.
- Saldo diproses.
- Riwayat penarikan saldo.
- Form tarik saldo.
- Insight laporan keuangan.

#### Complaint Management

- Laporan komplain masuk.
- Status komplain: baru, diproses, selesai, ditolak.
- Detail komplain penyewa.
- Aksi untuk menandai komplain diproses/selesai.

#### Management Page

- Manajemen kos.
- Manajemen kamar.
- Penyewa aktif.
- Booking request.
- Maintenance/operasional.
- Fasilitas dan harga.

#### Owner Profile

- Profil owner.
- Status verifikasi.
- Ringkasan bisnis.
- Informasi rekening.
- Pengaturan akun.
- Bantuan owner.
- Logout.

---

## 🔄 Preview Alur Aplikasi

### Alur Mahasiswa

```text
Splash Screen
    ↓
Login / Register
    ↓
Student Home
    ↓
Pilih Kos
    ↓
Detail Kos
    ↓
Booking / Navigasi Map
    ↓
Riwayat Booking
    ↓
Sudah Ambil Kunci / Komplain
```

### Alur Owner

```text
Splash Screen
    ↓
Login
    ↓
Owner Dashboard
    ↓
Kelola Booking / Kos / Chat / Finance / Profile
    ↓
Terima Booking
    ↓
Pantau Check-in
    ↓
Kelola Komplain & Saldo
```

### Alur Booking dan Saldo

```text
Penyewa booking kos
    ↓
Owner menerima booking
    ↓
Penyewa melakukan pembayaran
    ↓
Saldo owner masuk pending
    ↓
Penyewa mengambil kunci
    ↓
Penyewa klik “Sudah Ambil Kunci”
    ↓
Status menjadi Aktif Ngekos
    ↓
Saldo pending berubah menjadi saldo tersedia
    ↓
Owner dapat tarik saldo
```

---

## 🛠️ Tech Stack

| Bagian | Teknologi |
|---|---|
| Bahasa utama | Java |
| UI | XML Layout |
| IDE | Android Studio |
| Build system | Gradle Kotlin DSL |
| Minimum SDK | 24 |
| Target SDK | 36 |
| Compile SDK | 36 |
| Architecture | Activity-based Android app |
| Map | Mapbox Maps SDK v11 |
| UI Components | AndroidX, AppCompat, Material Components, ConstraintLayout, RecyclerView |

---

## 📁 Struktur Project

Struktur utama repository:

```text
KosHub_App/
├── app/
│   ├── developer-config.xml.example
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/
│           │   └── com/
│           │       └── koshub/
│           │           └── psdku/
│           │               ├── SplashActivity.java
│           │               ├── LoginActivity.java
│           │               ├── RegisterActivity.java
│           │               ├── StudentHomeActivity.java
│           │               ├── PropertyDetailBookingActivity.java
│           │               ├── MapViewRouteNavigationActivity.java
│           │               ├── WaitingListQueueActivity.java
│           │               ├── ProfileHistoryActivity.java
│           │               ├── OwnerDashboardActivity.java
│           │               ├── OwnerBookingActivity.java
│           │               ├── OwnerChatActivity.java
│           │               ├── OwnerChatRoomActivity.java
│           │               ├── OwnerComplaintActivity.java
│           │               ├── OwnerFinanceReportActivity.java
│           │               ├── OwnerManagementActivity.java
│           │               ├── OwnerProfileSettingsActivity.java
│           │               ├── OwnerWithdrawActivity.java
│           │               ├── TenantComplaintFormActivity.java
│           │               ├── KosAdapter.java
│           │               ├── KosItem.java
│           │               ├── MapboxTokenHelper.java
│           │               ├── NavigationHelper.java
│           │               ├── NavigationTransitionHelper.java
│           │               └── OwnerBottomNavHelper.java
│           └── res/
│               ├── anim/
│               ├── drawable/
│               ├── layout/
│               ├── values/
│               └── xml/
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── .gitignore
└── README.md
```

---

## ✅ Requirements

Sebelum menjalankan project, pastikan sudah memiliki:

- Android Studio versi terbaru.
- JDK 11 atau yang kompatibel dengan project.
- Gradle wrapper dari repository.
- Android SDK dengan compile SDK 36.
- Emulator atau device Android dengan minimum SDK 24.
- Koneksi internet untuk fitur peta dan asset online jika diperlukan.
- Mapbox public access token untuk mengaktifkan fitur peta.

---

## ▶️ Cara Menjalankan Project

### 1. Clone Repository

```bash
git clone https://github.com/heyimflh/KosHub_App.git
cd KosHub_App
```

### 2. Buka di Android Studio

1. Buka Android Studio.
2. Pilih **Open**.
3. Arahkan ke folder `KosHub_App`.
4. Tunggu proses Gradle sync selesai.

### 3. Setup Mapbox Token

Ikuti langkah pada bagian [Setup Mapbox Token](#-setup-mapbox-token).

### 4. Jalankan Aplikasi

Pilih emulator/device, lalu klik:

```text
Run ▶
```

Atau gunakan terminal:

```bash
./gradlew assembleDebug
```

Untuk Windows:

```bash
gradlew.bat assembleDebug
```

---

## 🗺️ Setup Mapbox Token

Project ini menggunakan **Mapbox SDK v11** untuk fitur peta. Token asli **tidak boleh di-commit** ke GitHub.

File contoh sudah tersedia:

```text
app/developer-config.xml.example
```

### Langkah setup token lokal

1. Buat Mapbox Public Access Token dari dashboard Mapbox.
2. Pastikan token diawali dengan:

```text
pk.
```

3. Copy file contoh:

```text
app/developer-config.xml.example
```

4. Paste/copy isinya menjadi file baru:

```text
app/src/main/res/values/developer-config.xml
```

5. Ubah isi token:

```xml
<string name="mapbox_access_token" translatable="false">YOUR_MAPBOX_PUBLIC_TOKEN_HERE</string>
```

menjadi:

```xml
<string name="mapbox_access_token" translatable="false">pk.xxxxxxxxxxxxxxxxx</string>
```

6. Rebuild project.

### Catatan penting

File berikut sudah diabaikan oleh `.gitignore`:

```text
app/src/main/res/values/developer-config.xml
```

Jadi token asli tidak akan ikut ter-push ke GitHub selama file `.gitignore` tidak diubah.

---

## 🛡️ Force Close Prevention

Project ini memiliki beberapa proteksi agar aplikasi tetap aman walaupun token Mapbox belum diisi atau data tertentu kosong.

Proteksi yang digunakan:

- `MapboxTokenHelper` untuk validasi token Mapbox.
- MapView hanya diinisialisasi jika token valid.
- Fallback UI jika map tidak bisa ditampilkan.
- `try-catch` pada inisialisasi Mapbox.
- `runOnUiThread()` pada callback Mapbox yang menyentuh UI.
- Null safety pada `RecyclerView`.
- Null safety pada data `Intent`.
- Validasi data `KosItem`.

Jika token Mapbox belum diisi, aplikasi tetap dapat berjalan, tetapi fitur peta akan menampilkan fallback atau tidak aktif.

---

## 🧭 Activity Utama

| Activity | Fungsi |
|---|---|
| `SplashActivity` | Splash screen awal aplikasi. |
| `LoginActivity` | Halaman login pengguna. |
| `RegisterActivity` | Halaman registrasi pengguna. |
| `StudentHomeActivity` | Halaman utama mahasiswa untuk mencari kos. |
| `PropertyDetailBookingActivity` | Detail properti dan booking kos. |
| `MapViewRouteNavigationActivity` | Halaman map/rute berbasis Mapbox. |
| `WaitingListQueueActivity` | Halaman waiting list. |
| `ProfileHistoryActivity` | Profil dan riwayat mahasiswa/penyewa. |
| `TenantComplaintFormActivity` | Form komplain dari penyewa ke owner. |
| `OwnerDashboardActivity` | Dashboard utama owner. |
| `OwnerManagementActivity` | Manajemen kos/kamar/penyewa. |
| `OwnerBookingActivity` | Booking masuk owner. |
| `OwnerChatActivity` | Inbox chat owner. |
| `OwnerChatRoomActivity` | Detail percakapan/chat room. |
| `OwnerFinanceReportActivity` | Laporan keuangan owner. |
| `OwnerWithdrawActivity` | Form tarik saldo owner. |
| `OwnerComplaintActivity` | Laporan komplain masuk dari penyewa. |
| `OwnerProfileSettingsActivity` | Profil dan pengaturan owner. |

---

## 🎨 UI/UX Highlight

KosHub dirancang dengan pendekatan:

- Clean mobile UI.
- Card-based layout.
- Rounded corner.
- Soft shadow.
- Green identity color.
- Marketplace-style navigation.
- Bottom navigation konsisten untuk role owner.
- Smooth page transition.
- Splash screen profesional.
- Chat room dengan context card.
- Badge status untuk booking, komplain, dan saldo.
- Layout yang responsif untuk layar Android.

---

## 🔐 Catatan Keamanan GitHub

Sebelum push ke GitHub, pastikan file berikut **tidak ikut ter-commit**:

```text
local.properties
secrets.properties
app/src/main/res/values/developer-config.xml
*.jks
*.keystore
```

Jangan pernah menyimpan token asli di:

```text
README.md
strings.xml public
AndroidManifest.xml
build.gradle.kts
file Java
```

Gunakan file lokal yang masuk `.gitignore`.

---

## 🧪 Testing Checklist

Sebelum final/demo, lakukan pengujian berikut:

### General

- [ ] Aplikasi berhasil dibuka dari splash screen.
- [ ] Login dapat berpindah ke halaman sesuai role.
- [ ] Tidak ada force close saat navigasi antar halaman.
- [ ] Page transition berjalan smooth.

### Mahasiswa

- [ ] Student Home tampil.
- [ ] Search kos tidak crash.
- [ ] Klik favorite berjalan.
- [ ] Detail kos terbuka.
- [ ] Booking/waiting list terbuka.
- [ ] Profil dan riwayat tampil.
- [ ] Tombol komplain penyewa berjalan.

### Owner

- [ ] Owner Dashboard tampil.
- [ ] Bottom navigation konsisten.
- [ ] Booking Owner tampil.
- [ ] Chat Owner tampil.
- [ ] Chat Room sesuai item chat yang dipilih.
- [ ] Finance Report tampil.
- [ ] Tarik Saldo tampil.
- [ ] Complaint Inbox tampil.
- [ ] Owner Profile tampil.

### Mapbox

- [ ] Aplikasi tidak crash saat token kosong.
- [ ] Aplikasi tidak crash saat token placeholder.
- [ ] Map tampil saat token valid.
- [ ] Fallback map tampil saat token tidak valid.

---

## 🧩 Known Notes

Beberapa fitur masih berupa simulasi/dummy untuk kebutuhan project:

- Data kos masih lokal/dummy.
- Chat belum real-time.
- Booking belum terhubung backend.
- Komplain belum tersimpan ke database online.
- Tarik saldo masih simulasi UI.
- Upload foto bukti komplain belum aktif.
- Map membutuhkan token Mapbox valid agar tampil penuh.

---

## 🛣️ Roadmap Pengembangan

Rencana pengembangan berikutnya:

- [ ] Integrasi database lokal/online.
- [ ] Auth role-based yang lebih matang.
- [ ] Real-time chat.
- [ ] Upload foto kos dan bukti komplain.
- [ ] Integrasi pembayaran.
- [ ] Sistem saldo owner yang terhubung transaksi.
- [ ] Push notification.
- [ ] Rating dan review kos.
- [ ] Filter kos yang lebih lengkap.
- [ ] Deployment APK/release build.

---

## 👨‍💻 Kontributor

Project ini dikembangkan oleh:

**Muhammad Fakhri Abdullah**  
Mahasiswa Informatika  
Project UAS Struktur Data dan Algoritma

---

## 📄 Lisensi

Project ini dibuat untuk kebutuhan pembelajaran dan tugas akademik.  
Lisensi dapat disesuaikan kembali apabila project akan dikembangkan menjadi aplikasi publik.

---

<div align="center">

### KosHub SuperApp

**Temukan kos nyaman, kelola lebih mudah.**

</div>
