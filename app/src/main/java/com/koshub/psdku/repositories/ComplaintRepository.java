package com.koshub.psdku.repositories;

import com.koshub.psdku.models.Complaint;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for Complaint data.
 * Initially returns dummy data.
 */
public class ComplaintRepository {
    private static ComplaintRepository instance;
    private List<Complaint> dummyComplaints;

    private ComplaintRepository() {
        initDummyData();
    }

    public static synchronized ComplaintRepository getInstance() {
        if (instance == null) {
            instance = new ComplaintRepository();
        }
        return instance;
    }

    public interface ComplaintCallback<T> {
        void onSuccess(T result);
        void onError(String message);
    }

    private void initDummyData() {
        dummyComplaints = new ArrayList<>();
        dummyComplaints.add(new Complaint("1", "Muhammad Fakhri", "Kos Melati Indah", "A-12", "Fasilitas", "AC kamar tidak dingin", "20 Mei 2026", "Baru"));
        dummyComplaints.add(new Complaint("2", "Sinta Aulia", "Kos Melati Indah", "A-05", "Internet", "WiFi sering mati", "19 Mei 2026", "Diproses"));
        dummyComplaints.add(new Complaint("3", "Raka Pratama", "Kos Mawar Residence", "B-04", "Pembayaran", "Pembayaran belum terverifikasi", "18 Mei 2026", "Selesai"));
    }

    public void getAllComplaints(ComplaintCallback<List<Complaint>> callback) {
        // TODO: Replace with Firebase query
        if (callback != null) {
            callback.onSuccess(new ArrayList<>(dummyComplaints));
        }
    }

    public void createComplaint(Complaint complaint, ComplaintCallback<Boolean> callback) {
        // TODO: Implement Firebase Create
        dummyComplaints.add(complaint);
        if (callback != null) {
            callback.onSuccess(true);
        }
    }
}
