package com.example.cinematch.firebase;

import android.content.Context;

import com.example.cinematch.models.User;
import com.example.cinematch.utils.SharedPrefManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

// Quản lý toàn bộ đăng nhập/đăng ký + đồng bộ session vào SharedPreferences.
// Role được lưu ở Firestore (collection "users"), AuthManager chỉ lo phần Authentication.
public class AuthManager {

    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String message);
    }

    private final FirebaseAuth firebaseAuth;
    private final FirestoreManager firestoreManager;
    private final SharedPrefManager prefManager;

    public AuthManager(Context context) {
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.firestoreManager = new FirestoreManager();
        this.prefManager = new SharedPrefManager(context);
    }

    public FirebaseUser getCurrentFirebaseUser() {
        return firebaseAuth.getCurrentUser();
    }

    public boolean isLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    // Đăng ký: tạo tài khoản Auth -> tạo document User trong Firestore với role mặc định "user"
    public void register(String email, String password, String displayName, AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        callback.onError("Không lấy được thông tin tài khoản vừa tạo");
                        return;
                    }
                    User newUser = new User(firebaseUser.getUid(), email, displayName);
                    firestoreManager.createUserDocument(newUser, new FirestoreManager.SimpleCallback() {
                        @Override
                        public void onSuccess() {
                            prefManager.saveSession(newUser.getUid(), newUser.getRole());
                            callback.onSuccess(newUser);
                        }
                        @Override
                        public void onError(String message) {
                            callback.onError("Tạo tài khoản OK nhưng lưu profile lỗi: " + message);
                        }
                    });
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // Đăng nhập: xác thực Auth -> load lại User document để biết role (user/moderator)
    public void login(String email, String password, AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        callback.onError("Đăng nhập thất bại");
                        return;
                    }
                    firestoreManager.getUserProfile(firebaseUser.getUid(), new FirestoreManager.UserCallback() {
                        @Override
                        public void onSuccess(User user) {
                            prefManager.saveSession(user.getUid(), user.getRole());
                            callback.onSuccess(user);
                        }
                        @Override
                        public void onError(String message) {
                            callback.onError("Đăng nhập OK nhưng không tải được profile: " + message);
                        }
                    });
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void logout() {
        firebaseAuth.signOut();
        prefManager.clearSession();
    }
}
