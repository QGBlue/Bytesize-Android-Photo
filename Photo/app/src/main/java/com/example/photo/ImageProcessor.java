package com.example.photo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;

public class ImageProcessor {

    // 增强版亮度调节算法 - 效果更明显
    public static Bitmap adjustBrightness(Bitmap originalBitmap, int value) {
        Bitmap adjustedBitmap = Bitmap.createBitmap(
                originalBitmap.getWidth(),
                originalBitmap.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(adjustedBitmap);
        Paint paint = new Paint();

        // 大幅增强亮度调整效果
        float brightness = value / 50.0f;

        ColorMatrix matrix = new ColorMatrix();
        matrix.set(new float[] {
                1, 0, 0, 0, brightness,
                0, 1, 0, 0, brightness,
                0, 0, 1, 0, brightness,
                0, 0, 0, 1, 0
        });

        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        canvas.drawBitmap(originalBitmap, 0, 0, paint);

        return adjustedBitmap;
    }

    // 对比度调节算法保持不变
    public static Bitmap adjustContrast(Bitmap originalBitmap, int value) {
        Bitmap adjustedBitmap = Bitmap.createBitmap(
                originalBitmap.getWidth(),
                originalBitmap.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        float contrast = (value + 50) / 50.0f;

        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();
        int[] pixels = new int[width * height];

        originalBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int alpha = Color.alpha(pixels[i]);
            int red = Color.red(pixels[i]);
            int green = Color.green(pixels[i]);
            int blue = Color.blue(pixels[i]);

            red = (int) (((red - 127) * contrast) + 127);
            green = (int) (((green - 127) * contrast) + 127);
            blue = (int) (((blue - 127) * contrast) + 127);

            red = Math.max(0, Math.min(255, red));
            green = Math.max(0, Math.min(255, green));
            blue = Math.max(0, Math.min(255, blue));

            pixels[i] = Color.argb(alpha, red, green, blue);
        }

        adjustedBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return adjustedBitmap;
    }

    // 同时调整亮度和对比度 - 只增强亮度部分
    public static Bitmap adjustBrightnessContrast(Bitmap originalBitmap, int brightness, int contrast) {
        Bitmap adjustedBitmap = Bitmap.createBitmap(
                originalBitmap.getWidth(),
                originalBitmap.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(adjustedBitmap);
        Paint paint = new Paint();

        // 增强亮度调整
        float bright = brightness / 50.0f;

        // 对比度调整保持不变
        float contr = (contrast + 50) / 50.0f;

        // 创建颜色矩阵
        ColorMatrix matrix = new ColorMatrix();

        // 对比度矩阵
        float scale = contr;
        float translate = (1 - contr) * 0.5f * 255;
        ColorMatrix contrastMatrix = new ColorMatrix(new float[] {
                scale, 0, 0, 0, translate,
                0, scale, 0, 0, translate,
                0, 0, scale, 0, translate,
                0, 0, 0, 1, 0
        });

        // 亮度矩阵 - 增强效果
        ColorMatrix brightnessMatrix = new ColorMatrix(new float[] {
                1, 0, 0, 0, bright,
                0, 1, 0, 0, bright,
                0, 0, 1, 0, bright,
                0, 0, 0, 1, 0
        });

        // 合并两个矩阵
        matrix.postConcat(contrastMatrix);
        matrix.postConcat(brightnessMatrix);

        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        canvas.drawBitmap(originalBitmap, 0, 0, paint);

        return adjustedBitmap;
    }

    // 超强亮度效果版本
    public static Bitmap adjustBrightnessExtreme(Bitmap originalBitmap, int brightness) {
        Bitmap adjustedBitmap = Bitmap.createBitmap(
                originalBitmap.getWidth(),
                originalBitmap.getHeight(),
                Bitmap.Config.ARGB_8888
        );

        int width = originalBitmap.getWidth();
        int height = originalBitmap.getHeight();
        int[] pixels = new int[width * height];

        originalBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        float brightFactor = brightness / 25.0f;

        for (int i = 0; i < pixels.length; i++) {
            int alpha = Color.alpha(pixels[i]);
            int red = Color.red(pixels[i]);
            int green = Color.green(pixels[i]);
            int blue = Color.blue(pixels[i]);

            red = (int) (red + brightFactor * 128 * (1 + Math.abs(brightFactor) / 10));
            green = (int) (green + brightFactor * 128 * (1 + Math.abs(brightFactor) / 10));
            blue = (int) (blue + brightFactor * 128 * (1 + Math.abs(brightFactor) / 10));

            red = Math.max(0, Math.min(255, red));
            green = Math.max(0, Math.min(255, green));
            blue = Math.max(0, Math.min(255, blue));

            pixels[i] = Color.argb(alpha, red, green, blue);
        }

        adjustedBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return adjustedBitmap;
    }

    /**
     * 旋转图片
     * @param bitmap 原图
     * @param degrees 旋转角度（90, -90, 180）
     * @return 旋转后的图片
     */
    public static Bitmap rotateBitmap(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);

        return Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /**
     * 翻转图片
     * @param bitmap 原图
     * @param horizontal 是否水平翻转
     * @param vertical 是否垂直翻转
     * @return 翻转后的图片
     */
    public static Bitmap flipBitmap(Bitmap bitmap, boolean horizontal, boolean vertical) {
        Matrix matrix = new Matrix();

        float sx = horizontal ? -1 : 1;
        float sy = vertical ? -1 : 1;

        matrix.postScale(sx, sy);

        // 调整位置，确保图片显示正确
        if (horizontal) {
            matrix.postTranslate(bitmap.getWidth(), 0);
        }
        if (vertical) {
            matrix.postTranslate(0, bitmap.getHeight());
        }

        return Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    /**
     * 获取当前旋转角度（用于连续旋转）
     */
    public static float getCurrentRotation(Matrix matrix) {
        float[] values = new float[9];
        matrix.getValues(values);
        return (float) Math.toDegrees(Math.atan2(values[Matrix.MSKEW_X], values[Matrix.MSCALE_X]));
    }
}