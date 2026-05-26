# Integration Testing & QA Hardening Plan - KosHub

## 1. Tujuan Testing Integrasi
Memastikan seluruh fitur KosHub (Auth, Booking, Chat, Complaint, Finance, Review, Notification) berjalan harmonis sesuai role Student dan Owner, serta menangani edge case dengan aman (no force close).

## 2. Akun Testing yang Dibutuhkan
- **Student A:** `student_test_a@koshub.com` / `password123`
- **Owner A:** `owner_test_a@koshub.com` / `password123`
- **Owner B:** `owner_test_b@koshub.com` / `password123` (untuk testing data isolation)

## 3. Data Awal yang Harus Ada
- Setidaknya 3 Kos milik Owner A dengan kategori berbeda (Putra, Putri, Campur).
- Setidaknya 5 Kamar pada salah satu Kos Owner A dengan status 'available'.
- Setidaknya 1 Kos milik Owner B.

## 4. Checklist Testing Role Student
- [ ] **Auth:** Register, Login, Logout, Session persistence.
- [ ] **Home:** Melihat daftar kos real dari Firestore.
- [ ] **Search:** Filter berdasarkan kategori dan pencarian nama.
- [ ] **Detail:** Informasi harga, fasilitas, lokasi (Mapbox), dan ulasan real.
- [ ] **Favorite:** Menambah/menghapus kos dari daftar favorit.
- [ ] **Chat:** Memulai chat dari halaman detail kos atau booking.
- [ ] **Booking:** Mengajukan booking (status 'pending').
- [ ] **Waiting List:** Memantau timeline status booking (Pending -> Accepted -> Waiting Checkin -> Active).
- [ ] **Payment:** Simulasi pembayaran (status 'waiting_checkin').
- [ ] **Check-in:** Konfirmasi ambil kunci (status 'active').
- [ ] **Complaint:** Mengirim komplain untuk sewa aktif.
- [ ] **Review:** Memberi rating dan ulasan untuk booking yang sudah 'completed'.
- [ ] **Notification:** Menerima notifikasi real-time saat status booking berubah atau ada chat masuk.

## 5. Checklist Testing Role Owner
- [ ] **Dashboard:** Melihat statistik kos (Total Kos, Kamar Terisi, Pendapatan).
- [ ] **Kos Management:** Tambah Kos, Edit Fasilitas, Upload Foto (Cloudinary).
- [ ] **Room Management:** Tambah Kamar, Update status kamar.
- [ ] **Booking Management:** Menerima (Accept) atau Menolak (Reject) booking masuk.
- [ ] **Chat:** Membalas chat dari penyewa.
- [ ] **Complaint Management:** Memproses komplain (Process -> Done).
- [ ] **Finance:** Melihat riwayat transaksi dan saldo tersedia.
- [ ] **Withdraw:** Mengajukan penarikan saldo (simulasi).
- [ ] **Notification:** Menerima notifikasi booking baru, chat, dan komplain.

## 6. Checklist Testing Edge Case
- [ ] **No Internet:** Aplikasi tidak crash, menampilkan pesan "Koneksi bermasalah".
- [ ] **Empty Data:** Halaman menampilkan Empty State (ilustrasi/teks), bukan layar kosong atau crash.
- [ ] **Permission Denied:** User tidak bisa mengakses data user lain (Security Rules validation).
- [ ] **Mapbox Fail:** Peta gagal load (token kosong/invalid) tidak menyebabkan app crash.
- [ ] **Duplicate Booking:** Student tidak bisa booking ganda untuk kos yang sama jika masih ada proses aktif.
- [ ] **Insufficient Balance:** Owner tidak bisa withdraw melebihi saldo tersedia.

## 7. Checklist Firestore Collections
- [ ] `users`: Role dan profile data tersimpan benar.
- [ ] `kos` & `rooms`: Relasi `ownerId` dan `kosId` valid.
- [ ] `bookings`: `studentId` dan `ownerId` tersimpan untuk filter query.
- [ ] `notifications`: `recipientId` benar agar notifikasi tidak nyasar.

## 8. Checklist Security Rules
- [ ] Student tidak bisa baca data `withdrawals` owner manapun.
- [ ] Owner A tidak bisa baca `chats` milik Owner B.
- [ ] Student tidak bisa update data `kos` orang lain (kecuali field rating via review).

## 9. Kriteria Kelulusan
1. Seluruh flow end-to-end Student dan Owner sukses tanpa crash.
2. Tidak ada tombol utama yang hanya memunculkan Toast dummy tanpa aksi.
3. Seluruh data sensitif terisolasi antar user.
4. Build `./gradlew assembleDebug` berhasil tanpa error.
