package com.example.testmap;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.io.IOException;

public class ServiceDetailActivity extends AppCompatActivity {

    private EditText etDescription;
    private TextView tvWordCount;
    private Button btnClose;
    private Button btnBookService;
    private Button btnUpload;
    private ImageView ivImagePlaceholder;
    private static final int STORAGE_PERMISSION_CODE = 100;
    private static final int REQUEST_PERMISSION_SETTING = 101;
    private Uri selectedMediaUri;
    private boolean permissionDeniedOnce = false;

    // ActivityResultLauncher để xử lý kết quả chọn tệp
    private final ActivityResultLauncher<Intent> mediaPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedMediaUri = result.getData().getData();
                    if (selectedMediaUri != null) {
                        try {
                            // Kiểm tra loại tệp (hình ảnh hay video)
                            String mimeType = getContentResolver().getType(selectedMediaUri);
                            if (mimeType != null) {
                                if (mimeType.startsWith("image")) {
                                    // Hiển thị hình ảnh
                                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedMediaUri);
                                    ivImagePlaceholder.setImageBitmap(bitmap);
                                } else if (mimeType.startsWith("video")) {
                                    // Hiển thị thumbnail của video
                                    Bitmap thumbnail;
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                        thumbnail = getContentResolver().loadThumbnail(selectedMediaUri, new Size(512, 512), null);
                                    } else {
                                        thumbnail = MediaStore.Video.Thumbnails.getThumbnail(
                                                getContentResolver(),
                                                ContentUris.parseId(selectedMediaUri),
                                                MediaStore.Video.Thumbnails.MINI_KIND,
                                                null);
                                    }
                                    ivImagePlaceholder.setImageBitmap(thumbnail);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Không thể tải tệp", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_service_detail);

        // Ẩn ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Ẩn thanh trạng thái và thanh điều hướng
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        decorView.setSystemUiVisibility(uiOptions);

        // Ánh xạ các thành phần giao diện
        etDescription = findViewById(R.id.etDescription);
        tvWordCount = findViewById(R.id.tvWordCount);
        btnClose = findViewById(R.id.btnClose);
        btnBookService = findViewById(R.id.btnBookService);
        btnUpload = findViewById(R.id.btnUpload);
        ivImagePlaceholder = findViewById(R.id.ivImagePlaceholder);

        // Nhận tên dịch vụ từ Intent
        String serviceName = getIntent().getStringExtra("SERVICE_NAME");
        if (serviceName != null) {
            etDescription.setHint("Nhập mô tả cho dịch vụ " + serviceName);
        }

        // Cập nhật số từ khi người dùng nhập
        etDescription.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = s.toString();
                String[] words = text.trim().isEmpty() ? new String[0] : text.trim().split("\\s+");
                int wordCount = words.length;
                tvWordCount.setText(wordCount + "/1000");

                // Giới hạn 1000 từ
                if (wordCount > 1000) {
                    Toast.makeText(ServiceDetailActivity.this, "Mô tả không được vượt quá 1000 từ!", Toast.LENGTH_SHORT).show();
                    String[] limitedWords = new String[1000];
                    System.arraycopy(words, 0, limitedWords, 0, 1000);
                    etDescription.setText(String.join(" ", limitedWords));
                    etDescription.setSelection(etDescription.getText().length());
                    tvWordCount.setText("1000/1000");
                }
            }
        });

        // Xử lý sự kiện nhấn vào nút Tải lên
        btnUpload.setOnClickListener(v -> {
            // Kiểm tra quyền truy cập bộ nhớ
            if (checkStoragePermission()) {
                openMediaPicker();
            } else {
                // Yêu cầu quyền trực tiếp mà không hiển thị dialog
                requestStoragePermission();
            }
        });

        // Xử lý sự kiện cho nút X (đóng Activity)
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Xử lý sự kiện cho nút Đặt dịch vụ
        btnBookService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String description = etDescription.getText().toString();
                String mediaInfo = selectedMediaUri != null ? "Có tệp đính kèm: " + selectedMediaUri.toString() : "Không có tệp đính kèm";

                // Chuyển sang RescueFindingActivity
                Intent intent = new Intent(ServiceDetailActivity.this, RescureFindingActivity.class);
                intent.putExtra("SERVICE_NAME", serviceName);
                intent.putExtra("DESCRIPTION", description);
                intent.putExtra("MEDIA_INFO", mediaInfo);
                startActivity(intent);
            }
        });
    }

    // Kiểm tra quyền truy cập bộ nhớ
    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            // Kiểm tra quyền READ_MEDIA_IMAGES và READ_MEDIA_VIDEO
            boolean imagesPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
            boolean videosPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
            return imagesPermission && videosPermission;
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6.0 - 12
            // Kiểm tra quyền READ_EXTERNAL_STORAGE
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
        return true; // Dưới Android 6.0, không cần kiểm tra quyền
    }

    // Yêu cầu quyền truy cập bộ nhớ
    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            // Yêu cầu quyền READ_MEDIA_IMAGES và READ_MEDIA_VIDEO
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_IMAGES) ||
                    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_MEDIA_VIDEO)) {
                Toast.makeText(this, "Ứng dụng cần quyền truy cập hình ảnh và video để tải tệp.", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO},
                        STORAGE_PERMISSION_CODE);
            } else if (permissionDeniedOnce) {
                // Người dùng đã từ chối vĩnh viễn, hướng dẫn mở cài đặt
                Toast.makeText(this, "Quyền truy cập đã bị từ chối vĩnh viễn. Vui lòng cấp quyền trong cài đặt.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, REQUEST_PERMISSION_SETTING);
            } else {
                // Yêu cầu quyền lần đầu
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO},
                        STORAGE_PERMISSION_CODE);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // Android 6.0 - 12
            // Yêu cầu quyền READ_EXTERNAL_STORAGE
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                Toast.makeText(this, "Ứng dụng cần quyền truy cập bộ nhớ để tải hình ảnh hoặc video.", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
            } else if (permissionDeniedOnce) {
                Toast.makeText(this, "Quyền truy cập bộ nhớ đã bị từ chối vĩnh viễn. Vui lòng cấp quyền trong cài đặt.", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivityForResult(intent, REQUEST_PERMISSION_SETTING);
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        STORAGE_PERMISSION_CODE);
            }
        }
    }

    // Xử lý kết quả yêu cầu quyền
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (checkStoragePermission()) {
                openMediaPicker();
            } else {
                permissionDeniedOnce = true;
                Toast.makeText(this, "Quyền truy cập bị từ chối!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Xử lý kết quả sau khi người dùng quay lại từ cài đặt
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_PERMISSION_SETTING) {
            if (checkStoragePermission()) {
                openMediaPicker();
            } else {
                Toast.makeText(this, "Quyền truy cập vẫn bị từ chối!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Mở trình chọn tệp (hình ảnh hoặc video)
    private void openMediaPicker() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+
            // Sử dụng Intent.ACTION_OPEN_DOCUMENT để hỗ trợ Selected Photos Access
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String[] mimeTypes = {"image/*", "video/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false); // Chỉ chọn 1 tệp
        } else {
            // Sử dụng Intent.ACTION_OPEN_DOCUMENT cho Android 13 trở xuống (cũng hỗ trợ tốt hơn trên các phiên bản cũ)
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            String[] mimeTypes = {"image/*", "video/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        }
        mediaPickerLauncher.launch(intent);
    }
}