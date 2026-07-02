package com.example.cinematch.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Wrapper đơn giản cho Handler + ExecutorService, dùng thay AsyncTask (đã deprecated).
// Đáp ứng yêu cầu "Multi-threading: Handler hoặc AsyncTask" của môn học.
// Dùng cho các tác vụ nặng chạy nền: truy vấn SQLite, tính toán genre weight, cache ảnh...
public class ThreadUtils {

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface Task<T> {
        T doInBackground(); // chạy trên background thread
    }

    public interface Callback<T> {
        void onResult(T result); // chạy lại trên UI thread
    }

    // Chạy task nền rồi trả kết quả về UI thread qua Handler.post()
    public static <T> void runInBackground(Task<T> task, Callback<T> callback) {
        executor.execute(() -> {
            final T result = task.doInBackground();
            mainHandler.post(() -> callback.onResult(result));
        });
    }

    // Dùng khi không cần trả kết quả (fire-and-forget), vd: cacheMovie()
    public static void runInBackground(Runnable backgroundWork) {
        executor.execute(backgroundWork);
    }
}
