package com.example.photo;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.DrawableRes;

public class DraggableStickerView extends View {

    private Bitmap stickerBitmap;
    private Matrix matrix = new Matrix();

    // 变换参数
    private float scaleFactor = 1.0f;
    private float rotationAngle = 0.0f;
    private float pivotX = 0;
    private float pivotY = 0;
    private float alpha = 1.0f; // 透明度，范围 0.0-1.0

    // 触摸相关
    private static final int NONE = 0;
    private static final int DRAG = 1;
    private static final int ZOOM = 2;
    private static final int ROTATE = 3;

    private int mode = NONE;
    private float startX, startY;
    private float lastTouchX, lastTouchY;
    private float startDistance;
    private float startRotation;

    // 控制点
    private RectF deleteBounds;
    private RectF rotateBounds;
    private static final int CONTROL_POINT_SIZE = 60;

    // 监听器
    private OnStickerSelectedListener listener;
    private boolean isSelected = false;

    // 画笔
    private Paint borderPaint;
    private Paint controlPointPaint;

    public interface OnStickerSelectedListener {
        void onStickerSelected(DraggableStickerView stickerView);
        void onStickerDeleted(DraggableStickerView stickerView);
    }

    public DraggableStickerView(Context context) {
        super(context);
        init();
    }

    public DraggableStickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DraggableStickerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        // 初始化控制点边界
        deleteBounds = new RectF();
        rotateBounds = new RectF();

        // 初始化画笔
        borderPaint = new Paint();
        borderPaint.setColor(Color.BLUE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3);
        borderPaint.setAntiAlias(true);

