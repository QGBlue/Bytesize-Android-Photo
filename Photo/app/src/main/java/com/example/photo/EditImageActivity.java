package com.example.photo;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.photo.ZoomableImageView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import android.util.Log;
public class EditImageActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_WRITE_EXTERNAL_STORAGE = 1;

    private ZoomableImageView imageView;
    private Bitmap originalBitmap;
    private Bitmap currentBitmap;
    private Uri imageUri;

    private SeekBar seekBarAdjust;
    private View adjustmentPanel;

    private int currentAdjustMode = 0; // 0: brightness, 1: contrast
    private int currentBrightness = 0;
    private int currentContrast = 0;
    private boolean useEnhancedBrightness = true;
    //翻转
    private float currentRotation = 0;
    private boolean isFlippedHorizontal = false;
    private boolean isFlippedVertical = false;
    //裁剪
    private CropOverlayView cropOverlayView;
    private View cropPanel;
    private boolean isCropMode = false;
    private RectF currentCropRect;
    // 文字编辑
    private TextEditDialog textEditDialog;
    private DraggableTextView selectedTextView;
    private List<DraggableTextView> textViews = new ArrayList<>();
    private TextStyleDialog textStyleDialog;
    private TextTransformDialog textTransformDialog;
    private FilterDialog filterDialog;
    private FilterProcessor.FilterType currentFilter = FilterProcessor.FilterType.NONE;
    private FilterManager filterManager;
    //保存
    private static final int PERMISSION_REQUEST_SAVE_IMAGE = 102;
    private ImageSaver imageSaver;
    private AlertDialog progressDialog;
    private boolean isSaving = false;
    private Thread saveThread;
    // 贴纸编辑
    private StickerDialog stickerDialog;
    private DraggableStickerView selectedSticker;
    private List<DraggableStickerView> stickers = new ArrayList<>();
    private RelativeLayout stickerContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_image);

        // 获取图片URI
        imageUri = Uri.parse(getIntent().getStringExtra("image_uri"));

        initViews();
        loadImage();
        setupListeners();    // 初始化图片保存器
        imageSaver = new ImageSaver(this);

    }

    private void initViews() {
        // 设置工具栏
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("编辑图片");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        imageView = findViewById(R.id.imageView);
        seekBarAdjust = findViewById(R.id.seekBarAdjust);
        adjustmentPanel = findViewById(R.id.adjustmentPanel);

        // 初始化裁剪视图
        cropOverlayView = findViewById(R.id.cropOverlayView);
        cropPanel = findViewById(R.id.cropPanel);

        // 设置裁剪矩形变化监听
        cropOverlayView.setOnCropRectChangeListener(rect -> {
            currentCropRect = rect;
        });
        // 初始化文字编辑对话框
        textEditDialog = new TextEditDialog(this);

        // 设置文字容器的点击事件，用于取消选中文字
        findViewById(R.id.textContainer).setOnClickListener(v -> {
            if (selectedTextView != null) {
                selectedTextView.setSelected(false);
                selectedTextView = null;
            }
        });
        // 初始化文字样式对话框
        textStyleDialog = new TextStyleDialog(this);
        // 初始化变换对话框
        textTransformDialog = new TextTransformDialog(this);
        // 初始化滤镜对话框
        filterDialog = new FilterDialog(this);
        // 初始化贴纸容器
        stickerContainer = findViewById(R.id.stickerContainer);
        // 设置贴纸容器的点击事件，用于取消选中贴纸
        stickerContainer.setOnClickListener(v -> {
            if (selectedSticker != null) {
                selectedSticker.setSelected(false);
                selectedSticker = null;
            }
        });
        // 初始化贴纸对话框
        stickerDialog = new StickerDialog(this);
    }

    private void loadImage() {
        try {
            // 检查是否来自相机
            boolean isFromCamera = getIntent().getBooleanExtra("is_from_camera", false);
            if (isFromCamera) {
                Log.d(TAG, "加载相机拍摄的图片");
                loadCameraImage();
            } else {
                Log.d(TAG, "加载相册图片");
                loadGalleryImage();
            }
            // 确保图片加载成功后再初始化FilterManager
            if (originalBitmap != null) {
                // 初始化 FilterManager
                filterManager = new FilterManager(originalBitmap);
            } else {
                throw new IOException("图片加载失败");
            }

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "图片加载失败", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    // 统一的图片加载方法
    private void loadImageFromUri(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream == null) {
                throw new IOException("无法打开图片流");
            }
            // 先获取图片尺寸
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            // 计算合适的缩放比例
            int scale = calculateInSampleSize(options, 2048, 2048);
            // 重新打开流并加载图片
            inputStream = getContentResolver().openInputStream(imageUri);
            options.inJustDecodeBounds = false;
            options.inSampleSize = scale;
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            originalBitmap = BitmapFactory.decodeStream(inputStream, null, options);
            inputStream.close();
            if (originalBitmap != null) {
                currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                imageView.setImageBitmap(currentBitmap);
            } else {
                throw new IOException("无法解码图片");
            }
        } catch (Exception e) {
            throw new RuntimeException("加载图片失败", e);
        }
    }
    private void loadGalleryImage() {
        loadImageFromUri(imageUri);
    }
    // 添加相机图片加载方法
    private void loadCameraImage() {
        try {
            // 使用 ContentResolver 读取图片，而不是直接使用文件路径
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            if (inputStream != null) {
                // 先解码图片尺寸
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();
                // 计算合适的缩放比例
                int scale = 1;
                while ((options.outWidth / scale) > 2048 || (options.outHeight / scale) > 2048) {
                    scale *= 2;
                }
                // 重新打开流并解码图片
                inputStream = getContentResolver().openInputStream(imageUri);
                options.inJustDecodeBounds = false;
                options.inSampleSize = scale;
                originalBitmap = BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();
                if (originalBitmap != null) {
                    currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    imageView.setImageBitmap(currentBitmap);
                    // 初始化 FilterManager
                    filterManager = new FilterManager(originalBitmap);
                } else {
                    throw new IOException("无法解码图片");
                }
            } else {
                throw new IOException("无法打开图片流");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "相机图片加载失败", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    // 计算合适的缩放比例
    private int calculateInSampleSize(BitmapFactory.Options options,
                                      int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // 计算最大的inSampleSize值，该值是2的幂，
            // 且高度和宽度都大于请求的高度和宽度
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
    private void setupListeners() {

        // 裁剪按钮
        findViewById(R.id.btnCrop).setOnClickListener(v -> enterCropMode());
        // 裁剪控制按钮
        findViewById(R.id.btnCropApply).setOnClickListener(v -> applyCrop());
        findViewById(R.id.btnCropCancel).setOnClickListener(v -> exitCropMode());
        // 固定比例裁剪按钮
        findViewById(R.id.btnCropFree).setOnClickListener(v -> {
            // 自由裁剪，不改变当前裁剪框
            Toast.makeText(this, "自由裁剪模式", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.btnCrop1_1).setOnClickListener(v -> cropWithRatio(1.0f));
        findViewById(R.id.btnCrop4_3).setOnClickListener(v -> cropWithRatio(4.0f/3));
        findViewById(R.id.btnCrop16_9).setOnClickListener(v -> cropWithRatio(16.0f/9));
        findViewById(R.id.btnCrop3_4).setOnClickListener(v -> cropWithRatio(3.0f/4));
        findViewById(R.id.btnCrop9_16).setOnClickListener(v -> cropWithRatio(9.0f/16));
        // 旋转90°按钮
        findViewById(R.id.btnRotate90).setOnClickListener(v -> rotateImage(90));
        // 旋转180°按钮
        findViewById(R.id.btnRotate180).setOnClickListener(v -> rotateImage(180));
        // 水平翻转按钮
        findViewById(R.id.btnFlipHorizontal).setOnClickListener(v -> flipImage(true, false));
        // 垂直翻转按钮
        findViewById(R.id.btnFlipVertical).setOnClickListener(v -> flipImage(false, true));
        // 重置旋转和翻转按钮
        findViewById(R.id.btnResetRotation).setOnClickListener(v -> resetRotationAndFlip());
        // 亮度按钮
        findViewById(R.id.btnBrightness).setOnClickListener(v -> showBrightnessAdjustment());
        // 切换效果按钮
        findViewById(R.id.btnToggleEffect).setOnClickListener(v -> toggleBrightnessEffect());
        // 对比度按钮
        findViewById(R.id.btnContrast).setOnClickListener(v -> showContrastAdjustment());
        // 文字按钮
        findViewById(R.id.btnText).setOnClickListener(v -> showTextDialog());
        // 文字按钮
        findViewById(R.id.btnText).setOnClickListener(v -> {
            // 先显示简单的文本输入对话框
            showSimpleTextInputDialog();
        });
        // 贴纸按钮
        findViewById(R.id.btnSticker).setOnClickListener(v -> {
            showStickerDialog();
        });
        // 滤镜按钮
        findViewById(R.id.btnFilter).setOnClickListener(v -> showFilterDialog());
        findViewById(R.id.btnUndo).setOnClickListener(v -> undoFilter());
        findViewById(R.id.btnRedo).setOnClickListener(v -> redoFilter());
        // 保存按钮
        findViewById(R.id.btnSave).setOnClickListener(v -> saveImage());
        // 参数调节SeekBar
        seekBarAdjust.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    updateAdjustment(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
// ============ 贴纸相关方法 ============

    /**
     * 显示贴纸选择对话框
     */
    private void showStickerDialog() {
        try {
            // 使用简化的贴纸对话框
            if (stickerDialog == null) {
                stickerDialog = new StickerDialog(this);
            }

            stickerDialog.show(stickerItem -> {
                addStickerToImage(stickerItem);
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "打开贴纸对话框失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 贴纸添加方法
    /**
     * 添加贴纸到图片
     */
    private void addStickerToImage(StickerItem stickerItem) {
        try {
            Log.d("EditImageActivity", "开始添加贴纸: " + stickerItem.getName() + ", 资源ID: " + stickerItem.getResId());

            // 检查贴纸容器
            if (stickerContainer == null) {
                Log.e("EditImageActivity", "贴纸容器为null");
                Toast.makeText(this, "贴纸容器未初始化", Toast.LENGTH_SHORT).show();
                return;
            }

            // 创建贴纸视图
            Log.d("EditImageActivity", "创建DraggableStickerView");
            DraggableStickerView stickerView = new DraggableStickerView(this);

            // 检查贴纸资源
            int resId = stickerItem.getResId();
            if (resId == 0) {
                Log.e("EditImageActivity", "贴纸资源ID为0");
                Toast.makeText(this, "贴纸资源无效", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d("EditImageActivity", "设置贴纸资源: " + resId);
            stickerView.setStickerResource(resId);

            // 检查贴纸Bitmap是否成功加载
            if (stickerView.getStickerBitmap() == null) {
                Log.e("EditImageActivity", "贴纸Bitmap加载失败");
                Toast.makeText(this, "贴纸图片加载失败", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d("EditImageActivity", "设置布局参数");
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                    RelativeLayout.LayoutParams.WRAP_CONTENT,
                    RelativeLayout.LayoutParams.WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            stickerView.setLayoutParams(params);

            // 设置选中监听器
            Log.d("EditImageActivity", "设置选中监听器");
            stickerView.setOnStickerSelectedListener(new DraggableStickerView.OnStickerSelectedListener() {
                @Override
                public void onStickerSelected(DraggableStickerView selectedStickerView) {
                    Log.d("EditImageActivity", "贴纸被选中");
                    if (selectedSticker != null && selectedSticker != selectedStickerView) {
                        selectedSticker.setSelected(false);
                    }
                    selectedSticker = selectedStickerView;
                    selectedStickerView.setSelected(true);
                    showSimpleStickerMenu(selectedStickerView);
                }

                @Override
                public void onStickerDeleted(DraggableStickerView stickerView) {
                    Log.d("EditImageActivity", "删除贴纸");
                    removeSticker(stickerView);
                }
            });

            Log.d("EditImageActivity", "添加到贴纸容器");
            stickerContainer.addView(stickerView);
            stickerContainer.setVisibility(View.VISIBLE);
            stickers.add(stickerView);

            // 选中新添加的贴纸
            stickerView.setSelected(true);
            if (selectedSticker != null && selectedSticker != stickerView) {
                selectedSticker.setSelected(false);
            }
            selectedSticker = stickerView;

            Log.d("EditImageActivity", "贴纸添加成功");
            Toast.makeText(this, "贴纸已添加: " + stickerItem.getName(), Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            Log.e("EditImageActivity", "添加贴纸异常: " + e.getMessage(), e);
            e.printStackTrace();
            Toast.makeText(this, "添加贴纸失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 简单的贴纸菜单
    private void showSimpleStickerMenu(final DraggableStickerView stickerView) {
        new AlertDialog.Builder(this)
                .setTitle("贴纸操作")
                .setItems(new String[]{"删除", "取消"}, (dialog, which) -> {
                    if (which == 0) {
                        removeSticker(stickerView);
                    }
                })
                .show();
    }

    /**
     * 移除贴纸
     */
    private void removeSticker(DraggableStickerView stickerView) {
        stickerContainer.removeView(stickerView);
        stickers.remove(stickerView);
        stickerView.recycle();

        if (selectedSticker == stickerView) {
            selectedSticker = null;
        }

        // 如果没有贴纸了，隐藏容器
        if (stickers.isEmpty()) {
            stickerContainer.setVisibility(View.GONE);
        }

        Toast.makeText(this, "贴纸已删除", Toast.LENGTH_SHORT).show();
    }

    /**
     * 更新贴纸的Z轴顺序
     */
    private void updateStickerZOrder() {
        // 根据贴纸在列表中的顺序更新显示顺序
        for (int i = 0; i < stickers.size(); i++) {
            stickers.get(i).bringToFront();
        }
    }

    /**
     * 显示贴纸透明度对话框
     */
    private void showStickerOpacityDialog(final DraggableStickerView stickerView) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_opacity, null);
        builder.setView(dialogView);
        builder.setTitle("调整透明度");

        SeekBar opacitySeekBar = dialogView.findViewById(R.id.opacitySeekBar);
        TextView opacityValue = dialogView.findViewById(R.id.opacityValue);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnApply = dialogView.findViewById(R.id.btnApply);

        // 设置初始值（假设贴纸有透明度属性）
        opacitySeekBar.setMax(100);
        opacitySeekBar.setProgress(100);
        opacityValue.setText("100%");

        opacitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                opacityValue.setText(progress + "%");
                // 这里可以实时预览透明度效果
                stickerView.setAlpha(progress / 100f);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());
        btnApply.setOnClickListener(v -> {
            // 应用透明度
            int opacity = opacitySeekBar.getProgress();
            stickerView.setAlpha(opacity / 100f);
            Toast.makeText(this, "透明度已设置为" + opacity + "%", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        dialog.show();
    }

    /**
     * 在保存图片时绘制贴纸图层
     */
    private void drawStickerLayers(Canvas canvas) {
        if (stickerContainer.getVisibility() != View.VISIBLE || stickers.isEmpty()) {
            return;
        }

        // 计算图片显示区域
        RectF imageRect = getImageDisplayRect();

        // 计算缩放比例
        float scaleX = currentBitmap.getWidth() / imageRect.width();
        float scaleY = currentBitmap.getHeight() / imageRect.height();

        // 创建一个临时Bitmap用于绘制贴纸
        Bitmap tempBitmap = Bitmap.createBitmap(
                currentBitmap.getWidth(),
                currentBitmap.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas tempCanvas = new Canvas(tempBitmap);

        // 保存Canvas状态
        tempCanvas.save();

        // 应用缩放，将Canvas坐标系映射到图片坐标系
        tempCanvas.scale(scaleX, scaleY);

        // 应用平移，将Canvas原点移动到图片显示区域
        tempCanvas.translate(-imageRect.left, -imageRect.top);

        // 按照Z轴顺序绘制所有贴纸（列表后面的在最上面）
        for (DraggableStickerView sticker : stickers) {
            drawStickerToCanvas(tempCanvas, sticker);
        }

        tempCanvas.restore();

        // 将贴纸Bitmap绘制到最终Canvas
        canvas.drawBitmap(tempBitmap, 0, 0, null);

        // 回收临时Bitmap
        tempBitmap.recycle();
    }

    /**
     * 绘制单个贴纸到Canvas
     */
    private void drawStickerToCanvas(Canvas canvas, DraggableStickerView sticker) {
        // 保存Canvas状态
        canvas.save();

        try {
            // 获取贴纸的变换矩阵
            Matrix transformMatrix = sticker.getTransformMatrix();

            // 应用贴纸的变换矩阵
            canvas.concat(transformMatrix);

            // 获取贴纸Bitmap并绘制
            Bitmap stickerBitmap = sticker.getStickerBitmap();
            if (stickerBitmap != null) {
                // 应用贴纸的透明度
                Paint paint = new Paint();
                paint.setAlpha((int) (sticker.getAlpha() * 255));

                canvas.drawBitmap(stickerBitmap, 0, 0, paint);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 恢复Canvas状态
            canvas.restore();
        }
    }

    private void showCropDialog() {
        enterCropMode();
    }

    // 进入裁剪模式
    private void enterCropMode() {
        isCropMode = true;

        // 显示裁剪界面，隐藏其他编辑控件
        cropOverlayView.setVisibility(View.VISIBLE);
        cropPanel.setVisibility(View.VISIBLE);
        adjustmentPanel.setVisibility(View.GONE);

        // 初始化裁剪矩形（图片的80%大小，居中）
        int viewWidth = imageView.getWidth();
        int viewHeight = imageView.getHeight();

        float cropWidth = viewWidth * 0.8f;
        float cropHeight = viewHeight * 0.8f;
        float left = (viewWidth - cropWidth) / 2;
        float top = (viewHeight - cropHeight) / 2;

        RectF initialRect = new RectF(left, top, left + cropWidth, top + cropHeight);
        cropOverlayView.setCropRect(initialRect);
        currentCropRect = initialRect;

        // 禁用图片的缩放和平移手势
        imageView.setEnabled(false);
    }

    // 退出裁剪模式
    private void exitCropMode() {
        isCropMode = false;
        cropOverlayView.setVisibility(View.GONE);
        cropPanel.setVisibility(View.GONE);
        imageView.setEnabled(true);
    }

    // 应用裁剪
    private void applyCrop() {
        if (currentCropRect == null) {
            Toast.makeText(this, "请选择裁剪区域", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 将视图坐标转换为图片坐标
            RectF imageCropRect = convertViewRectToImageRect(currentCropRect);
            Bitmap croppedBitmap = CropHelper.cropBitmap(currentBitmap, imageCropRect);

            // 更新图片
            if (currentBitmap != null && currentBitmap != originalBitmap) {
                currentBitmap.recycle();
            }
            currentBitmap = croppedBitmap;
            imageView.setImageBitmap(currentBitmap);
            imageView.resetZoom();

            Toast.makeText(this, "裁剪完成", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "裁剪失败", Toast.LENGTH_SHORT).show();
        }

        exitCropMode();
    }

    // 将视图坐标转换为图片坐标
    private RectF convertViewRectToImageRect(RectF viewRect) {
        // 获取图片在ImageView中的显示区域
        float[] values = new float[9];
        imageView.getImageMatrix().getValues(values);

        float scale = values[Matrix.MSCALE_X];
        float transX = values[Matrix.MTRANS_X];
        float transY = values[Matrix.MTRANS_Y];

        // 计算图片的实际显示区域
        float imageLeft = transX;
        float imageTop = transY;
        float imageRight = imageLeft + currentBitmap.getWidth() * scale;
        float imageBottom = imageTop + currentBitmap.getHeight() * scale;

        // 将视图坐标转换为图片坐标
        float left = (viewRect.left - imageLeft) / scale;
        float top = (viewRect.top - imageTop) / scale;
        float right = (viewRect.right - imageLeft) / scale;
        float bottom = (viewRect.bottom - imageTop) / scale;

        // 确保坐标在图片范围内
        left = Math.max(0, Math.min(left, currentBitmap.getWidth()));
        top = Math.max(0, Math.min(top, currentBitmap.getHeight()));
        right = Math.max(0, Math.min(right, currentBitmap.getWidth()));
        bottom = Math.max(0, Math.min(bottom, currentBitmap.getHeight()));

        return new RectF(left, top, right, bottom);
    }

    // 按比例裁剪
    private void cropWithRatio(float ratio) {
        Bitmap croppedBitmap = CropHelper.cropBitmapWithRatio(currentBitmap, ratio);

        if (currentBitmap != null && currentBitmap != originalBitmap) {
            currentBitmap.recycle();
        }
        currentBitmap = croppedBitmap;
        imageView.setImageBitmap(currentBitmap);
        imageView.resetZoom();

        String ratioName = CropHelper.getRatioName(ratio);
        Toast.makeText(this, "已应用 " + ratioName + " 裁剪", Toast.LENGTH_SHORT).show();
        exitCropMode();
    }
    private void rotateImage(float degrees) {
        // 更新当前旋转角度
        currentRotation += degrees;

        // 确保角度在 0-360 范围内
        if (currentRotation >= 360) {
            currentRotation -= 360;
        } else if (currentRotation < 0) {
            currentRotation += 360;
        }

        // 应用旋转
        Bitmap rotatedBitmap = ImageProcessor.rotateBitmap(currentBitmap, degrees);

        // 释放之前的位图
        if (currentBitmap != null && currentBitmap != originalBitmap) {
            currentBitmap.recycle();
        }

        currentBitmap = rotatedBitmap;
        imageView.setImageBitmap(currentBitmap);

        // 显示旋转角度
        Toast.makeText(this, "已旋转 " + degrees + "°，当前角度: " + currentRotation + "°",
                Toast.LENGTH_SHORT).show();
    }
    // 实现翻转功能
    private void flipImage(boolean horizontal, boolean vertical) {
        if (horizontal) {
            isFlippedHorizontal = !isFlippedHorizontal;
        }
        if (vertical) {
            isFlippedVertical = !isFlippedVertical;
        }

        // 应用翻转
        Bitmap flippedBitmap = ImageProcessor.flipBitmap(currentBitmap, isFlippedHorizontal, isFlippedVertical);

        // 释放之前的位图
        if (currentBitmap != null && currentBitmap != originalBitmap) {
            currentBitmap.recycle();
        }

        currentBitmap = flippedBitmap;
        imageView.setImageBitmap(currentBitmap);

        // 显示翻转状态
        String message = "";
        if (horizontal && vertical) {
            message = "水平和垂直翻转";
        } else if (horizontal) {
            message = isFlippedHorizontal ? "水平翻转" : "取消水平翻转";
        } else if (vertical) {
            message = isFlippedVertical ? "垂直翻转" : "取消垂直翻转";
        }

        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // 添加重置旋转和翻转的方法
    private void resetRotationAndFlip() {
        currentRotation = 0;
        isFlippedHorizontal = false;
        isFlippedVertical = false;

        // 重新加载原始图片
        if (currentBitmap != null && currentBitmap != originalBitmap) {
            currentBitmap.recycle();
        }

        currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        imageView.setImageBitmap(currentBitmap);
        imageView.resetZoom();

        Toast.makeText(this, "已重置旋转和翻转", Toast.LENGTH_SHORT).show();
    }
    // ============ 以下是新增的亮度调节相关方法 ============

    private void showBrightnessAdjustment() {
        currentAdjustMode = 0;
        adjustmentPanel.setVisibility(View.VISIBLE);
        findViewById(R.id.adjustmentTitle).setVisibility(View.VISIBLE);
        ((android.widget.TextView) findViewById(R.id.adjustmentTitle)).setText("亮度调节");

        // 扩大亮度范围：-150 到 150
        seekBarAdjust.setMax(300);
        seekBarAdjust.setProgress(currentBrightness + 150); // 转换为 0-300 范围
        ((android.widget.TextView) findViewById(R.id.adjustmentValue)).setText(String.valueOf(currentBrightness));
    }

    // 对比度调节范围保持不变
    private void showContrastAdjustment() {
        currentAdjustMode = 1;
        adjustmentPanel.setVisibility(View.VISIBLE);
        findViewById(R.id.adjustmentTitle).setVisibility(View.VISIBLE);
        ((android.widget.TextView) findViewById(R.id.adjustmentTitle)).setText("对比度调节");

        // 对比度范围保持不变：-50 到 150
        seekBarAdjust.setMax(200);
        seekBarAdjust.setProgress(currentContrast + 50); // 转换为 0-200 范围
        ((android.widget.TextView) findViewById(R.id.adjustmentValue)).setText(String.valueOf(currentContrast));
    }

    // 更新调节值处理
    private void updateAdjustment(int progress) {
        if (currentAdjustMode == 0) {
            // 亮度调节: -150 到 150
            currentBrightness = progress - 150;
            ((android.widget.TextView) findViewById(R.id.adjustmentValue)).setText(String.valueOf(currentBrightness));
            applyBrightness(currentBrightness);
        } else {
            // 对比度调节: -50 到 150 (保持不变)
            currentContrast = progress - 50;
            ((android.widget.TextView) findViewById(R.id.adjustmentValue)).setText(String.valueOf(currentContrast));
            applyContrast(currentContrast);
        }
    }

    // 添加切换亮度效果的方法
    private void toggleBrightnessEffect() {
        useEnhancedBrightness = !useEnhancedBrightness;
        String mode = useEnhancedBrightness ? "增强亮度" : "超强亮度";
        Toast.makeText(this, "已切换到" + mode, Toast.LENGTH_SHORT).show();

        // 重新应用当前亮度设置
        if (currentAdjustMode == 0) {
            applyBrightness(currentBrightness);
        }
    }


    private void applyBrightness(int brightness) {
        Bitmap adjustedBitmap;

        if (useEnhancedBrightness) {
            // 使用增强亮度效果
            adjustedBitmap = ImageProcessor.adjustBrightnessContrast(
                    originalBitmap, brightness, currentContrast);
        } else {
            // 使用超强亮度效果
            adjustedBitmap = ImageProcessor.adjustBrightnessExtreme(
                    originalBitmap, brightness);
        }

        // 释放之前的位图
        if (currentBitmap != null && currentBitmap != originalBitmap) {
            currentBitmap.recycle();
        }

        currentBitmap = adjustedBitmap;
        imageView.setImageBitmap(currentBitmap);
    }

    private void applyContrast(int contrast) {
        // 使用标准对比度调节
        Bitmap adjustedBitmap = ImageProcessor.adjustBrightnessContrast(
                originalBitmap, currentBrightness, contrast);

        // 释放之前的位图
        if (currentBitmap != null && currentBitmap != originalBitmap) {
            currentBitmap.recycle();
        }

        currentBitmap = adjustedBitmap;
        imageView.setImageBitmap(currentBitmap);
    }


    private void showTextDialog() {
        // 显示新建文字对话框
        textEditDialog.showNewTextDialog(new TextEditDialog.OnTextChangedListener() {
            @Override
            public void onTextChanged(DraggableTextView textView) {
                addTextViewToContainer(textView);
            }

            @Override
            public void onTextRemoved() {
                if (selectedTextView != null) {
                    removeTextView(selectedTextView);
                }
            }
        });
    }
    // 添加文字视图到容器
    private void addTextViewToContainer(DraggableTextView textView) {
        RelativeLayout textContainer = findViewById(R.id.textContainer);

        // 设置布局参数
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);

        textView.setLayoutParams(params);

        // 设置选中监听器
        textView.setOnTextSelectedListener(new DraggableTextView.OnTextSelectedListener() {
            @Override
            public void onTextSelected(DraggableTextView textView) {
                // 取消之前选中的文字
                if (selectedTextView != null && selectedTextView != textView) {
                    selectedTextView.setSelected(false);
                }
                // 选中当前文字
                selectedTextView = textView;
                // 显示编辑对话框
                showEditTextDialog(textView);
            }
        });

        textContainer.addView(textView);
        textContainer.setVisibility(View.VISIBLE);
        textViews.add(textView);
    }
    // 更新文字添加方法
// 更新文字添加方法，添加变换控制点
    private void addTextToImage(String text) {
        // 创建可拖动的文字视图
        DraggableTextView textView = new DraggableTextView(this);
        textView.setText(text);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(24);
        textView.setShadowLayer(3, 1, 1, Color.BLACK);

        // 设置布局参数
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        textView.setLayoutParams(params);

        // 添加到文字容器
        RelativeLayout textContainer = findViewById(R.id.textContainer);
        textContainer.addView(textView);
        textContainer.setVisibility(View.VISIBLE);

        // 添加到文字列表
        textViews.add(textView);

        // 设置拖动、选中和变换监听
        textView.setOnTextSelectedListener(new DraggableTextView.OnTextSelectedListener() {
            @Override
            public void onTextSelected(DraggableTextView selectedTextView) {
                // 取消之前选中的文字
                if (EditImageActivity.this.selectedTextView != null &&
                        EditImageActivity.this.selectedTextView != selectedTextView) {
                    EditImageActivity.this.selectedTextView.setSelected(false);
                }

                // 选中当前文字
                EditImageActivity.this.selectedTextView = selectedTextView;

                // 显示操作菜单（样式、变换）
                showTextActionMenu(selectedTextView);
            }
        });

        Toast.makeText(this, "文字已添加，支持拖动、缩放、旋转", Toast.LENGTH_SHORT).show();
    }
    // 显示文字操作菜单
    private void showTextActionMenu(DraggableTextView textView) {
        // 创建选项菜单
        String[] options = {"编辑样式", "缩放旋转", "删除文字"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("文字操作");
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // 编辑样式
                    showTextStyleDialog(textView);
                    break;
                case 1: // 缩放旋转
                    showTextTransformDialog(textView);
                    break;
                case 2: // 删除文字
                    removeTextView(textView);
                    break;
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    // 显示文字变换对话框
    private void showTextTransformDialog(DraggableTextView textView) {
        textTransformDialog.show(textView, new TextTransformDialog.OnTransformChangedListener() {
            @Override
            public void onTransformChanged() {
                // 变换已应用
                Toast.makeText(EditImageActivity.this, "变换已应用", Toast.LENGTH_SHORT).show();
            }
        });
    }
    // 显示文字样式对话框
// 显示文字样式对话框
    private void showTextStyleDialog(DraggableTextView textView) {
        textStyleDialog.show(textView, new TextStyleDialog.OnStyleChangedListener() {
            @Override
            public void onStyleChanged(DraggableTextView textView) {
                Toast.makeText(EditImageActivity.this, "样式已更新", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 简单的文本输入对话框
    private void showSimpleTextInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_text_input, null);
        builder.setView(dialogView);

        EditText editText = dialogView.findViewById(R.id.editText);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnAdd = dialogView.findViewById(R.id.btnAdd);

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String text = editText.getText().toString().trim();
            if (!text.isEmpty()) {
                addTextToImage(text);
            }
            dialog.dismiss();
        });

        dialog.show();
    }
    // 显示文字编辑对话框
    private void showEditTextDialog(DraggableTextView textView) {
        textEditDialog.show(textView, new TextEditDialog.OnTextChangedListener() {
            @Override
            public void onTextChanged(DraggableTextView textView) {
                // 文字已更新，无需额外操作
            }

            @Override
            public void onTextRemoved() {
                removeTextView(textView);
            }
        });
    }

    // 移除文字视图
    private void removeTextView(DraggableTextView textView) {
        RelativeLayout textContainer = findViewById(R.id.textContainer);
        textContainer.removeView(textView);
        textViews.remove(textView);

        // 如果没有文字了，隐藏容器
        if (textViews.isEmpty()) {
            textContainer.setVisibility(View.GONE);
        }

        selectedTextView = null;
        Toast.makeText(this, "文字已删除", Toast.LENGTH_SHORT).show();
    }

    // 保存图片时需要合并文字和图片
    private Bitmap combineTextWithImage(Bitmap bitmap) {
        // 创建一个新的Bitmap，在上面绘制图片和文字
        // 后续在保存功能中实现
        return bitmap;
    }
    // 显示滤镜选择对话框
    private void showFilterDialog() {
        // 创建预览用的缩略图
        Bitmap previewBitmap = createPreviewBitmap();

        filterDialog.show(previewBitmap, new FilterDialog.OnFilterSelectedListener() {
            @Override
            public void onFilterSelected(FilterProcessor.FilterType filterType) {
                applyFilter(filterType);
            }
        });
    }
    // 创建预览用的缩略图
    private Bitmap createPreviewBitmap() {
        if (currentBitmap != null) {
            // 创建缩略图用于预览
            int previewSize = 400;
            return Bitmap.createScaledBitmap(
                    currentBitmap,
                    previewSize,
                    previewSize,
                    true
            );
        }
        return null;
    }
    // 应用滤镜效果
    private void applyFilter(FilterProcessor.FilterType filterType) {
        currentFilter = filterType;

        // 显示加载提示
        Toast.makeText(this, "正在应用滤镜...", Toast.LENGTH_SHORT).show();

        // 在新线程中处理滤镜（避免阻塞UI）
        new Thread(() -> {
            try {
                // 应用滤镜效果
                Bitmap filteredBitmap = FilterProcessor.applyFilter(currentBitmap, filterType);

                // 回到主线程更新UI
                runOnUiThread(() -> {
                    if (filteredBitmap != null) {
                        // 释放之前的位图
                        if (currentBitmap != null && currentBitmap != originalBitmap) {
                            currentBitmap.recycle();
                        }

                        currentBitmap = filteredBitmap;
                        imageView.setImageBitmap(currentBitmap);

                        String filterName = FilterProcessor.getFilterName(filterType);
                        Toast.makeText(EditImageActivity.this,
                                "已应用" + filterName + "滤镜",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(EditImageActivity.this,
                            "滤镜应用失败",
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    // 撤销滤镜
    private void undoFilter() {
        if (filterManager != null && filterManager.canUndo()) {
            new Thread(() -> {
                Bitmap previousBitmap = filterManager.undo();

                runOnUiThread(() -> {
                    if (previousBitmap != null) {
                        if (currentBitmap != null && currentBitmap != originalBitmap) {
                            currentBitmap.recycle();
                        }

                        currentBitmap = previousBitmap;
                        imageView.setImageBitmap(currentBitmap);
                        Toast.makeText(this, "已撤销", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        } else {
            Toast.makeText(this, "没有可撤销的操作", Toast.LENGTH_SHORT).show();
        }
    }

    // 重做滤镜
    private void redoFilter() {
        if (filterManager != null && filterManager.canRedo()) {
            new Thread(() -> {
                Bitmap nextBitmap = filterManager.redo();

                runOnUiThread(() -> {
                    if (nextBitmap != null) {
                        if (currentBitmap != null && currentBitmap != originalBitmap) {
                            currentBitmap.recycle();
                        }

                        currentBitmap = nextBitmap;
                        imageView.setImageBitmap(currentBitmap);
                        Toast.makeText(this, "已重做", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        } else {
            Toast.makeText(this, "没有可重做的操作", Toast.LENGTH_SHORT).show();
        }
    }
    private void saveImage() {
        // 检查权限
        if (checkSavePermission()) {
            performSave();
        }
    }
    // 检查保存权限
// 权限检查方法
    private boolean checkSavePermission() {
        // 检查所有需要的权限
        List<String> permissionsNeeded = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 需要 READ_MEDIA_IMAGES
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_IMAGES) !=
                    PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0-12 需要 READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) !=
                    PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }

        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsNeeded.toArray(new String[0]),
                    PERMISSION_REQUEST_SAVE_IMAGE);
            return false;
        }

        return true;
    }

    // 保存方法
    private void performSave() {
        if (isSaving) {
            Toast.makeText(this, "正在保存中，请稍候...", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示确认对话框
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("保存图片");
        builder.setMessage("是否保存当前编辑的图片到相册？");

        builder.setPositiveButton("保存", (dialog, which) -> {
            startSaveProcess();
        });

        builder.setNegativeButton("取消", null);

        builder.show();
    }
    //创建最终Bitmap（合并所有编辑效果）
    private Bitmap createFinalBitmap() {
        if (currentBitmap == null) {
            return null;
        }

        // 创建最终Bitmap
        Bitmap finalBitmap = Bitmap.createBitmap(
                currentBitmap.getWidth(),
                currentBitmap.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(finalBitmap);

        //  绘制原始图片
        canvas.drawBitmap(currentBitmap, 0, 0, null);

        // 绘制贴纸图层
        drawStickerLayers(canvas);
        // 绘制文字图层
        drawTextLayersReliable(canvas);

        // 添加水印
        finalBitmap = WatermarkUtils.addTextWatermark(finalBitmap, "训练营");

        return finalBitmap;
    }
    // 开始保存过程
    private void startSaveProcess() {
        isSaving = true;

        // 显示进度对话框（带取消按钮）
        showCancelableProgressDialog();

        // 启动保存线程
        saveThread = new Thread(() -> {
            try {
                // 合并所有图层
                Bitmap finalBitmap = createFinalBitmap();

                if (finalBitmap == null) {
                    runOnUiThread(() -> {
                        hideProgressDialog();
                        Toast.makeText(this, "无法生成最终图片", Toast.LENGTH_SHORT).show();
                        isSaving = false;
                    });
                    return;
                }

                // 添加水印
                Bitmap watermarkedBitmap = WatermarkUtils.addTextWatermark(finalBitmap, "训练营");

                // 保存图片
                Uri savedUri = imageSaver.saveImageToGallery(watermarkedBitmap, "PhotoEdit", null);

                // 回收临时Bitmap
                if (watermarkedBitmap != finalBitmap) {
                    watermarkedBitmap.recycle();
                }
                if (finalBitmap != currentBitmap && finalBitmap != originalBitmap) {
                    finalBitmap.recycle();
                }

                // 显示结果
                runOnUiThread(() -> {
                    hideProgressDialog();
                    isSaving = false;

                    if (savedUri != null) {
                        showSaveSuccessDialog(savedUri);
                    } else {
                        Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    hideProgressDialog();
                    isSaving = false;
                    Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });

        saveThread.start();
    }

    private void drawTextLayersReliable(Canvas canvas) {
        RelativeLayout textContainer = findViewById(R.id.textContainer);
        if (textContainer.getVisibility() != View.VISIBLE || textViews.isEmpty()) {
            return;
        }

        // 方法：直接截取整个文字容器，然后合并到图片上

        // 获取图片显示区域
        RectF imageRect = getImageDisplayRect();

        //  计算缩放比例
        float scaleX = currentBitmap.getWidth() / imageRect.width();
        float scaleY = currentBitmap.getHeight() / imageRect.height();

        // 创建一个与图片相同大小的临时Bitmap
        Bitmap tempBitmap = Bitmap.createBitmap(
                currentBitmap.getWidth(),
                currentBitmap.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas tempCanvas = new Canvas(tempBitmap);

        // 将文字容器绘制到临时Canvas
        drawTextContainerToCanvas(tempCanvas, scaleX, scaleY, imageRect);

        // 将临时Bitmap绘制到最终Canvas
        canvas.drawBitmap(tempBitmap, 0, 0, null);

        // 回收临时Bitmap
        tempBitmap.recycle();
    }

    private void drawTextContainerToCanvas(Canvas canvas, float scaleX, float scaleY, RectF imageRect) {
        RelativeLayout textContainer = findViewById(R.id.textContainer);

        // 保存Canvas状态
        canvas.save();

        // 应用缩放，将Canvas坐标系映射到图片坐标系
        canvas.scale(scaleX, scaleY);

        // 应用平移，将Canvas原点移动到图片显示区域
        canvas.translate(-imageRect.left, -imageRect.top);

        // 绘制所有文字视图
        for (DraggableTextView textView : textViews) {
            drawTextToCanvas(canvas, textView);
        }

        canvas.restore();
    }

    private void drawTextToCanvas(Canvas canvas, DraggableTextView textView) {
        // 获取文字在容器中的位置
        float x = textView.getX();
        float y = textView.getY();

        // 获取文字中心点
        float centerX = x + textView.getWidth() / 2f;
        float centerY = y + textView.getHeight() / 2f;

        // 保存Canvas状态
        canvas.save();

        try {
            // 移动到文字中心点
            canvas.translate(centerX, centerY);

            // 应用缩放
            canvas.scale(textView.getScaleFactor(), textView.getScaleFactor());

            // 应用旋转
            canvas.rotate(textView.getRotationAngle());

            // 获取文字Bitmap（不包含变换）
            Bitmap textBitmap = createTextBitmapWithoutTransform(textView);
            if (textBitmap != null) {
                // 绘制文字（中心对齐）
                canvas.drawBitmap(textBitmap,
                        -textBitmap.getWidth() / 2f,
                        -textBitmap.getHeight() / 2f,
                        null);

                textBitmap.recycle();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            canvas.restore();
        }
    }

    /**
     * 创建不包含变换的文字Bitmap
     */
    private Bitmap createTextBitmapWithoutTransform(DraggableTextView textView) {
        try {
            // 禁用文字视图的变换
            textView.setPivotX(0);
            textView.setPivotY(0);
            textView.setScaleX(1);
            textView.setScaleY(1);
            textView.setRotation(0);
            textView.setTranslationX(0);
            textView.setTranslationY(0);

            // 创建Bitmap
            Bitmap bitmap = Bitmap.createBitmap(
                    textView.getWidth(),
                    textView.getHeight(),
                    Bitmap.Config.ARGB_8888
            );

            Canvas canvas = new Canvas(bitmap);

            // 绘制文字
            textView.draw(canvas);

            return bitmap;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            // 恢复文字视图的变换
            textView.invalidate();
        }
    }
    // 显示可取消的进度对话框
    private void showCancelableProgressDialog() {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_progress_cancelable, null);
            builder.setView(dialogView);
            builder.setCancelable(false);

            TextView messageText = dialogView.findViewById(R.id.progressMessage);
            messageText.setText("正在保存图片...");

            Button cancelButton = dialogView.findViewById(R.id.btnCancel);

            progressDialog = builder.create();

            cancelButton.setOnClickListener(v -> {
                if (saveThread != null && saveThread.isAlive()) {
                    saveThread.interrupt();
                }
                isSaving = false;
                progressDialog.dismiss();
                Toast.makeText(this, "保存已取消", Toast.LENGTH_SHORT).show();
            });

            progressDialog.show();
        });
    }
    // 绘制文字图层到Canvas
    private void drawTextLayers(Canvas canvas) {
        RelativeLayout textContainer = findViewById(R.id.textContainer);
        if (textContainer.getVisibility() != View.VISIBLE || textViews.isEmpty()) {
            return;
        }

        // 计算图片在ImageView中的显示区域
        RectF imageRect = getImageDisplayRect();

        // 遍历所有文字视图
        for (DraggableTextView textView : textViews) {
            drawTextView(canvas, textView, imageRect);
        }
    }

    /**
     * 获取图片在ImageView中的实际显示区域
     */
    private RectF getImageDisplayRect() {
        // 获取ImageView的矩阵
        Matrix imageMatrix = imageView.getImageMatrix();

        // 创建图片的边界
        RectF imageRect = new RectF(0, 0,
                currentBitmap.getWidth(),
                currentBitmap.getHeight());

        // 应用ImageView的变换矩阵
        imageMatrix.mapRect(imageRect);

        return imageRect;
    }

    /**
     * 绘制单个文字视图到Canvas
     */
    private void drawTextView(Canvas canvas, DraggableTextView textView, RectF imageRect) {
        // 保存Canvas状态
        canvas.save();

        try {
            // 获取文字在容器中的位置
            int[] location = new int[2];
            textView.getLocationOnScreen(location);

            // 获取文字容器在屏幕中的位置
            int[] containerLocation = new int[2];
            RelativeLayout textContainer = findViewById(R.id.textContainer);
            textContainer.getLocationOnScreen(containerLocation);

            // 计算文字相对于容器的位置
            float relativeX = location[0] - containerLocation[0];
            float relativeY = location[1] - containerLocation[1];

            // 获取图片在屏幕中的位置
            int[] imageViewLocation = new int[2];
            imageView.getLocationOnScreen(imageViewLocation);

            // 计算文字相对于图片的位置
            float imageX = relativeX - imageViewLocation[0] + containerLocation[0];
            float imageY = relativeY - imageViewLocation[1] + containerLocation[1];

            // 考虑图片的缩放和平移
            float scaleX = currentBitmap.getWidth() / imageRect.width();
            float scaleY = currentBitmap.getHeight() / imageRect.height();

            // 转换为图片像素坐标
            float finalX = (imageX - imageRect.left) * scaleX;
            float finalY = (imageY - imageRect.top) * scaleY;

            // 应用文字自身的变换（缩放、旋转）
            canvas.translate(finalX, finalY);
            canvas.scale(textView.getScaleFactor(), textView.getScaleFactor());
            canvas.rotate(textView.getRotationAngle());

            // 创建文字的Bitmap
            Bitmap textBitmap = createTextBitmap(textView);
            if (textBitmap != null) {
                // 调整绘制位置，使文字中心对齐
                canvas.drawBitmap(textBitmap,
                        -textBitmap.getWidth() / 2f,
                        -textBitmap.getHeight() / 2f,
                        null);

                textBitmap.recycle();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 恢复Canvas状态
            canvas.restore();
        }
    }

    /**
     * 创建文字的Bitmap
     */
    private Bitmap createTextBitmap(DraggableTextView textView) {
        try {
            // 创建文字的Bitmap
            Bitmap bitmap = Bitmap.createBitmap(
                    textView.getWidth(),
                    textView.getHeight(),
                    Bitmap.Config.ARGB_8888
            );

            Canvas canvas = new Canvas(bitmap);

            // 绘制文字
            textView.draw(canvas);

            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void showProgressDialog(String message) {
        runOnUiThread(() -> {
            // 创建自定义的进度对话框
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_progress, null);

            TextView messageText = dialogView.findViewById(R.id.progressMessage);
            if (messageText != null) {
                messageText.setText(message);
            }

            builder.setView(dialogView);
            builder.setCancelable(false);

            progressDialog = builder.create();
            progressDialog.show();
        });
    }



    // 隐藏进度对话框
    private void hideProgressDialog() {
        runOnUiThread(() -> {
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        });
    }

    // 显示保存成功对话框
    private void showSaveSuccessDialog(Uri imageUri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("保存成功");
        builder.setMessage("图片已保存到相册\n\n是否立即查看？");

        builder.setPositiveButton("查看图片", (dialog, which) -> {
            openImageInGallery(imageUri);
        });

        builder.setNegativeButton("继续编辑", (dialog, which) -> {
            dialog.dismiss();
        });

        builder.setNeutralButton("分享", (dialog, which) -> {
            shareImage(imageUri);
        });

        builder.show();
    }
    // 在图库中打开图片
// 查看图片方法
    private void openImageInGallery(Uri imageUri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);

            // 设置数据类型
            intent.setDataAndType(imageUri, "image/*");

            // 添加读取权限
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // 检查是否有应用可以处理这个Intent
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
            } else {
                Toast.makeText(this, "没有可用的图片查看应用", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "无法打开图片: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    // 分享图片
    private void shareImage(Uri imageUri) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);

            // 设置分享类型
            shareIntent.setType("image/jpeg");

            // 添加图片URI
            shareIntent.putExtra(Intent.EXTRA_STREAM, imageUri);

            // 添加读取权限
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // 创建选择器标题
            String title = "分享图片";

            // 启动分享
            Intent chooser = Intent.createChooser(shareIntent, title);

            // 确保有应用可以处理分享
            if (shareIntent.resolveActivity(getPackageManager()) != null) {
                startActivity(chooser);
            } else {
                Toast.makeText(this, "没有可用的分享应用", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "分享失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // 更新权限请求结果处理
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_SAVE_IMAGE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                // 权限授予，执行保存
                startSaveProcess();
            } else {
                Toast.makeText(this, "需要存储权限来保存图片", Toast.LENGTH_LONG).show();
                // 显示详细的权限说明
                showPermissionExplanation();
            }
        }
    }
    // 显示权限说明
    private void showPermissionExplanation() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("需要权限");
        builder.setMessage("保存图片需要存储权限，用于将编辑后的图片保存到相册。\n\n请前往设置授予权限。");

        builder.setPositiveButton("去设置", (dialog, which) -> {
            try {
                Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放位图资源
        if (originalBitmap != null && !originalBitmap.isRecycled()) {
            originalBitmap.recycle();
        }
        if (currentBitmap != null && !currentBitmap.isRecycled() && currentBitmap != originalBitmap) {
            currentBitmap.recycle();
        }
        // 释放贴纸资源
        for (DraggableStickerView sticker : stickers) {
            sticker.recycle();
        }
        stickers.clear();
        if (filterManager != null) {
            filterManager.cleanup();
        }
    }
}