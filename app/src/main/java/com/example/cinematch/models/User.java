package com.example.cinematch.models;

// Model tương ứng document trong collection "users" trên Firestore.
// KHÔNG dùng @SerializedName (đó là của Gson/Retrofit) vì Firestore tự map field
// qua getter/setter theo tên field, chỉ cần constructor rỗng public.
public class User {

    public static final String ROLE_USER = "user";
    public static final String ROLE_MODERATOR = "moderator";

    private String uid;
    private String email;
    private String displayName;
    private String role;      // "user" | "moderator"
    private String avatarUrl;
    private long createdAt;

    public User() { } // bắt buộc cho Firestore.toObject()

    public User(String uid, String email, String displayName) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.role = ROLE_USER; // mặc định user thường khi đăng ký
        this.createdAt = System.currentTimeMillis();
    }

    public String getUid() { return uid; }
    public String getEmail() { return email; }
    public String getDisplayName() { return displayName; }
    public String getRole() { return role; }
    public String getAvatarUrl() { return avatarUrl; }
    public long getCreatedAt() { return createdAt; }

    public void setUid(String uid) { this.uid = uid; }
    public void setEmail(String email) { this.email = email; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setRole(String role) { this.role = role; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isModerator() {
        return ROLE_MODERATOR.equals(role);
    }
}
