package com.example.photo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

public class FilterProcessor {

    // 滤镜类型枚举
    public enum FilterType {
        NONE,           // 原图
        GRAYSCALE,      // 黑白
        VINTAGE,        // 复古
        FRESH,          // 清新
        WARM,           // 暖色调
        COOL,           // 冷色调
        BRIGHT,         // 明亮
        CONTRAST,       // 高对比度
        SEPIA,          // 怀旧
        INVERT          // 反色
    }

    /**
     * 应用滤镜效果
     */
    public static Bitmap applyFilter(Bitmap originalBitmap, FilterType filterType) {
        if (originalBitmap == null) {
            return null;
        }

        // 创建新的Bitmap用于处理
        Bitmap resultBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);

        switch (filterType) {
            case GRAYSCALE:
                return applyGrayscale(resultBitmap);
            case VINTAGE:
                return applyVintage(resultBitmap);
            case FRESH:
                return applyFresh(resultBitmap);
            case WARM:
                return applyWarm(resultBitmap);
            case COOL:
                return applyCool(resultBitmap);
            case BRIGHT:
                return applyBright(resultBitmap);
            case CONTRAST:
                return applyContrast(resultBitmap);
            case SEPIA:
                return applySepia(resultBitmap);
            case INVERT:
                return applyInvert(resultBitmap);
            case NONE:
            default:
                return originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        }
    }

    /**
     * 黑白滤镜
     */
    private static Bitmap applyGrayscale(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();

        ColorMatrix matrix = new ColorMatrix();
        matrix.setSaturation(0); // 去饱和，变成灰度

        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return result;
    }

    /**
     * 复古滤镜
     */
    private static Bitmap applyVintage(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();

        // 复古效果：降低饱和度，增加红色通道，降低蓝色通道
        ColorMatrix matrix = new ColorMatrix();

        // 调整饱和度
        matrix.setSaturation(0.7f);

        // 调整颜色通道（增加红色，降低蓝色）
        float[] mat = new float[] {
                1.1f, 0, 0, 0, 10,   // 红色通道增强
                0, 0.9f, 0, 0, 0,    // 绿色通道减弱
                0, 0, 0.8f, 0, 0,    // 蓝色通道减弱
                0, 0, 0, 1, 0        // 透明度不变
        };

        ColorMatrix colorAdjust = new ColorMatrix(mat);
        matrix.postConcat(colorAdjust);

        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return result;
    }

    /**
     * 清新滤镜（提高亮度和饱和度）
     */
    private static Bitmap applyFresh(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();

        ColorMatrix matrix = new ColorMatrix();

        // 提高饱和度
        matrix.setSaturation(1.5f);

        // 提高亮度
        float[] brightnessMat = new float[] {
                1, 0, 0, 0, 20,
                0, 1, 0, 0, 20,
                0, 0, 1, 0, 20,
                0, 0, 0, 1, 0
        };

        ColorMatrix brightnessMatrix = new ColorMatrix(brightnessMat);
        matrix.postConcat(brightnessMatrix);

        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return result;
    }

    /**
     * 暖色调滤镜（增加红色和黄色）
     */
    private static Bitmap applyWarm(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();

        // 暖色调：增强红色和黄色，减弱蓝色
        ColorMatrix matrix = new ColorMatrix();

        float[] mat = new float[] {
                1.2f, 0, 0, 0, 0,    // 增强红色
                0, 1.1f, 0, 0, 0,    // 增强绿色（暖黄色）
                0, 0, 0.9f, 0, 0,    // 减弱蓝色
                0, 0, 0, 1, 0        // 透明度不变
        };

        matrix.set(mat);
        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return result;
    }

    /**
     * 冷色调滤镜（增加蓝色和青色）
     */
    private static Bitmap applyCool(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();

        // 冷色调：增强蓝色和青色，减弱红色
        ColorMatrix matrix = new ColorMatrix();

        float[] mat = new float[] {
                0.9f, 0, 0, 0, 0,    // 减弱红色
                0, 1.0f, 0, 0, 0,    // 绿色不变
                0, 0, 1.2f, 0, 0,    // 增强蓝色
                0, 0, 0, 1, 0        // 透明度不变
        };

        matrix.set(mat);
        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return result;
    }

    /**
     * 明亮滤镜
     */
    private static Bitmap applyBright(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();

        ColorMatrix matrix = new ColorMatrix();

        // 提高亮度
        float[] mat = new float[] {
                1.3f, 0, 0, 0, 30,
                0, 1.3f, 0, 0, 30,
                0, 0, 1.3f, 0, 30,
                0, 0, 0, 1, 0
        };

        matrix.set(mat);
        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return result;
    }

    /**
     * 高对比度滤镜
     */
    private static Bitmap applyContrast(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();

        ColorMatrix matrix = new ColorMatrix();

        // 提高对比度
        float contrast = 1.5f;
        float translate = (-0.5f * contrast + 0.5f) * 255;

        float[] mat = new float[] {
                contrast, 0, 0, 0, translate,
                0, contrast, 0, 0, translate,
                0, 0, contrast, 0, translate,
                0, 0, 0, 1, 0
        };

        matrix.set(mat);
        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return result;
    }

    /**
     * 怀旧（深褐色）滤镜
     */
    private static Bitmap applySepia(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();

        ColorMatrix matrix = new ColorMatrix();

        // 怀旧滤镜矩阵
        float[] mat = new float[] {
                0.393f, 0.769f, 0.189f, 0, 0,
                0.349f, 0.686f, 0.168f, 0, 0,
                0.272f, 0.534f, 0.131f, 0, 0,
                0, 0, 0, 1, 0
        };

        matrix.set(mat);
        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return result;
    }

    /**
     * 反色滤镜
     */
    private static Bitmap applyInvert(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        Paint paint = new Paint();

        ColorMatrix matrix = new ColorMatrix();

        // 反色矩阵
        float[] mat = new float[] {
                -1, 0, 0, 0, 255,
                0, -1, 0, 0, 255,
                0, 0, -1, 0, 255,
                0, 0, 0, 1, 0
        };

        matrix.set(mat);
        paint.setColorFilter(new ColorMatrixColorFilter(matrix));
        canvas.drawBitmap(bitmap, 0, 0, paint);

        return result;
    }

    /**
     * 获取滤镜名称
     */
    public static String getFilterName(FilterType filterType) {
        switch (filterType) {
            case NONE: return "原图";
            case GRAYSCALE: return "黑白";
            case VINTAGE: return "复古";
            case FRESH: return "清新";
            case WARM: return "暖色调";
            case COOL: return "冷色调";
            case BRIGHT: return "明亮";
            case CONTRAST: return "高对比";
            case SEPIA: return "怀旧";
            case INVERT: return "反色";
            default: return "原图";
        }
    }
}