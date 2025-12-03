package com.example.photo;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.view.View;
import android.widget.ImageView;

public class CoordinateConverter {

    /**
     * 将屏幕坐标转换为图片像素坐标
     * @param screenX 屏幕X坐标
     * @param screenY 屏幕Y坐标
     * @param imageView 图片视图
     * @param originalBitmapWidth 原始图片宽度
     * @param originalBitmapHeight 原始图片高度
     * @return 图片像素坐标 [x, y]
     */
    public static float[] screenToImageCoordinates(
            float screenX, float screenY,
            ImageView imageView,
            int originalBitmapWidth,
            int originalBitmapHeight) {

        // 获取图片在ImageView中的变换矩阵
        Matrix imageMatrix = imageView.getImageMatrix();
        Matrix inverseMatrix = new Matrix();

        // 计算逆矩阵
        if (!imageMatrix.invert(inverseMatrix)) {
            // 如果矩阵不可逆，返回原始坐标
            return new float[]{screenX, screenY};
        }

        // 创建源点数组
        float[] src = {screenX, screenY};
        float[] dst = new float[2];

        // 应用逆矩阵转换
        inverseMatrix.mapPoints(dst, src);

        // 确保坐标在图片范围内
        dst[0] = Math.max(0, Math.min(dst[0], originalBitmapWidth));
        dst[1] = Math.max(0, Math.min(dst[1], originalBitmapHeight));

        return dst;
    }

    /**
     * 获取图片在ImageView中的实际显示区域
     */
    public static RectF getImageDisplayRect(ImageView imageView) {
        RectF rect = new RectF();

        if (imageView.getDrawable() != null) {
            // 获取图片边界
            RectF imageBounds = new RectF(0, 0,
                    imageView.getDrawable().getIntrinsicWidth(),
                    imageView.getDrawable().getIntrinsicHeight());

            // 获取图片变换矩阵
            Matrix matrix = imageView.getImageMatrix();

            // 应用变换矩阵
            matrix.mapRect(rect, imageBounds);
        }

        return rect;
    }

    /**
     * 计算视图在屏幕上的中心点
     */
    public static float[] getViewCenterOnScreen(View view) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);

        float centerX = location[0] + view.getWidth() / 2f;
        float centerY = location[1] + view.getHeight() / 2f;

        return new float[]{centerX, centerY};
    }

    /**
     * 获取视图在父容器中的中心点相对坐标
     */
    public static float[] getViewCenterRelative(View view) {
        float centerX = view.getX() + view.getWidth() / 2f;
        float centerY = view.getY() + view.getHeight() / 2f;

        return new float[]{centerX, centerY};
    }
}