        controlPointPaint = new Paint();
        controlPointPaint.setColor(Color.RED);
        controlPointPaint.setStyle(Paint.Style.FILL);
        controlPointPaint.setAntiAlias(true);
    }

    public void setStickerBitmap(Bitmap bitmap) {
        this.stickerBitmap = bitmap;
        if (bitmap != null) {
            // 初始位置居中
            pivotX = bitmap.getWidth() / 2f;
            pivotY = bitmap.getHeight() / 2f;

            // 初始矩阵
            matrix.reset();
            matrix.postTranslate(-pivotX, -pivotY);
            matrix.postScale(scaleFactor, scaleFactor, pivotX, pivotY);
            matrix.postRotate(rotationAngle, pivotX, pivotY);

            // 更新视图大小
            updateViewSize();
        }
        invalidate();
    }

    public void setStickerResource(@DrawableRes int resId) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
        setStickerBitmap(bitmap);
    }

    private void updateViewSize() {
        if (stickerBitmap != null) {
            // 根据缩放因子调整视图大小
            int newWidth = (int) (stickerBitmap.getWidth() * scaleFactor * 1.5f);
            int newHeight = (int) (stickerBitmap.getHeight() * scaleFactor * 1.5f);

            // 更新布局参数
            getLayoutParams().width = newWidth;
            getLayoutParams().height = newHeight;
            requestLayout();
        }
    }

    public Bitmap getStickerBitmap() {
        return stickerBitmap;
    }

    public float getAlphaValue() {
        return alpha;
    }

    public void setAlphaValue(float alpha) {
        this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        invalidate();
    }

    @Override
    public void setAlpha(float alpha) {
        this.alpha = alpha;
        super.setAlpha(alpha);
        invalidate();
    }

    public float getScaleFactor() {
        return scaleFactor;
    }

    public float getRotationAngle() {
        return rotationAngle;
    }

    public Matrix getTransformMatrix() {
        return new Matrix(matrix);
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
        invalidate();
    }

    public void setOnStickerSelectedListener(OnStickerSelectedListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (stickerBitmap == null) return;

        canvas.save();

        // 应用透明度
        Paint paint = new Paint();
        paint.setAlpha((int) (alpha * 255));

        // 应用变换矩阵
        canvas.concat(matrix);

        // 绘制贴纸图片
        canvas.drawBitmap(stickerBitmap, 0, 0, paint);

        canvas.restore();

        // 如果选中，绘制边框和控制点
        if (isSelected) {
            drawSelectionBorder(canvas);
        }
    }

    private void drawSelectionBorder(Canvas canvas) {
        // 获取贴纸的边界框
        RectF bounds = getStickerBounds();

        // 绘制边框
        canvas.drawRect(bounds, borderPaint);

        // 绘制控制点
        // 左上角：删除控制点
        deleteBounds.set(
                bounds.left - CONTROL_POINT_SIZE / 2,
                bounds.top - CONTROL_POINT_SIZE / 2,
                bounds.left + CONTROL_POINT_SIZE / 2,
                bounds.top + CONTROL_POINT_SIZE / 2
        );
        canvas.drawCircle(deleteBounds.centerX(), deleteBounds.centerY(),
                CONTROL_POINT_SIZE / 2, controlPointPaint);

        // 右下角：旋转控制点
        rotateBounds.set(
                bounds.right - CONTROL_POINT_SIZE / 2,
                bounds.bottom - CONTROL_POINT_SIZE / 2,
                bounds.right + CONTROL_POINT_SIZE / 2,
                bounds.bottom + CONTROL_POINT_SIZE / 2
        );
        canvas.drawCircle(rotateBounds.centerX(), rotateBounds.centerY(),
                CONTROL_POINT_SIZE / 2, controlPointPaint);
    }

    private RectF getStickerBounds() {
        if (stickerBitmap == null) {
            return new RectF();
        }

        // 创建一个矩形表示贴纸的边界
        RectF bounds = new RectF(0, 0, stickerBitmap.getWidth(), stickerBitmap.getHeight());

        // 应用变换矩阵
        Matrix tempMatrix = new Matrix(matrix);
        tempMatrix.mapRect(bounds);

        return bounds;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                handleTouchDown(x, y);
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                handlePointerDown(event);
                break;

            case MotionEvent.ACTION_MOVE:
                handleTouchMove(event, x, y);
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                handleTouchUp();
                break;
        }

        return true;
    }

    private void handleTouchDown(float x, float y) {
        // 检查是否点击了控制点
        RectF bounds = getStickerBounds();

        // 检查删除控制点
        if (isSelected && deleteBounds.contains(x, y)) {
            if (listener != null) {
                listener.onStickerDeleted(this);
            }
            return;
        }

        // 检查旋转控制点
        if (isSelected && rotateBounds.contains(x, y)) {
            mode = ROTATE;
            startX = x;
            startY = y;
            return;
        }

        // 检查是否点击在贴纸范围内
        if (bounds.contains(x, y)) {
            // 选中贴纸
            if (listener != null) {
                listener.onStickerSelected(this);
            }

            mode = DRAG;
            startX = x;
            startY = y;
            lastTouchX = x;
            lastTouchY = y;
        }
    }

    private void handlePointerDown(MotionEvent event) {
        if (event.getPointerCount() == 2) {
            mode = ZOOM;

            // 计算两点距离
            float x1 = event.getX(0);
            float y1 = event.getY(0);
            float x2 = event.getX(1);
            float y2 = event.getY(1);

            startDistance = (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));

            // 计算起始旋转角度
            startRotation = (float) Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
        }
    }

    private void handleTouchMove(MotionEvent event, float x, float y) {
        switch (mode) {
            case DRAG:
                float dx = x - lastTouchX;
                float dy = y - lastTouchY;

                matrix.postTranslate(dx, dy);
                invalidate();

                lastTouchX = x;
                lastTouchY = y;
                break;

            case ZOOM:
                if (event.getPointerCount() == 2) {
                    // 计算新的距离
                    float x1 = event.getX(0);
                    float y1 = event.getY(0);
                    float x2 = event.getX(1);
                    float y2 = event.getY(1);

                    float newDistance = (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));

                    // 计算缩放比例
                    float scale = newDistance / startDistance;

                    // 计算两点中点
                    float midX = (x1 + x2) / 2;
                    float midY = (y1 + y2) / 2;

                    // 应用缩放
                    matrix.postScale(scale, scale, midX, midY);
                    scaleFactor *= scale;

                    // 更新起始距离
                    startDistance = newDistance;
                    invalidate();

                    // 更新视图大小
                    updateViewSize();
                }
                break;

            case ROTATE:
                // 计算旋转角度
                float deltaX = x - startX;
                float deltaY = y - startY;
                float angle = (float) Math.toDegrees(Math.atan2(deltaY, deltaX));

                // 计算旋转中心（贴纸中心）
                RectF bounds = getStickerBounds();
                float centerX = bounds.centerX();
                float centerY = bounds.centerY();

                // 应用旋转
                matrix.postRotate(angle - rotationAngle, centerX, centerY);
                rotationAngle = angle;
                invalidate();

                startX = x;
                startY = y;
                break;
        }
    }

    private void handleTouchUp() {
        mode = NONE;
    }

    // 清理资源
    public void recycle() {
        if (stickerBitmap != null && !stickerBitmap.isRecycled()) {
            stickerBitmap.recycle();
            stickerBitmap = null;
        }
    }
}