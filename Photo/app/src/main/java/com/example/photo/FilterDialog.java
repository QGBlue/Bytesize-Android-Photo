package com.example.photo;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.photo.FilterProcessor.FilterType;

import java.util.ArrayList;
import java.util.List;

public class FilterDialog {

    private Context context;
    private Bitmap originalBitmap;
    private OnFilterSelectedListener listener;

    private List<FilterItem> filterItems = new ArrayList<>();

    public interface OnFilterSelectedListener {
        void onFilterSelected(FilterType filterType);
    }

    public FilterDialog(Context context) {
        this.context = context;
        initFilterItems();
    }

    private void initFilterItems() {
        // 初始化滤镜列表
        filterItems.clear();
        filterItems.add(new FilterItem(FilterType.NONE, "原图"));
        filterItems.add(new FilterItem(FilterType.GRAYSCALE, "黑白"));
        filterItems.add(new FilterItem(FilterType.VINTAGE, "复古"));
        filterItems.add(new FilterItem(FilterType.FRESH, "清新"));
        filterItems.add(new FilterItem(FilterType.WARM, "暖色调"));
        filterItems.add(new FilterItem(FilterType.COOL, "冷色调"));
        filterItems.add(new FilterItem(FilterType.BRIGHT, "明亮"));
        filterItems.add(new FilterItem(FilterType.CONTRAST, "高对比"));
        filterItems.add(new FilterItem(FilterType.SEPIA, "怀旧"));
        filterItems.add(new FilterItem(FilterType.INVERT, "反色"));
    }

    public void show(Bitmap previewBitmap, OnFilterSelectedListener listener) {
        this.listener = listener;
        this.originalBitmap = previewBitmap;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_filter, null);
        builder.setView(dialogView);

        LinearLayout filterContainer = dialogView.findViewById(R.id.filterContainer);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        AlertDialog dialog = builder.create();

        // 创建滤镜预览项
        createFilterItems(filterContainer, dialog);

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void createFilterItems(LinearLayout container, AlertDialog dialog) {
        container.removeAllViews();

        for (FilterItem filterItem : filterItems) {
            View filterView = LayoutInflater.from(context).inflate(R.layout.item_filter, null);

            ImageView previewImage = filterView.findViewById(R.id.filterPreview);
            TextView filterName = filterView.findViewById(R.id.filterName);

            // 设置滤镜名称
            filterName.setText(filterItem.name);

            // 生成滤镜预览（使用缩略图提高性能）
            Bitmap preview = createFilterPreview(filterItem.type);
            if (preview != null) {
                previewImage.setImageBitmap(preview);
            }

            // 设置点击事件
            filterView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFilterSelected(filterItem.type);
                }
                dialog.dismiss();
            });

            // 添加到容器
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, // 宽度
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            params.weight = 1;
            params.setMargins(4, 4, 4, 4);
            filterView.setLayoutParams(params);

            container.addView(filterView);
        }
    }

    private Bitmap createFilterPreview(FilterType filterType) {
        if (originalBitmap == null) {
            return null;
        }

        try {
            // 创建缩略图以提高预览性能
            int previewSize = 200; // 预览图大小
            Bitmap thumbnail = Bitmap.createScaledBitmap(
                    originalBitmap,
                    previewSize,
                    previewSize,
                    true
            );

            // 应用滤镜效果
            return FilterProcessor.applyFilter(thumbnail, filterType);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static class FilterItem {
        FilterType type;
        String name;

        FilterItem(FilterType type, String name) {
            this.type = type;
            this.name = name;
        }
    }
}