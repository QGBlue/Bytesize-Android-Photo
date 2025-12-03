package com.example.photo;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

public class WatermarkUtils {

    /**
     * 添加文字水印到图片
     * @param bitmap 原图
     * @param watermarkText 水印文字
     * @return 添加水印后的图片
     */
    public static Bitmap addTextWatermark(Bitmap bitmap, String watermarkText) {
        if (bitmap == null) {
            return null;
        }

        // 创建一个可修改的Bitmap副本
        Bitmap result = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);

        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(40);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setAlpha(128); // 半透明效果（50%透明度）

        // 设置阴影
        paint.setShadowLayer(3, 1, 1, Color.BLACK);

        // 计算水印位置（右下角，留出边距）
        int padding = 20;
        float x = canvas.getWidth() - paint.measureText(watermarkText) - padding;
        float y = canvas.getHeight() - padding;

        // 绘制水印
        canvas.drawText(watermarkText, x, y, paint);

        return result;
    }

    /**
     * 添加多个水印（平铺效果）
     */
    public static Bitmap addTiledWatermark(Bitmap bitmap, String watermarkText) {
        if (bitmap == null) {
            return null;
        }

        Bitmap result = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);

        Paint paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(40);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setAlpha(60); // 更透明

        // 设置阴影
        paint.setShadowLayer(2, 1, 1, Color.BLACK);

        // 平铺水印
        float textWidth = paint.measureText(watermarkText);
        float textHeight = paint.getTextSize();

        // 计算间隔
        float horizontalSpacing = textWidth * 1.5f;
        float verticalSpacing = textHeight * 2f;

        // 绘制网格水印
        for (float y = textHeight; y < canvas.getHeight(); y += verticalSpacing) {
            for (float x = 0; x < canvas.getWidth(); x += horizontalSpacing) {
                // 交替偏移使效果更好
                float offsetX = ((y / verticalSpacing) % 2 == 0) ? x : x + textWidth / 2;
                canvas.drawText(watermarkText, offsetX, y, paint);
            }
        }

        return result;
    }

    /**
     * 添加角标水印（右下角）
     */
    public static Bitmap addCornerWatermark(Bitmap bitmap, String watermarkText) {
        if (bitmap == null) {
            return null;
        }

        Bitmap result = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(result);

        // 创建背景矩形
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.parseColor("#80000000")); // 半透明黑色
        bgPaint.setStyle(Paint.Style.FILL);

        // 文字画笔
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30);
        textPaint.setTypeface(Typeface.DEFAULT);
        textPaint.setAntiAlias(true);

        // 计算文字宽度和高度
        float textWidth = textPaint.measureText(watermarkText);
        float textHeight = textPaint.getTextSize();

        // 计算背景矩形位置（右下角）
        int padding = 10;
        float rectLeft = canvas.getWidth() - textWidth - padding * 2;
        float rectTop = canvas.getHeight() - textHeight - padding;
        float rectRight = canvas.getWidth() - padding;
        float rectBottom = canvas.getHeight() - padding;

        // 绘制背景
        canvas.drawRect(rectLeft, rectTop, rectRight, rectBottom, bgPaint);

        // 绘制文字
        float textX = rectLeft + padding;
        float textY = rectBottom - padding;
        canvas.drawText(watermarkText, textX, textY, textPaint);

        return result;
    }
}