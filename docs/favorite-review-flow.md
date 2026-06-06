# Favorite and Review Flow Documentation

## 1. Favorites Collection
- **Path**: `favorites/{favoriteId}`
- **ID Strategy**: `userId + "_" + kosId`
- **Fields**:
  - `id`: String (e.g., "uid123_kosabc")
  - `userId`: String
  - `kosId`: String
  - `kosName`: String
  - `kosAddress`: String
  - `kosImageUrl`: String
  - `createdAt`: Long

## 2. Reviews Collection
- **Path**: `reviews/{reviewId}`
- **ID Strategy**: `bookingId` (Ensures one review per booking)
- **Fields**:
  - `id`: String (e.g., "booking456")
  - `studentId`: String
  - `studentName`: String
  - `kosId`: String
  - `kosName`: String
  - `bookingId`: String
  - `rating`: Double (1.0 to 5.0)
  - `comment`: String
  - `createdAt`: Long
  - `updatedAt`: Long

## 3. Aggregate Ratings
When a review is created:
1. All reviews for the `kosId` are fetched.
2. `ratingAverage` and `ratingCount` are recalculated.
3. The `kos/{kosId}` document is updated with:
   - `ratingAverage`
   - `ratingCount`
   - `rating` (for backward compatibility)

## 4. Business Rules
- **Favorites**: Only logged-in users. Students can toggle favorites from the detail screen.
- **Reviews**: Only for bookings with status `completed`.
- **Duplicates**: Prevented by ID strategies.

## 5. Testing
To test reviews, manually change a booking status to `completed` in the Firestore console.
The "Beri Review" button will then appear in the student's Profile History under recent bookings.
