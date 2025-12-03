package com.example.photo;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class TextTransformDialog {

    private Context context;
    private DraggableTextView textView;
    private OnTransformChangedListener listener;

    public interface OnTransformChangedListener {
        void onTransformChanged();
    }

    public TextTransformDialog(Context context) {
        this.context = context;
    }

    public void show(DraggableTextView textView, OnTransformChangedListener listener) {
        this.textView = textView;
        this.listener = listener;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_text_transform, null);
        builder.setView(dialogView);

        // 获取控件
        SeekBar scaleSeekBar = dialogView.findViewById(R.id.scaleSeekBar);
        TextView scaleValue = dialogView.findViewById(R.id.scaleValue);
        SeekBar rotateSeekBar = dialogView.findViewById(R.id.rotateSeekBar);
        TextView rotateValue = dialogView.findViewById(R.id.rotateValue);
        Button btnReset = dialogView.findViewById(R.id.btnReset);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnApply = dialogView.findViewById(R.id.btnApply);

        AlertDialog dialog = builder.create();

        // 初始化当前值
        if (textView != null) {
            // 缩放：0-300% 对应 SeekBar 0-300
            float currentScale = textView.getScaleFactor();
            scaleSeekBar.setProgress((int) (currentScale * 100));
            scaleValue.setText(String.format("%.0f%%", currentScale * 100));

            // 旋转：0-360度 对应 SeekBar 0-360
            float currentRotation = textView.getRotationAngle();
            rotateSeekBar.setProgress((int) currentRotation);
            rotateValue.setText(String.format("%.0f°", currentRotation));
        }

        // 缩放调节监听
        scaleSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (textView != null && fromUser) {
                    float scale = progress / 100f; // 转换为 0.5-3.0 范围
                    scale = Math.max(0.5f, Math.min(scale, 3.0f));
                    textView.setScaleFactor(scale);
                    scaleValue.setText(String.format("%.0f%%", scale * 100));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 旋转调节监听
        rotateSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (textView != null && fromUser) {
                    textView.setRotationAngle(progress);
                    rotateValue.setText(String.format("%.0f°", (float) progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 重置按钮
        btnReset.setOnClickListener(v -> {
            if (textView != null) {
                textView.setScaleFactor(1.0f);
                textView.setRotationAngle(0.0f);
                scaleSeekBar.setProgress(100);
                rotateSeekBar.setProgress(0);
                scaleValue.setText("100%");
                rotateValue.setText("0°");
            }
        });

        // 取消按钮
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // 应用按钮
        btnApply.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTransformChanged();
            }
            dialog.dismiss();
        });

        dialog.show();
    }
}