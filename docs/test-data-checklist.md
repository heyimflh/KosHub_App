# Test Data Checklist - KosHub

Pastikan data berikut tersedia di Firestore sebelum memulai testing integrasi.

## 1. User Accounts
- [ ] **Student Account**
  - UID: `TEST_STUDENT_UID`
  - Name: `Student Tester`
  - Role: `student`
- [ ] **Owner Account**
  - UID: `TEST_OWNER_UID`
  - Name: `Owner Tester`
  - Role: `owner`

## 2. Property Data
- [ ] **Kos Document**
  - ownerId: `TEST_OWNER_UID`
  - Name: `Kos Test Mandiri`
- [ ] **Room Document**
  - kosId: `TEST_KOS_ID`
  - ownerId: `TEST_OWNER_UID`
  - Status: `available`

## 3. Interaction Data
- [ ] **Booking Flow Data**
  - [ ] Pending booking (Student -> Owner)
  - [ ] Accepted booking (Owner -> Student)
  - [ ] Paid booking (Student)
  - [ ] Active booking (Key Taken)
  - [ ] Completed booking (Ready for review)
- [ ] **Chat Room**
  - [ ] Chat student <-> owner
- [ ] **Complaint**
  - [ ] Complaint linked to active booking
- [ ] **Finance**
  - [ ] Available transaction (after check-in)
  - [ ] Withdrawal history

## 4. System Data
- [ ] **FCM Token**
  - [ ] Tersimpan di `users/{uid}/fcmTokens`
- [ ] **Notification**
  - [ ] Contoh notifikasi untuk student
  - [ ] Contoh notifikasi untuk owner
