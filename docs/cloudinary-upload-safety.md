# Cloudinary Upload Safety Configuration

Karena aplikasi Android menggunakan **Unsigned Upload**, sangat penting untuk membatasi *Upload Preset* di Cloudinary Console untuk mencegah penyalahgunaan.

## Pengaturan di Cloudinary Console

Ikuti langkah-langkah berikut untuk mengonfigurasi Upload Preset `koshub_unsigned`:

### 1. Buat/Edit Upload Preset
- Buka **Settings** -> **Upload** -> **Upload presets**.
- Pilih preset bernama `koshub_unsigned` (atau buat baru jika belum ada).

### 2. Tab "General Settings"
- **Signing Mode**: `Unsigned` (Wajib).
- **Folder**: `koshub` (Semua upload akan masuk ke root folder ini).
- **Overwrite**: `Off` (Mencegah user menimpa file yang sudah ada).
- **Use file name as public ID**: `Off`.
- **Unique filename**: `On`.
- **Delivery type**: `Upload`.

### 3. Tab "Upload Control"
- **Allowed formats**: Masukkan `jpg, jpeg, png, webp`. Ini mencegah upload file berbahaya seperti `.exe`, `.pdf`, atau `.zip`.
- **Max file size**: Atur ke `5242880` bytes (5 MB).

### 4. Tab "Incoming Transformations" (Opsional tapi Direkomendasikan)
- Tambahkan transformasi untuk otomatis mengecilkan gambar di sisi server:
  - **Width**: `1280`
  - **Height**: `1280`
  - **Crop Mode**: `Limit`
  - **Quality**: `Auto`
  - **Format**: `Auto`

## Mengapa Ini Aman?

1. **Client-side Validation**: Aplikasi memvalidasi file sebelum dikirim (menghemat bandwidth).
2. **Server-side Validation**: Cloudinary menolak file jika melewati batas ukuran atau format yang diizinkan di preset, meskipun ada yang mencoba membypass aplikasi.
3. **No Secrets in APK**: Kita tidak menyimpan `API_SECRET` di aplikasi. Penyerang hanya bisa melakukan upload ke folder yang kita tentukan, tidak bisa menghapus atau mengubah data yang ada.
4. **Folder Separation**: Repository mengatur folder secara dinamis (misal: `koshub/profiles/{userId}`) untuk memudahkan manajemen aset.

> [!WARNING]
> Jangan pernah membagikan **API Secret** Cloudinary Anda di dalam kode sumber Android.
