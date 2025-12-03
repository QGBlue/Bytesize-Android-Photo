package com.example.photo;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageSaver {

    private static final String TAG = "ImageSaver";
    private Context context;

    public ImageSaver(Context context) {
        this.context = context;
    }

    /**
     * 保存图片到相册（兼容Android 10+）
     */
    public Uri saveImageToGallery(Bitmap bitmap, String folderName, String fileName) {
        if (bitmap == null) {
            Log.e(TAG, "saveImageToGallery: bitmap is null");
            return null;
        }

        Uri imageUri = null;

        try {
            // 使用MediaStore API保存到公共目录
            imageUri = saveToMediaStore(bitmap, folderName, fileName);

            return imageUri;

        } catch (Exception e) {
            Log.e(TAG, "保存图片失败: " + e.getMessage());
            e.printStackTrace();

            // 如果MediaStore失败，尝试使用FileProvider保存到应用私有目录
            try {
                return saveToPrivateStorage(bitmap, fileName);
            } catch (Exception ex) {
                Log.e(TAG, "私有存储保存失败: " + ex.getMessage());
                return null;
            }
        }
    }

    /**
     * 使用MediaStore保存到公共目录（Android 10+）
     */
    private Uri saveToMediaStore(Bitmap bitmap, String folderName, String fileName) throws IOException {
        ContentResolver resolver = context.getContentResolver();

        // 设置图片信息
        ContentValues contentValues = new ContentValues();

        // 生成文件名
        String displayName = (fileName != null) ? fileName : generateFileName();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        // 设置保存位置
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用RELATIVE_PATH
            String relativePath = Environment.DIRECTORY_PICTURES;
            if (folderName != null && !folderName.isEmpty()) {
                relativePath += File.separator + folderName;
            }
            contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);
        }

        // 插入记录获取URI
        Uri imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        if (imageUri == null) {
            throw new IOException("创建文件URI失败");
        }

        // 保存图片数据
        OutputStream outputStream = resolver.openOutputStream(imageUri);
        if (outputStream == null) {
            throw new IOException("无法打开输出流");
        }

        boolean saved = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
        outputStream.close();

        if (!saved) {
            throw new IOException("图片保存失败");
        }

        Log.d(TAG, "图片已保存到MediaStore: " + imageUri);
        return imageUri;
    }

    /**
     * 保存到应用私有目录（使用FileProvider共享）
     */
    private Uri saveToPrivateStorage(Bitmap bitmap, String fileName) throws IOException {
        // 创建缓存目录
        File cacheDir = new File(context.getExternalCacheDir(), "images");
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            throw new IOException("无法创建缓存目录");
        }

        // 生成文件名
        String imageName = (fileName != null) ? fileName : generateFileName();
        File imageFile = new File(cacheDir, imageName);

        // 保存图片
        FileOutputStream outputStream = new FileOutputStream(imageFile);
        boolean saved = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
        outputStream.close();

        if (!saved) {
            throw new IOException("图片保存失败");
        }

        Log.d(TAG, "图片已保存到私有目录: " + imageFile.getAbsolutePath());

        // 使用FileProvider生成可共享的URI
        return FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider",
                imageFile);
    }

    /**
     * 生成默认文件名（时间戳格式）
     */
    private String generateFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        String timeStamp = sdf.format(new Date());
        return "PhotoEdit_" + timeStamp + ".jpg";
    }
}