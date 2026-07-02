package com.example.cinematch.ui.splash;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cinematch.R;
import com.example.cinematch.firebase.AuthManager;
import com.example.cinematch.ui.auth.LoginActivity;
import com.example.cinematch.ui.main.MainActivity;

// Kiểm tra session (Firebase Auth) rồi điều hướng: đã đăng nhập -> MainActivity, chưa -> LoginActivity.
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        AuthManager authManager = new AuthManager(this);

        // Delay ngắn để hiện logo, đồng thời đủ thời gian Firebase Auth restore session
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = authManager.isLoggedIn()
                    ? new Intent(this, MainActivity.class)
                    : new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }, 1200);
    }
}
