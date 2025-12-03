package com.example.photo;

import android.graphics.Bitmap;
import android.util.Log;

import com.example.photo.FilterProcessor.FilterType;

import java.util.Stack;

public class FilterManager {

    private static final String TAG = "FilterManager";

    private Bitmap originalBitmap;
    private Bitmap currentBitmap;
    private FilterType currentFilter = FilterType.NONE;

    private Stack<Bitmap> undoStack = new Stack<>();
    private Stack<Bitmap> redoStack = new Stack<>();

    public FilterManager(Bitmap originalBitmap) {
        this.originalBitmap = originalBitmap;
        this.currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        saveState();
    }

    /**
     * 应用滤镜
     */
    public Bitmap applyFilter(FilterType filterType) {
        if (currentBitmap == null) {
            Log.e(TAG, "applyFilter: currentBitmap is null");
            return null;
        }

        // 保存当前状态到撤销栈
        saveState();

        // 清空重做栈
        redoStack.clear();

        // 应用滤镜
        currentFilter = filterType;
        if (filterType == FilterType.NONE) {
            // 恢复原图
            currentBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        } else {
            // 应用滤镜效果
            currentBitmap = FilterProcessor.applyFilter(currentBitmap, filterType);
        }

        return currentBitmap.copy(Bitmap.Config.ARGB_8888, true);
    }

    /**
     * 保存当前状态
     */
    private void saveState() {
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            undoStack.push(currentBitmap.copy(Bitmap.Config.ARGB_8888, true));

            // 限制栈大小，避免内存溢出
            if (undoStack.size() > 10) {
                Bitmap oldest = undoStack.remove(0);
                if (oldest != null && !oldest.isRecycled()) {
                    oldest.recycle();
                }
            }
        }
    }

    /**
     * 撤销操作
     */
    public Bitmap undo() {
        if (canUndo()) {
            // 保存当前状态到重做栈
            redoStack.push(currentBitmap.copy(Bitmap.Config.ARGB_8888, true));

            // 恢复上一个状态
            currentBitmap = undoStack.pop();
            return currentBitmap.copy(Bitmap.Config.ARGB_8888, true);
        }
        return null;
    }

    /**
     * 重做操作
     */
    public Bitmap redo() {
        if (canRedo()) {
            // 保存当前状态到撤销栈
            undoStack.push(currentBitmap.copy(Bitmap.Config.ARGB_8888, true));

            // 恢复下一个状态
            currentBitmap = redoStack.pop();
            return currentBitmap.copy(Bitmap.Config.ARGB_8888, true);
        }
        return null;
    }

    /**
     * 是否可以撤销
     */
    public boolean canUndo() {
        return !undoStack.isEmpty();
    }

    /**
     * 是否可以重做
     */
    public boolean canRedo() {
        return !redoStack.isEmpty();
    }

    /**
     * 获取当前滤镜类型
     */
    public FilterType getCurrentFilter() {
        return currentFilter;
    }

    /**
     * 获取当前图片
     */
    public Bitmap getCurrentBitmap() {
        if (currentBitmap != null && !currentBitmap.isRecycled()) {
            return currentBitmap.copy(Bitmap.Config.ARGB_8888, true);
        }
        return null;
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        // 清理撤销栈
        while (!undoStack.isEmpty()) {
            Bitmap bitmap = undoStack.pop();
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }

        // 清理重做栈
        while (!redoStack.isEmpty()) {
            Bitmap bitmap = redoStack.pop();
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }

        undoStack.clear();
        redoStack.clear();
    }
}