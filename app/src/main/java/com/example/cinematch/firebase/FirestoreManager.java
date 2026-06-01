package com.example.cinematch.firebase;

import com.example.cinematch.models.Rating;
import com.example.cinematch.models.Report;
import com.example.cinematch.models.Review;
import com.example.cinematch.models.User;
import com.example.cinematch.models.WatchlistItem;
import com.example.cinematch.utils.Constants;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

// Toàn bộ thao tác CRUD với Firestore: users, ratings, reviews, watchlist, reports.
// Firestore Task API vốn đã bất đồng bộ (chạy nền), nên không cần bọc thêm ThreadUtils ở đây.
public class FirestoreManager {

    // ============ Callback interfaces dùng chung ============
    public interface SimpleCallback {
        void onSuccess();
        void onError(String message);
    }
    public interface UserCallback {
        void onSuccess(User user);
        void onError(String message);
    }
    public interface ListCallback<T> {
        void onSuccess(List<T> list);
        void onError(String message);
    }
    public interface BooleanCallback {
        void onResult(boolean result);
    }

    private final FirebaseFirestore db;

    public FirestoreManager() {
        db = FirebaseFirestore.getInstance();
    }

    // ================= USERS =================

    public void createUserDocument(User user, SimpleCallback callback) {
        db.collection(Constants.COLLECTION_USERS).document(user.getUid())
                .set(user)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getUserProfile(String uid, UserCallback callback) {
        db.collection(Constants.COLLECTION_USERS).document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = doc.toObject(User.class);
                        callback.onSuccess(user);
                    } else {
                        callback.onError("Không tìm thấy profile người dùng");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ================= RATINGS =================

    // Id document = uid_movieId -> 1 user chỉ có 1 rating cho 1 phim, rate lại thì ghi đè
    public void addOrUpdateRating(Rating rating, SimpleCallback callback) {
        String docId = rating.getUserId() + "_" + rating.getMovieId();
        db.collection(Constants.COLLECTION_RATINGS).document(docId)
                .set(rating)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Lấy toàn bộ rating của 1 user -> input cho RecommendationEngine tính genre weight
    public void getUserRatings(String userId, ListCallback<Rating> callback) {
        db.collection(Constants.COLLECTION_RATINGS)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(query -> {
                    List<Rating> ratings = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        ratings.add(doc.toObject(Rating.class));
                    }
                    callback.onSuccess(ratings);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ================= REVIEWS =================

    public void addReview(Review review, SimpleCallback callback) {
        db.collection(Constants.COLLECTION_REVIEWS)
                .add(review)
                .addOnSuccessListener(docRef -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Chỉ lấy review đang "visible" -> review bị ẩn do moderator sẽ không hiện với user thường.
    // Lưu ý: KHÔNG dùng .orderBy() kèm 2 whereEqualTo() vì Firestore bắt buộc phải tạo
    // composite index thủ công cho tổ hợp đó -> sort lại ở client để tránh phải cấu hình index.
    public void getReviewsForMovie(int movieId, ListCallback<Review> callback) {
        db.collection(Constants.COLLECTION_REVIEWS)
                .whereEqualTo("movieId", movieId)
                .whereEqualTo("status", Review.STATUS_VISIBLE)
                .get()
                .addOnSuccessListener(query -> {
                    List<Review> reviews = mapReviews(query);
                    reviews.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    callback.onSuccess(reviews);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    private List<Review> mapReviews(Iterable<QueryDocumentSnapshot> query) {
        List<Review> reviews = new ArrayList<>();
        for (QueryDocumentSnapshot doc : query) {
            Review review = doc.toObject(Review.class);
            review.setReviewId(doc.getId());
            reviews.add(review);
        }
        return reviews;
    }

    // ================= WATCHLIST =================

    public void addToWatchlist(WatchlistItem item, SimpleCallback callback) {
        String docId = item.getUserId() + "_" + item.getMovieId();
        db.collection(Constants.COLLECTION_WATCHLIST).document(docId)
                .set(item)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void removeFromWatchlist(String userId, int movieId, SimpleCallback callback) {
        String docId = userId + "_" + movieId;
        db.collection(Constants.COLLECTION_WATCHLIST).document(docId)
                .delete()
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void isInWatchlist(String userId, int movieId, BooleanCallback callback) {
        String docId = userId + "_" + movieId;
        db.collection(Constants.COLLECTION_WATCHLIST).document(docId).get()
                .addOnSuccessListener(doc -> callback.onResult(doc.exists()))
                .addOnFailureListener(e -> callback.onResult(false));
    }

    // Lưu ý: bỏ .orderBy() kèm whereEqualTo() để không bắt buộc phải tạo composite index
    // trên Firestore Console (đây chính là nguyên nhân lỗi FAILED_PRECONDITION khi mở Watchlist).
    public void getWatchlist(String userId, ListCallback<WatchlistItem> callback) {
        db.collection(Constants.COLLECTION_WATCHLIST)
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(query -> {
                    List<WatchlistItem> items = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        items.add(doc.toObject(WatchlistItem.class));
                    }
                    items.sort((a, b) -> Long.compare(b.getAddedAt(), a.getAddedAt()));
                    callback.onSuccess(items);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ================= REPORTS (Moderator) =================

    // User báo cáo 1 review vi phạm -> tạo document report + tăng reportCount trên review đó
    public void reportReview(Report report, SimpleCallback callback) {
        db.collection(Constants.COLLECTION_REPORTS)
                .add(report)
                .addOnSuccessListener(docRef -> {
                    db.collection(Constants.COLLECTION_REVIEWS).document(report.getReviewId())
                            .update("reportCount", FieldValue.increment(1))
                            .addOnSuccessListener(unused -> callback.onSuccess())
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Lấy toàn bộ review "visible" rồi lọc reportCount > 0 ở client, tránh phải tạo
    // composite index cho tổ hợp whereGreaterThan + whereEqualTo trên Firestore Console.
    public void getReportedReviews(ListCallback<Review> callback) {
        db.collection(Constants.COLLECTION_REVIEWS)
                .whereEqualTo("status", Review.STATUS_VISIBLE)
                .get()
                .addOnSuccessListener(query -> {
                    List<Review> reviews = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        Review review = doc.toObject(Review.class);
                        if (review.getReportCount() > 0) {
                            review.setReviewId(doc.getId());
                            reviews.add(review);
                        }
                    }
                    callback.onSuccess(reviews);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Moderator duyệt: giữ lại review hợp lệ (không đổi status, chỉ dùng để reset UI phía dashboard)
    public void dismissReports(String reviewId, SimpleCallback callback) {
        updateReviewStatus(reviewId, Review.STATUS_VISIBLE, callback);
    }

    // Moderator xóa/ẩn review vi phạm
    public void hideReview(String reviewId, SimpleCallback callback) {
        updateReviewStatus(reviewId, Review.STATUS_HIDDEN, callback);
    }

    private void updateReviewStatus(String reviewId, String status, SimpleCallback callback) {
        db.collection(Constants.COLLECTION_REVIEWS).document(reviewId)
                .update("status", status)
                .addOnSuccessListener(unused -> callback.onSuccess())
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
