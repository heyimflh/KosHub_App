# Phase 7: Real Complaint Flow Documentation

## 1. Firestore Schema: `complaints` collection

| Field | Type | Description |
|---|---|---|
| `id` | String | Document ID |
| `studentId` | String | UID of the student |
| `studentName` | String | Name of the student (cached from booking) |
| `studentEmail` | String | Email of the student (cached from booking) |
| `ownerId` | String | UID of the kos owner |
| `kosId` | String | ID of the kos |
| `kosName` | String | Name of the kos |
| `bookingId` | String | ID of the active booking |
| `roomId` | String | ID of the room |
| `roomName` | String | Name of the room |
| `title` | String | Complaint title (includes category prefix) |
| `description` | String | Detailed complaint description |
| `imageUrl` | String | URL of the evidence photo (Cloudinary) |
| `evidenceImageUrls`| List<String> | Array of evidence photo URLs |
| `status` | String | `new`, `process`, `done`, `rejected` |
| `ownerResponse` | String | Response from the owner when finishing/rejecting |
| `createdAt` | long | Timestamp |
| `updatedAt` | long | Timestamp |
| `resolvedAt` | long | Timestamp when status becomes `done` or `rejected` |

## 2. Complaint Statuses

- **`new`**: Newly created complaint.
- **`process`**: Owner has acknowledged and is working on it.
- **`done`**: Issue resolved.
- **`rejected`**: Complaint dismissed by owner.

## 3. Student Flow (Create Complaint)

1. Student must have an **active** booking (`status == "active"`).
2. Student navigates to `TenantComplaintFormActivity` (from Profile or Booking list).
3. Student fills:
    - **Category**: (Spinner) e.g., Facility, Internet, etc.
    - **Title**: Brief summary.
    - **Description**: Detailed explanation.
    - **Evidence**: (Optional) Pick an image from gallery.
4. On Submit:
    - Metadata is fetched from the active booking.
    - Complaint document is created in Firestore.
    - If image selected, it uploads to Cloudinary: `koshub/complaints/{uid}/{complaintId}`.
    - Document is updated with the Cloudinary URL.

## 4. Owner Flow (Handle Complaint)

1. Owner opens `OwnerComplaintActivity`.
2. List shows real complaints filtered by `ownerId`.
3. Actions:
    - **Proses**: Set status to `process`.
    - **Selesai**: Set status to `done` and add optional response.
    - **Tolak**: Set status to `rejected` and add optional response.
4. Detail view allows viewing the full description and evidence image.

## 5. Security Note (Firestore Rules)

- `create`: Only allowed for authenticated students.
- `read`: Students can read their own; Owners can read for their `kos`.
- `update`: Only Owners can update `status` and `ownerResponse`.

## 6. Known Limitations

- Real-time chat for complaints is NOT implemented in this phase.
- Only one evidence photo is supported per upload in the current UI, although the schema supports a list.
- Complaints cannot be edited or deleted by students once submitted.
