package com.example.cinematch.utils;

import android.content.Context;
import android.content.SharedPreferences;

// Quản lý session (uid, role) và settings (dark mode) qua SharedPreferences.
// Dùng để check "đã đăng nhập chưa" ở SplashActivity mà không cần gọi Firebase mỗi lần mở app.
public class SharedPrefManager {

    private final SharedPreferences prefs;

    public SharedPrefManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveSession(String uid, String role) {
        prefs.edit()
                .putString(Constants.PREF_KEY_UID, uid)
                .putString(Constants.PREF_KEY_ROLE, role)
                .apply();
    }

    public void clearSession() {
        prefs.edit()
                .remove(Constants.PREF_KEY_UID)
                .remove(Constants.PREF_KEY_ROLE)
                .apply();
    }

    public String getUid() {
        return prefs.getString(Constants.PREF_KEY_UID, null);
    }

    public String getRole() {
        return prefs.getString(Constants.PREF_KEY_ROLE, null);
    }

    public boolean isLoggedIn() {
        return getUid() != null;
    }

    public boolean isModerator() {
        return "moderator".equals(getRole());
    }

    // Settings
    public void setDarkMode(boolean enabled) {
        prefs.edit().putBoolean(Constants.PREF_KEY_DARK_MODE, enabled).apply();
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(Constants.PREF_KEY_DARK_MODE, false);
    }
}
