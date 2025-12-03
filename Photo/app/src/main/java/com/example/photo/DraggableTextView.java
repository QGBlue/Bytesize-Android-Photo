package com.example.photo;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.TextView;

public class DraggableTextView extends androidx.appcompat.widget.AppCompatTextView {

    // ============ 基础属性 ============
    private float lastX, lastY;
    private boolean isDragging = false;
    private Paint borderPaint;
    private boolean isSelected = false;
    private OnTextSelectedListener listener;

    // 文字样式属性
    private int currentColor = Color.WHITE;
    private float currentSize = 24;
    private float currentOpacity = 1.0f;
    private String currentFont = "default";

    // ============ 缩放和旋转属性 ============
    private ScaleGestureDetector scaleDetector;
    private float scaleFactor = 1.0f;
    private static final float MIN_SCALE = 0.5f;
    private static final float MAX_SCALE = 3.0f;

    // 旋转相关
    private float rotationAngle = 0.0f;
    private PointF rotationCenter = new PointF();
    private boolean isRotating = false;
    private float lastRotationAngle = 0.0f;

    // 触摸相关
    private int activePointerId = MotionEvent.INVALID_POINTER_ID;
    private PointF lastTouch = new PointF();

    // 操作模式
    private static final int MODE_NONE = 0;
    private static final int MODE_DRAG = 1;
    private static final int MODE_SCALE = 2;
    private static final int MODE_ROTATE = 3;
    private int currentMode = MODE_NONE;

    public interface OnTextSelectedListener {
        void onTextSelected(DraggableTextView textView);
    }

    public DraggableTextView(Context context) {
        super(context);
        init(context);
    }

    public DraggableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public DraggableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        // 设置默认样式
        setTextColor(currentColor);
        setTextSize(currentSize);
        setAlpha(currentOpacity);
        setShadowLayer(3, 1, 1, Color.BLACK);

