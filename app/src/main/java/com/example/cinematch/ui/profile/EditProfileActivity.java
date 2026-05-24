package com.example.cinematch.ui.profile;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.cinematch.R;
import com.example.cinematch.firebase.FirestoreManager;
import com.example.cinematch.firebase.StorageManager;
import com.example.cinematch.models.User;
import com.example.cinematch.utils.SharedPrefManager;

import java.util.Arrays;

// Cho phép user cập nhật tên, giới tính, tuổi, số điện thoại và đổi ảnh đại diện
// (chọn ảnh từ bộ sưu tập -> upload Firebase Storage -> lưu downloadUrl vào Firestore).
public class EditProfileActivity extends AppCompatActivity {

    private ImageView imgAvatarPreview;
    private EditText edtDisplayName, edtAge, edtPhone;
    private Spinner spinnerGender;
    private TextView tvEmailReadonly;
    private ProgressBar progressBar;
    private Button btnSave;

    private FirestoreManager firestoreManager;
    private StorageManager storageManager;
    private SharedPrefManager prefManager;

    private User currentUser;
    private Uri pickedImageUri; // ảnh mới chọn từ bộ sưu tập, null nếu không đổi ảnh

    private static final String[] GENDER_OPTIONS = {"Chưa chọn", "Nam", "Nữ", "Khác"};

    // Đăng ký launcher mở bộ sưu tập ảnh hệ thống -> không cần xin quyền runtime (system picker)
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    pickedImageUri = uri;
                    Glide.with(this).load(uri).circleCrop().into(imgAvatarPreview);
                }
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        firestoreManager = new FirestoreManager();
        storageManager = new StorageManager();
        prefManager = new SharedPrefManager(this);

        bindViews();
        setupGenderSpinner();
        loadCurrentUser();

        imgAvatarPreview.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        findViewById(R.id.tvChangeAvatarHint).setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void bindViews() {
        imgAvatarPreview = findViewById(R.id.imgAvatarPreview);
        edtDisplayName = findViewById(R.id.edtDisplayName);
        edtAge = findViewById(R.id.edtAge);
        edtPhone = findViewById(R.id.edtPhone);
        spinnerGender = findViewById(R.id.spinnerGender);
        tvEmailReadonly = findViewById(R.id.tvEmailReadonly);
        progressBar = findViewById(R.id.progressBar);
        btnSave = findViewById(R.id.btnSave);
    }

    private void setupGenderSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, Arrays.asList(GENDER_OPTIONS));
        spinnerGender.setAdapter(adapter);
    }

    private void loadCurrentUser() {
        String uid = prefManager.getUid();
        if (uid == null) return;

        firestoreManager.getUserProfile(uid, new FirestoreManager.UserCallback() {
            @Override
            public void onSuccess(User user) {
                currentUser = user;
                edtDisplayName.setText(user.getDisplayName());
                tvEmailReadonly.setText(user.getEmail());
                if (user.getAge() > 0) edtAge.setText(String.valueOf(user.getAge()));
                if (user.getPhone() != null) edtPhone.setText(user.getPhone());

                if (user.getGender() != null) {
                    int index = Arrays.asList(GENDER_OPTIONS).indexOf(user.getGender());
                    if (index >= 0) spinnerGender.setSelection(index);
                }

                if (user.getAvatarUrl() != null) {
                    Glide.with(EditProfileActivity.this).load(user.getAvatarUrl())
                            .circleCrop().placeholder(R.drawable.bg_avatar_circle).into(imgAvatarPreview);
                }
            }

            @Override
            public void onError(String message) {
                Toast.makeText(EditProfileActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
        if (currentUser == null) return;

        String name = edtDisplayName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Tên hiển thị không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoading(true);

        // Nếu user vừa chọn ảnh mới -> upload trước, xong mới lưu Firestore.
        // Nếu không đổi ảnh -> lưu thẳng, giữ nguyên avatarUrl cũ.
        if (pickedImageUri != null) {
            storageManager.uploadAvatar(pickedImageUri, currentUser.getUid(), new StorageManager.UploadCallback() {
                @Override
                public void onSuccess(String downloadUrl) {
                    currentUser.setAvatarUrl(downloadUrl);
                    persistProfileFields();
                }

                @Override
                public void onError(String message) {
                    setLoading(false);
                    Toast.makeText(EditProfileActivity.this, "Upload ảnh lỗi: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            persistProfileFields();
        }
    }

    private void persistProfileFields() {
        currentUser.setDisplayName(edtDisplayName.getText().toString().trim());

        String genderSelected = (String) spinnerGender.getSelectedItem();
        currentUser.setGender("Chưa chọn".equals(genderSelected) ? null : genderSelected);

        String ageStr = edtAge.getText().toString().trim();
        currentUser.setAge(ageStr.isEmpty() ? 0 : Integer.parseInt(ageStr));

        currentUser.setPhone(edtPhone.getText().toString().trim());

        firestoreManager.updateUserProfile(currentUser, new FirestoreManager.SimpleCallback() {
            @Override
            public void onSuccess() {
                setLoading(false);
                Toast.makeText(EditProfileActivity.this, "Đã lưu hồ sơ", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(EditProfileActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
    }
}
