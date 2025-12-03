package com.example.photo;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;

public class CropHelper {

    /**
     * 裁剪图片
     */
    public static Bitmap cropBitmap(Bitmap bitmap, RectF cropRect) {
        try {
            // 确保裁剪区域在图片范围内
            int left = (int) Math.max(0, cropRect.left);
            int top = (int) Math.max(0, cropRect.top);
            int width = (int) Math.min(bitmap.getWidth() - left, cropRect.width());
            int height = (int) Math.min(bitmap.getHeight() - top, cropRect.height());

            if (width <= 0 || height <= 0) {
                return bitmap;
            }

            return Bitmap.createBitmap(bitmap, left, top, width, height);
        } catch (Exception e) {
            e.printStackTrace();
            return bitmap;
        }
    }

    /**
     * 按固定比例裁剪图片（居中裁剪）
     */
    public static Bitmap cropBitmapWithRatio(Bitmap bitmap, float ratio) {
        int bitmapWidth = bitmap.getWidth();
        int bitmapHeight = bitmap.getHeight();

        int cropWidth, cropHeight;

        if (bitmapWidth / (float) bitmapHeight > ratio) {
            // 图片较宽，以高度为基准
            cropHeight = bitmapHeight;
            cropWidth = (int) (bitmapHeight * ratio);
        } else {
            // 图片较高，以宽度为基准
            cropWidth = bitmapWidth;
            cropHeight = (int) (bitmapWidth / ratio);
        }

        int left = (bitmapWidth - cropWidth) / 2;
        int top = (bitmapHeight - cropHeight) / 2;

        try {
            return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight);
        } catch (Exception e) {
            e.printStackTrace();
            return bitmap;
        }
    }

    /**
     * 获取常用裁剪比例的名称
     */
    public static String getRatioName(float ratio) {
        if (ratio == 1.0f) return "1:1 (正方形)";
        if (ratio == 4.0f/3) return "4:3 (标准)";
        if (ratio == 16.0f/9) return "16:9 (宽屏)";
        if (ratio == 3.0f/4) return "3:4 (竖屏)";
        if (ratio == 9.0f/16) return "9:16 (手机)";
        return "自由";
    }
}