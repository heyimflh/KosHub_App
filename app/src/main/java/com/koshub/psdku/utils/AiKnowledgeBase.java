package com.koshub.psdku.utils;

import com.koshub.psdku.models.AiFaqItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AiKnowledgeBase {

    public static List<AiFaqItem> getFaqItems() {
        List<AiFaqItem> items = new ArrayList<>();

        // GENERAL
        items.add(new AiFaqItem(
                "gen_login",
                "Masalah Login",
                "general",
                Arrays.asList("login", "masuk", "tidak bisa masuk", "gagal login", "lupa password"),
                "Untuk masuk ke akun kamu, pastikan email dan password yang kamu masukkan sudah benar. Jika kamu lupa password, gunakan fitur 'Lupa Password' di halaman login untuk mengatur ulang kata sandi kamu."
        ));

        items.add(new AiFaqItem(
                "gen_register",
                "Cara Daftar Akun",
                "general",
                Arrays.asList("register akun", "daftar akun", "membuat akun", "akun baru", "registrasi"),
                "Kamu bisa mendaftar dengan menekan tombol 'Daftar' di halaman awal. Pilih peran kamu sebagai Mahasiswa atau Pemilik Kos, lalu lengkapi data diri seperti nama, email, dan nomor WhatsApp yang aktif."
        ));

        items.add(new AiFaqItem(
                "gen_verify",
                "Verifikasi Email",
                "general",
                Arrays.asList("verifikasi email", "kode verifikasi", "konfirmasi email", "link verifikasi"),
                "Setelah mendaftar, cek kotak masuk atau folder spam email kamu untuk menemukan link verifikasi. Verifikasi email penting agar kamu bisa menggunakan semua fitur KosHub dengan aman."
        ));

        items.add(new AiFaqItem(
                "gen_profile",
                "Ubah Profil",
                "general",
                Arrays.asList("ubah profil", "ganti foto", "edit profil", "identitas diri", "update profil"),
                "Untuk mengubah profil, buka menu Profil di pojok kanan bawah, lalu pilih 'Edit Profil'. Kamu bisa memperbarui foto, nama, nomor WhatsApp, dan data lainnya di sana."
        ));

        items.add(new AiFaqItem(
                "gen_connection",
                "Koneksi Internet Error",
                "general",
                Arrays.asList("koneksi internet", "internet error", "gangguan jaringan", "offline", "tidak ada sinyal"),
                "Jika muncul pesan error koneksi, pastikan paket data atau Wi-Fi kamu aktif dan stabil. Coba muat ulang halaman atau tutup dan buka kembali aplikasi KosHub."
        ));

        items.add(new AiFaqItem(
                "gen_permission",
                "Permission Denied",
                "general",
                Arrays.asList("permission denied", "akses ditolak", "tidak diizinkan", "izin aplikasi", "masalah akses"),
                "Pesan 'Permission Denied' biasanya muncul karena akun tidak memiliki izin untuk mengakses data tertentu atau sesi login bermasalah. Coba logout lalu login kembali, dan pastikan kamu menggunakan akun dengan role yang benar."
        ));

        // STUDENT
        items.add(new AiFaqItem(
                "std_search",
                "Cara Cari Kos",
                "student",
                Arrays.asList("cari kos", "mencari kos", "filter kos", "pencarian", "temukan kos"),
                "Untuk cari kos di KosHub, buka halaman Beranda lalu gunakan kolom pencarian di bagian atas. Kak bisa filter berdasarkan lokasi, harga, dan tipe kamar. Setelah menemukan kos yang cocok, klik untuk melihat detailnya."
        ));

        items.add(new AiFaqItem(
                "std_booking",
                "Cara Booking Kos",
                "student",
                Arrays.asList("booking kos", "pesan kamar", "sewa kos", "cara booking", "ajukan booking", "sewa kamar"),
                "Untuk booking kos, buka detail kos yang kamu inginkan, lalu pilih kamar yang tersedia. Setelah itu tekan tombol 'Booking' dan ikuti instruksi di layar. Pantau statusnya di menu Riwayat Booking."
        ));

        items.add(new AiFaqItem(
                "std_pending",
                "Status Booking Pending",
                "student",
                Arrays.asList("booking pending", "menunggu konfirmasi", "konfirmasi owner", "status pending", "belum disetujui"),
                "Jika status booking masih 'Pending', berarti permintaan kamu sedang menunggu persetujuan dari pemilik kos. Mohon tunggu maksimal 1x24 jam. Jika terlalu lama, kamu bisa mencoba chat pemiliknya langsung."
        ));

        items.add(new AiFaqItem(
                "std_payment",
                "Pembayaran Booking",
                "student",
                Arrays.asList("pembayaran", "bayar kos", "transfer bank", "qris", "bayar booking", "belum berhasil", "transaksi"),
                "Jika pembayaran belum berhasil, pastikan koneksi internet stabil dan kamu mengikuti instruksi dari halaman pembayaran. Tunggu beberapa saat sampai status diperbarui. Jika tetap bermasalah, hubungi CS melalui Pusat Bantuan."
        ));

        items.add(new AiFaqItem(
                "std_chat",
                "Chat Owner",
                "student",
                Arrays.asList("chat owner", "chat pemilik", "hubungi pemilik", "kontak owner", "chat tidak muncul", "pesan tidak masuk", "hubungi owner"),
                "Kamu bisa menghubungi pemilik kos melalui tombol 'Chat' yang ada di halaman detail kos atau melalui menu Chat jika kamu sudah pernah memulai percakapan sebelumnya."
        ));

        items.add(new AiFaqItem(
                "std_complaint",
                "Cara Mengirim Komplain",
                "student",
                Arrays.asList("komplain", "mengirim komplain", "kirim komplain", "lapor masalah", "keluhan", "ajukan keluhan", "masalah kamar"),
                "Jika ada kendala dengan kos atau fasilitas, kamu bisa mengajukan komplain melalui menu 'Riwayat Booking', pilih transaksi yang aktif, lalu cari tombol 'Ajukan Komplain'."
        ));

        items.add(new AiFaqItem(
                "std_waitlist",
                "Waiting List",
                "student",
                Arrays.asList("waiting list", "antrian kamar", "kamar penuh", "ingatkan saya"),
                "Fitur Waiting List memungkinkan kamu mendapatkan notifikasi jika ada kamar yang kosong di kos favoritmu. Cukup tekan tombol 'Ingatkan Saya' pada kos yang sudah penuh."
        ));

        items.add(new AiFaqItem(
                "std_history",
                "Riwayat Booking",
                "student",
                Arrays.asList("riwayat booking", "history pesanan", "daftar booking", "lihat transaksi"),
                "Semua daftar pesanan kos kamu bisa dilihat di menu 'Riwayat' pada navigasi bawah. Di sana kamu bisa melihat status booking yang aktif maupun yang sudah selesai."
        ));

        items.add(new AiFaqItem(
                "std_favorite",
                "Favorite/Wishlist",
                "student",
                Arrays.asList("favorit kos", "wishlist", "simpan kos", "kos disukai"),
                "Klik ikon hati pada kos yang kamu suka untuk menyimpannya ke daftar Favorit. Kamu bisa melihat daftar ini kapan saja melalui menu 'Favorit' di dashboard."
        ));

        items.add(new AiFaqItem(
                "std_review",
                "Memberi Ulasan",
                "student",
                Arrays.asList("ulasan kos", "review kamar", "rating bintang", "kasih nilai", "tulis ulasan"),
                "Setelah masa sewa berakhir atau booking selesai, kamu bisa memberikan rating dan ulasan untuk kos tersebut melalui halaman Riwayat Booking."
        ));

        // OWNER
        items.add(new AiFaqItem(
                "own_add_kos",
                "Tambah Kos Baru",
                "owner",
                Arrays.asList("tambah kos", "menambah kos", "daftarkan kos", "pasang iklan", "input kos baru", "buat kos"),
                "Untuk menambah kos, buka menu 'Manajemen Kos' di dashboard Owner, lalu tekan tombol 'Tambah Kos'. Lengkapi data alamat, fasilitas, dan foto kos kamu."
        ));

        items.add(new AiFaqItem(
                "own_edit_kos",
                "Edit Data Kos",
                "owner",
                Arrays.asList("edit kos", "ubah data kos", "update informasi kos", "ganti info kos"),
                "Kamu bisa mengubah informasi kos melalui menu 'Manajemen Kos', pilih kos yang ingin diedit, lalu tekan ikon pensil atau tombol 'Edit'."
        ));

        items.add(new AiFaqItem(
                "own_add_room",
                "Tambah Kamar",
                "owner",
                Arrays.asList("tambah kamar", "menambah kamar", "input kamar", "kamar baru", "buat kamar"),
                "Di dalam menu detail kos pada Manajemen Kos, pilih tab 'Kamar' lalu tekan 'Tambah Kamar' untuk memasukkan tipe kamar baru beserta harganya."
        ));

        items.add(new AiFaqItem(
                "own_update_room",
                "Update Status Kamar",
                "owner",
                Arrays.asList("status kamar", "kamar penuh", "kamar tersedia", "update ketersediaan"),
                "Pastikan status kamar selalu update. Kamu bisa menandai kamar sebagai 'Penuh' atau 'Tersedia' melalui daftar kamar di menu Manajemen Kos."
        ));

        items.add(new AiFaqItem(
                "own_accept_booking",
                "Menerima Booking",
                "owner",
                Arrays.asList("terima booking", "konfirmasi booking", "setujui pesanan", "acc booking"),
                "Setiap ada pesanan masuk, kamu akan mendapat notifikasi. Buka menu 'Booking' di dashboard Owner untuk melihat detail calon penghuni dan tekan 'Terima' jika kamu setuju."
        ));

        items.add(new AiFaqItem(
                "own_reject_booking",
                "Menolak Booking",
                "owner",
                Arrays.asList("tolak booking", "batalkan pesanan", "cancel booking", "reject booking"),
                "Jika kamar sudah penuh secara offline atau data calon penghuni tidak sesuai, kamu bisa menekan tombol 'Tolak' pada detail pesanan di menu Booking."
        ));

        items.add(new AiFaqItem(
                "own_chat",
                "Chat Mahasiswa",
                "owner",
                Arrays.asList("chat mahasiswa", "hubungi penyewa", "balas pesan", "chat tidak masuk"),
                "Gunakan fitur Chat di dashboard Owner untuk berkomunikasi dengan calon penghuni atau penghuni yang sudah aktif menyewa di kos kamu."
        ));

        items.add(new AiFaqItem(
                "own_photo",
                "Upload Foto",
                "owner",
                Arrays.asList("upload foto", "unggah foto", "foto kos", "foto kamar", "ganti gambar"),
                "Pastikan foto kos jernih dan menarik. Kamu bisa mengunggah foto melalui menu Tambah/Edit Kos. Gunakan foto asli agar calon penghuni lebih percaya."
        ));

        items.add(new AiFaqItem(
                "own_finance",
                "Laporan Keuangan",
                "owner",
                Arrays.asList("laporan keuangan", "finance owner", "pendapatan", "penghasilan", "tarik saldo"),
                "KosHub menyediakan ringkasan pendapatan di dashboard Owner. Kamu bisa melihat total penghasilan bulanan dan riwayat transaksi pembayaran dari penghuni."
        ));

        // MISMATCH ROLE FAQ
        items.add(new AiFaqItem(
                "std_ask_own_add_kos",
                "Cara Tambah Kos (Student asking)",
                "student",
                Arrays.asList("tambah kos", "daftarkan kos", "buat kos"),
                "Fitur tambah kos hanya tersedia untuk akun owner, Kak. Kalau Kak memakai akun mahasiswa, Kak bisa memakai fitur cari kos, booking kamar, chat owner, komplain, favorite, dan riwayat booking."
        ));

        items.add(new AiFaqItem(
                "std_ask_own_add_room",
                "Cara Tambah Kamar (Student asking)",
                "student",
                Arrays.asList("tambah kamar", "buat kamar"),
                "Fitur tambah kamar hanya tersedia untuk akun owner, Kak. Jika Kak adalah pemilik kos, pastikan login menggunakan akun owner. Kalau memakai akun mahasiswa, fitur yang tersedia adalah cari kos, booking, chat owner, dan komplain."
        ));

        items.add(new AiFaqItem(
                "own_ask_std_booking",
                "Cara Booking Kos (Owner asking)",
                "owner",
                Arrays.asList("booking kos", "pesan kamar"),
                "Fitur booking biasanya digunakan oleh mahasiswa, Kak. Untuk akun owner, Kak bisa melihat dan mengelola booking yang masuk melalui dashboard atau menu booking owner."
        ));

        items.add(new AiFaqItem(
                "own_ask_std_payment",
                "Cara Bayar Kos (Owner asking)",
                "owner",
                Arrays.asList("cara bayar", "pembayaran"),
                "Pembayaran biasanya dilakukan dari akun mahasiswa, Kak. Sebagai owner, Kak bisa memantau status booking dan transaksi dari dashboard atau laporan jika fitur tersebut tersedia."
        ));

        return items;
    }
}