        // 边框画笔
        borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#4CAF50"));
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3);

        // 初始化缩放检测器
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());

        // 设置旋转中心为视图中心（默认）
        rotationCenter.set(getPivotX(), getPivotY());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // 更新旋转中心为视图中心
        rotationCenter.set(w / 2f, h / 2f);
        setPivotX(rotationCenter.x);
        setPivotY(rotationCenter.y);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // 应用旋转和缩放
        setRotation(rotationAngle);
        setScaleX(scaleFactor);
        setScaleY(scaleFactor);

        super.onDraw(canvas);

        // 如果被选中，绘制边框和控制点
        if (isSelected) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), borderPaint);

            // 绘制缩放和旋转控制点
            drawControlPoints(canvas);
        }
    }

    private void drawControlPoints(Canvas canvas) {
        Paint controlPaint = new Paint();
        controlPaint.setColor(Color.parseColor("#2196F3"));
        controlPaint.setStyle(Paint.Style.FILL);

        // 缩放控制点（右下角）
        float scaleX = getWidth() - 20;
        float scaleY = getHeight() - 20;
        canvas.drawCircle(scaleX, scaleY, 15, controlPaint);

        // 旋转控制点（右上角）
        float rotateX = getWidth() - 20;
        float rotateY = 20;
        canvas.drawCircle(rotateX, rotateY, 15, controlPaint);

        // 绘制图标
        Paint iconPaint = new Paint();
        iconPaint.setColor(Color.WHITE);
        iconPaint.setTextSize(12);
        iconPaint.setTextAlign(Paint.Align.CENTER);

        // 缩放图标（放大镜）
        canvas.drawText("+", scaleX, scaleY + 4, iconPaint);

        // 旋转图标（箭头）
        canvas.save();
        canvas.rotate(45, rotateX, rotateY);
        canvas.drawText("↻", rotateX, rotateY + 4, iconPaint);
        canvas.restore();
    }

    // ============ 触摸事件处理 ============

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // 首先传递给缩放检测器
        scaleDetector.onTouchEvent(event);

        // 获取触摸点数量
        int pointerCount = event.getPointerCount();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                // 单指按下
                activePointerId = event.getPointerId(0);
                lastTouch.set(event.getX(), event.getY());

                // 检查是否点击了控制点
                if (isSelected && isTouchingControlPoint(event.getX(), event.getY())) {
                    return true; // 交给控制点处理
                }

                // 选中文字
                setSelected(true);
                if (listener != null) {
                    listener.onTextSelected(this);
                }

                currentMode = MODE_DRAG;
                return true;

            case MotionEvent.ACTION_POINTER_DOWN:
                // 多指按下
                if (pointerCount == 2) {
                    // 双指按下，准备进行缩放或旋转
                    currentMode = MODE_SCALE;

                    // 计算两个手指的中点作为旋转中心
                    float x1 = event.getX(0);
                    float y1 = event.getY(0);
                    float x2 = event.getX(1);
                    float y2 = event.getY(1);

                    rotationCenter.set((x1 + x2) / 2, (y1 + y2) / 2);
                    setPivotX(rotationCenter.x);
                    setPivotY(rotationCenter.y);

                    // 计算初始距离和角度
                    lastRotationAngle = (float) Math.toDegrees(
                            Math.atan2(y2 - y1, x2 - x1)
                    );
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (currentMode == MODE_DRAG && pointerCount == 1) {
                    // 单指拖动
                    int pointerIndex = event.findPointerIndex(activePointerId);
                    if (pointerIndex == -1) break;

                    float x = event.getX(pointerIndex);
                    float y = event.getY(pointerIndex);

                    float dx = x - lastTouch.x;
                    float dy = y - lastTouch.y;

                    setTranslationX(getTranslationX() + dx);
                    setTranslationY(getTranslationY() + dy);

                    lastTouch.set(x, y);
                } else if (currentMode == MODE_SCALE && pointerCount == 2) {
                    // 双指旋转
                    float x1 = event.getX(0);
                    float y1 = event.getY(0);
                    float x2 = event.getX(1);
                    float y2 = event.getY(1);

                    float currentAngle = (float) Math.toDegrees(
                            Math.atan2(y2 - y1, x2 - x1)
                    );

                    float deltaAngle = currentAngle - lastRotationAngle;
                    rotationAngle += deltaAngle;

                    // 限制在0-360度
                    rotationAngle = rotationAngle % 360;
                    if (rotationAngle < 0) rotationAngle += 360;

                    lastRotationAngle = currentAngle;
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_POINTER_UP:
                // 有手指抬起
                if (pointerCount == 2) {
                    // 双指变单指，切换为拖动模式
                    currentMode = MODE_DRAG;
                    int remainingPointerIndex = (event.getActionIndex() == 0) ? 1 : 0;
                    activePointerId = event.getPointerId(remainingPointerIndex);
                    lastTouch.set(
                            event.getX(remainingPointerIndex),
                            event.getY(remainingPointerIndex)
                    );
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 重置状态
                currentMode = MODE_NONE;
                activePointerId = MotionEvent.INVALID_POINTER_ID;
                break;
        }

        return true;
    }

    private boolean isTouchingControlPoint(float x, float y) {
        // 检查是否触摸到缩放控制点（右下角）
        float scaleX = getWidth() - 20;
        float scaleY = getHeight() - 20;
        if (Math.sqrt(Math.pow(x - scaleX, 2) + Math.pow(y - scaleY, 2)) <= 30) {
            currentMode = MODE_SCALE;
            return true;
        }

        // 检查是否触摸到旋转控制点（右上角）
        float rotateX = getWidth() - 20;
        float rotateY = 20;
        if (Math.sqrt(Math.pow(x - rotateX, 2) + Math.pow(y - rotateY, 2)) <= 30) {
            currentMode = MODE_ROTATE;
            return true;
        }

        return false;
    }

    // ============ 缩放监听器 ============

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            scaleFactor *= detector.getScaleFactor();
            scaleFactor = Math.max(MIN_SCALE, Math.min(scaleFactor, MAX_SCALE));

            // 更新旋转中心
            rotationCenter.set(detector.getFocusX(), detector.getFocusY());
            setPivotX(rotationCenter.x);
            setPivotY(rotationCenter.y);

            invalidate();
            return true;
        }

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            currentMode = MODE_SCALE;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            // 缩放结束
        }
    }

    // ============ 样式设置方法 ============

    public void setTextColorHex(String colorHex) {
        try {
            currentColor = Color.parseColor(colorHex);
            setTextColor(currentColor);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setTextSizeValue(float size) {
        currentSize = size;
        setTextSize(size);
    }

    public void setTextOpacity(float opacity) {
        currentOpacity = opacity;
        setAlpha(opacity);
    }

    public void setTextFont(String fontName) {
        currentFont = fontName;
        Typeface typeface;

        switch (fontName) {
            case "default":
                typeface = Typeface.DEFAULT;
                break;
            case "bold":
                typeface = Typeface.DEFAULT_BOLD;
                break;
            case "serif":
                typeface = Typeface.SERIF;
                break;
            case "sans":
                typeface = Typeface.SANS_SERIF;
                break;
            case "monospace":
                typeface = Typeface.MONOSPACE;
                break;
            default:
                typeface = Typeface.DEFAULT;
        }

        setTypeface(typeface);
    }

    // ============ 缩放和旋转控制方法 ============

    public void setScaleFactor(float scale) {
        this.scaleFactor = Math.max(MIN_SCALE, Math.min(scale, MAX_SCALE));
        invalidate();
    }

    public void setRotationAngle(float angle) {
        this.rotationAngle = angle % 360;
        if (this.rotationAngle < 0) this.rotationAngle += 360;
        invalidate();
    }

    // ============ 获取当前样式和状态 ============

    public String getCurrentFont() { return currentFont; }
    public float getCurrentSize() { return currentSize; }
    public int getCurrentColor() { return currentColor; }
    public float getCurrentOpacity() { return currentOpacity; }
    public float getScaleFactor() { return scaleFactor; }
    public float getRotationAngle() { return rotationAngle; }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
        invalidate(); // 重绘以显示/隐藏边框和控制点
    }

    public void setOnTextSelectedListener(OnTextSelectedListener listener) {
        this.listener = listener;
    }

    /**
     * 获取文字的变换矩阵（包括位置、缩放、旋转）
     */
    public Matrix getTransformMatrix() {
        Matrix matrix = new Matrix();

        // 应用平移
        matrix.postTranslate(getTranslationX(), getTranslationY());

        // 应用缩放（以视图中心为缩放中心）
        matrix.postScale(scaleFactor, scaleFactor,
                getWidth() / 2f, getHeight() / 2f);

        // 应用旋转（以视图中心为旋转中心）
        matrix.postRotate(rotationAngle,
                getWidth() / 2f, getHeight() / 2f);

        return matrix;
    }
    /**
     * 获取文字在容器中的边界（包含变换）
     */
    public RectF getTransformedBounds() {
        RectF bounds = new RectF(0, 0, getWidth(), getHeight());
        Matrix matrix = getTransformMatrix();
        matrix.mapRect(bounds);
        return bounds;
    }
    /**
     * 获取文字的精确变换矩阵
     */
    public Matrix getExactTransformMatrix() {
        Matrix matrix = new Matrix();

        // 应用平移
        matrix.postTranslate(getX() + getTranslationX(), getY() + getTranslationY());

        // 应用缩放（以文字中心为原点）
        matrix.postScale(getScaleX(), getScaleY(),
                getWidth() / 2f, getHeight() / 2f);

        // 应用旋转（以文字中心为原点）
        matrix.postRotate(getRotation(),
                getWidth() / 2f, getHeight() / 2f);

        return matrix;
    }


    /**
     * 获取文字的实际边界（考虑所有变换后）
     */
    public RectF getActualBounds() {
        // 获取文字视图的原始边界
        RectF bounds = new RectF(0, 0, getWidth(), getHeight());

        // 创建变换矩阵
        Matrix matrix = new Matrix();

        // 1. 移动到中心（以中心点为变换基准）
        matrix.postTranslate(-getWidth() / 2f, -getHeight() / 2f);

        // 2. 应用缩放
        matrix.postScale(scaleFactor, scaleFactor);

        // 3. 应用旋转
        matrix.postRotate(rotationAngle);

        // 4. 移回到实际位置（包括平移变换）
        matrix.postTranslate(getTranslationX() + getWidth() / 2f,
                getTranslationY() + getHeight() / 2f);

        // 应用矩阵变换到边界
        matrix.mapRect(bounds);

        return bounds;
    }

    /**
     * 获取文字中心点位置
     */
    public PointF getCenterPoint() {
        RectF bounds = getActualBounds();
        return new PointF(bounds.centerX(), bounds.centerY());
    }

}