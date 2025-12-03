package com.example.photo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class CropOverlayView extends View {

    private RectF cropRect;
    private Paint borderPaint;
    private Paint shadowPaint;
    private Paint cornerPaint;

    private float cornerTouchRadius = 50;
    private int activeCorner = -1; // 0:左上, 1:右上, 2:左下, 3:右下, -1:无

    private OnCropRectChangeListener listener;

    public interface OnCropRectChangeListener {
        void onCropRectChanged(RectF rect);
    }

    public CropOverlayView(Context context) {
        super(context);
        init();
    }

    public CropOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 边框画笔
        borderPaint = new Paint();
        borderPaint.setColor(Color.WHITE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3);

        // 阴影画笔
        shadowPaint = new Paint();
        shadowPaint.setColor(Color.argb(128, 0, 0, 0));

        // 角落画笔
        cornerPaint = new Paint();
        cornerPaint.setColor(Color.WHITE);
        cornerPaint.setStyle(Paint.Style.FILL);
    }

    public void setCropRect(RectF rect) {
        this.cropRect = rect;
        invalidate();
    }

    public RectF getCropRect() {
        return cropRect;
    }

    public void setOnCropRectChangeListener(OnCropRectChangeListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (cropRect == null) {
            return;
        }

        // 绘制外部阴影
        Path shadowPath = new Path();
        shadowPath.addRect(0, 0, getWidth(), getHeight(), Path.Direction.CW);
        shadowPath.addRect(cropRect, Path.Direction.CCW);
        canvas.drawPath(shadowPath, shadowPaint);

        // 绘制裁剪框边框
        canvas.drawRect(cropRect, borderPaint);

        // 绘制角落标记
        drawCorner(canvas, cropRect.left, cropRect.top);
        drawCorner(canvas, cropRect.right, cropRect.top);
        drawCorner(canvas, cropRect.left, cropRect.bottom);
        drawCorner(canvas, cropRect.right, cropRect.bottom);
    }

    private void drawCorner(Canvas canvas, float x, float y) {
        float cornerSize = 20;
        canvas.drawRect(x - 3, y - cornerSize, x + 3, y + cornerSize, cornerPaint);
        canvas.drawRect(x - cornerSize, y - 3, x + cornerSize, y + 3, cornerPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (cropRect == null) {
            return false;
        }

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                activeCorner = getTouchedCorner(x, y);
                return activeCorner != -1;

            case MotionEvent.ACTION_MOVE:
                if (activeCorner != -1) {
                    updateCropRect(x, y, activeCorner);
                    if (listener != null) {
                        listener.onCropRectChanged(cropRect);
                    }
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                activeCorner = -1;
                break;
        }

        return true;
    }

    private int getTouchedCorner(float x, float y) {
        float[] corners = {
                cropRect.left, cropRect.top,      // 左上
                cropRect.right, cropRect.top,     // 右上
                cropRect.left, cropRect.bottom,   // 左下
                cropRect.right, cropRect.bottom   // 右下
        };

        for (int i = 0; i < 4; i++) {
            float cornerX = corners[i * 2];
            float cornerY = corners[i * 2 + 1];
            if (Math.abs(x - cornerX) <= cornerTouchRadius &&
                    Math.abs(y - cornerY) <= cornerTouchRadius) {
                return i;
            }
        }

        return -1;
    }

    private void updateCropRect(float x, float y, int corner) {
        float minSize = 100; // 最小裁剪尺寸

        switch (corner) {
            case 0: // 左上
                cropRect.left = Math.max(0, Math.min(x, cropRect.right - minSize));
                cropRect.top = Math.max(0, Math.min(y, cropRect.bottom - minSize));
                break;
            case 1: // 右上
                cropRect.right = Math.min(getWidth(), Math.max(x, cropRect.left + minSize));
                cropRect.top = Math.max(0, Math.min(y, cropRect.bottom - minSize));
                break;
            case 2: // 左下
                cropRect.left = Math.max(0, Math.min(x, cropRect.right - minSize));
                cropRect.bottom = Math.min(getHeight(), Math.max(y, cropRect.top + minSize));
                break;
            case 3: // 右下
                cropRect.right = Math.min(getWidth(), Math.max(x, cropRect.left + minSize));
                cropRect.bottom = Math.min(getHeight(), Math.max(y, cropRect.top + minSize));
                break;
        }
    }
}