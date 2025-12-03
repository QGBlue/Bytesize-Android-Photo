package com.example.photo;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private static final String TAG = "CameraActivity";
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    // CameraX 组件
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private CameraSelector cameraSelector;
    private Camera camera;

    // UI组件
    private ImageButton btnCapture;
    private ImageButton btnSwitchCamera;
    private ImageButton btnClose;

    // 权限
    private boolean hasCameraPermission = false;
    private boolean hasStoragePermission = false;

    // 线程池
    private ExecutorService cameraExecutor;

    // 文件保存路径
    private String currentPhotoPath;

    // 当前摄像头方向（0: 后置, 1: 前置）
    private int currentCamera = CameraSelector.LENS_FACING_BACK;

    // 是否正在拍照
    private boolean isTakingPhoto = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 防止系统相机选择器弹出
//        if (getIntent() != null && Intent.ACTION_MAIN.equals(getIntent().getAction())) {
//            finish();
//            return;
//        }
        setContentView(R.layout.activity_camera);



        // 初始化视图
        initViews();

        // 初始化线程池
        cameraExecutor = Executors.newSingleThreadExecutor();

        // 检查权限
        checkPermissions();
    }

    private void initViews() {
        previewView = findViewById(R.id.cameraPreview);
        btnCapture = findViewById(R.id.btnCapture);
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera);
        btnClose = findViewById(R.id.btnClose);

        // 设置按钮点击事件
        setupListeners();
    }

    private void setupListeners() {
        // 关闭按钮
        btnClose.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // 切换摄像头按钮
        btnSwitchCamera.setOnClickListener(v -> {
            if (!isTakingPhoto) {
                switchCamera();
            }
        });

        // 拍照按钮
        btnCapture.setOnClickListener(v -> {
            if (!isTakingPhoto) {
                takePhotoWithAnimation();
            }
        });

        // 长按拍照按钮提示
        btnCapture.setOnLongClickListener(v -> {
            Toast.makeText(this, "轻点拍照", Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    /**
     * 检查权限
     */
    private void checkPermissions() {
        String[] permissionsNeeded;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 需要相机和媒体权限
            permissionsNeeded = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
            };
        } else {
            // Android 12及以下需要相机和存储权限
            permissionsNeeded = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }

        List<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissionsNeeded) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        if (permissionsToRequest.isEmpty()) {
            // 所有权限都已授予
            hasCameraPermission = true;
            hasStoragePermission = true;
            startCamera();
        } else {
            // 请求权限
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                hasCameraPermission = true;
                hasStoragePermission = true;
                startCamera();
            } else {
                // 权限被拒绝
                Toast.makeText(this, "需要相机和存储权限才能使用拍照功能",
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    /**
     * 启动相机
     */
    private void startCamera() {
        if (!hasCameraPermission || !hasStoragePermission) {
            return;
        }

        // 显示加载提示
        Toast.makeText(this, "正在启动相机...", Toast.LENGTH_SHORT).show();

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 创建预览
                Preview preview = new Preview.Builder()
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // 创建图像捕获
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(previewView.getDisplay().getRotation())
                        .build();

                // 选择摄像头（默认后置）
                cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(currentCamera)
                        .build();

                // 解绑所有用例
                cameraProvider.unbindAll();

                // 绑定用例到生命周期
                camera = cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageCapture
                );

                // 隐藏加载提示
                Toast.makeText(CameraActivity.this, "相机已就绪", Toast.LENGTH_SHORT).show();

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "启动相机失败: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(CameraActivity.this,
                            "无法启动相机: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * 切换摄像头
     */
    private void switchCamera() {
        if (camera == null) {
            return;
        }

        // 切换摄像头方向
        if (currentCamera == CameraSelector.LENS_FACING_BACK) {
            currentCamera = CameraSelector.LENS_FACING_FRONT;
        } else {
            currentCamera = CameraSelector.LENS_FACING_BACK;
        }

        // 显示切换提示
        String cameraType = (currentCamera == CameraSelector.LENS_FACING_BACK)
                ? "后置摄像头" : "前置摄像头";
        Toast.makeText(this, "切换到" + cameraType, Toast.LENGTH_SHORT).show();

        // 重新启动相机
        startCamera();
    }

    /**
     * 带动画的拍照
     */
    private void takePhotoWithAnimation() {
        if (isTakingPhoto) {
            return;
        }

        isTakingPhoto = true;

        // 添加拍照动画
        Animation scaleAnimation = AnimationUtils.loadAnimation(this, R.anim.camera_click);
        btnCapture.startAnimation(scaleAnimation);

        // 添加快门音效（可选）
        try {
            android.media.MediaActionSound sound = new android.media.MediaActionSound();
            sound.play(android.media.MediaActionSound.SHUTTER_CLICK);
        } catch (Exception e) {
            Log.d(TAG, "无法播放快门音效: " + e.getMessage());
        }

        // 延迟执行实际拍照，让动画完成
        btnCapture.postDelayed(() -> {
            takePhoto();
        }, 100);
    }

    /**
     * 拍照
     */
    private void takePhoto() {
        if (imageCapture == null) {
            Toast.makeText(this, "相机未就绪", Toast.LENGTH_SHORT).show();
            isTakingPhoto = false;
            return;
        }

        // 创建照片文件
        File photoFile = createPhotoFile();
        if (photoFile == null) {
            Toast.makeText(this, "无法创建照片文件", Toast.LENGTH_SHORT).show();
            isTakingPhoto = false;
            return;
        }

        // 创建输出选项
        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        // 显示拍照提示
        Toast.makeText(this, "拍照中...", Toast.LENGTH_SHORT).show();

        // 执行拍照
        imageCapture.takePicture(
                outputFileOptions,
                cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        // 拍照成功
                        currentPhotoPath = photoFile.getAbsolutePath();
                        Log.d(TAG, "照片已保存: " + currentPhotoPath);

                        runOnUiThread(() -> {
                            isTakingPhoto = false;

                            // 发送广播通知系统更新图库
                            sendBroadcast(new Intent(
                                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                    Uri.fromFile(photoFile)
                            ));

                            // 进入编辑界面
                            openEditActivity(photoFile);
                        });
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "拍照失败: " + exception.getMessage());
                        runOnUiThread(() -> {
                            isTakingPhoto = false;
                            Toast.makeText(CameraActivity.this,
                                    "拍照失败: " + exception.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                }
        );
    }

    /**
     * 创建照片文件
     */
    private File createPhotoFile() {
        try {
            // 创建时间戳文件名
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                    .format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";

            // 获取存储目录
            File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            if (storageDir == null) {
                storageDir = getFilesDir();
            }

            // 确保目录存在
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            // 创建文件
            File imageFile = File.createTempFile(
                    imageFileName,  /* 前缀 */
                    ".jpg",         /* 后缀 */
                    storageDir      /* 目录 */
            );

            return imageFile;

        } catch (Exception e) {
            Log.e(TAG, "创建文件失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 打开编辑界面
     */
    private void openEditActivity(File photoFile) {
        if (photoFile == null || !photoFile.exists()) {
            Toast.makeText(this, "照片文件不存在", Toast.LENGTH_SHORT).show();
            isTakingPhoto = false;
            return;
        }

        try {
            // 使用FileProvider生成安全的URI
            Uri photoUri = FileProvider.getUriForFile(this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    photoFile);

            // 启动编辑界面
            Intent intent = new Intent(this, EditImageActivity.class);
            intent.putExtra("image_uri", photoUri.toString());
            intent.putExtra("is_from_camera", true);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // 添加摄像头方向信息（重要！）
            intent.putExtra("is_front_camera", currentCamera == CameraSelector.LENS_FACING_FRONT);

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            // 添加过渡动画
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish(); // 关闭相机界面

        } catch (Exception e) {
            Log.e(TAG, "打开编辑界面失败: " + e.getMessage());
            Toast.makeText(this, "无法打开编辑界面", Toast.LENGTH_SHORT).show();
            isTakingPhoto = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 如果权限已授予，确保相机正在运行
        if (hasCameraPermission && hasStoragePermission && camera == null) {
            startCamera();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 重置拍照状态
        isTakingPhoto = false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}