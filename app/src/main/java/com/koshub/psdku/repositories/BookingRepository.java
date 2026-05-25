package com.koshub.psdku.repositories;

import com.koshub.psdku.models.Booking;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Booking data.
 * Initially returns dummy data.
 */
public class BookingRepository {
    private static BookingRepository instance;
    private List<Booking> dummyBookings;

    private BookingRepository() {
        initDummyData();
    }

    public static synchronized BookingRepository getInstance() {
        if (instance == null) {
            instance = new BookingRepository();
        }
        return instance;
    }

    public interface BookingCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    private void initDummyData() {
        dummyBookings = new ArrayList<>();
        dummyBookings.add(new Booking("1", "Muhammad Fakhri", "Mahasiswa UNS", "Kos Melati Indah", "A-12", "20 Mei 2026", "22 Mei 2026", "1 bulan", "Rp 850.000", "Menunggu"));
        dummyBookings.add(new Booking("2", "Ahmad Subarjo", "Karyawan", "Kos Melati Indah", "B-02", "20 Mei 2026", "23 Mei 2026", "1 bulan", "Rp 850.000", "Menunggu"));
        dummyBookings.add(new Booking("3", "Siti Aminah", "Mahasiswi", "Kos Melati Indah", "A-07", "20 Mei 2026", "25 Mei 2026", "1 bulan", "Rp 850.000", "Menunggu"));
        dummyBookings.add(new Booking("4", "Raka Pratama", "Mahasiswa Baru", "Kos Mawar Residence", "B-04", "19 Mei 2026", "25 Mei 2026", "3 bulan", "Rp 900.000", "Siap Check-in"));
        dummyBookings.add(new Booking("5", "Sinta Aulia", "Mahasiswa Aktif", "Kos Melati Indah", "A-05", "18 Mei 2026", "21 Mei 2026", "6 bulan", "Rp 850.000", "Aktif Ngekos"));
        dummyBookings.add(new Booking("6", "Dimas Saputra", "Mahasiswa", "Kos Anggrek", "C-02", "17 Mei 2026", "24 Mei 2026", "1 bulan", "Rp 750.000", "Ditolak"));
        dummyBookings.add(new Booking("7", "Nabila Putri", "Mahasiswa", "Kos Mawar Residence", "B-07", "15 Mei 2026", "16 Mei 2026", "1 bulan", "Rp 900.000", "Selesai"));
    }

    public void getBookingsByUser(String userId, BookingCallback<List<Booking>> callback) {
        // TODO: Replace with Firebase query where tenantId == userId
        if (callback != null) {
            callback.onSuccess(new ArrayList<>(dummyBookings));
        }
    }

    public void createBooking(Booking booking, BookingCallback<Boolean> callback) {
        // TODO: Implement Firebase Create
        dummyBookings.add(booking);
        if (callback != null) {
            callback.onSuccess(true);
        }
    }
}
