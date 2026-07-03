package com.example.cinematch.firebase;

import android.net.Uri;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

// Quản lý upload ảnh lên Firebase Storage. Dùng cho tính năng đổi ảnh đại diện
// (chọn ảnh từ bộ sưu tập -> upload -> lưu downloadUrl vào Firestore users/{uid}.avatarUrl).
public class StorageManager {

    public interface UploadCallback {
        void onSuccess(String downloadUrl);
        void onError(String message);
    }

    private final FirebaseStorage storage;

    public StorageManager() {
        storage = FirebaseStorage.getInstance();
    }

    // Upload ảnh đại diện, path cố định theo uid -> ảnh mới sẽ tự ghi đè ảnh cũ, không rác Storage
    public void uploadAvatar(Uri imageUri, String uid, UploadCallback callback) {
        StorageReference avatarRef = storage.getReference().child("avatars/" + uid + ".jpg");

        avatarRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> avatarRef.getDownloadUrl()
                        .addOnSuccessListener(downloadUri -> callback.onSuccess(downloadUri.toString()))
                        .addOnFailureListener(e -> callback.onError(e.getMessage())))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
}
