package com.example.photo;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.graphics.Paint;
import android.graphics.Rect;
import com.example.photo.DraggableTextView;

public class TextEditDialog {

    private Context context;
    private DraggableTextView textView;
    private OnTextChangedListener listener;

    public interface OnTextChangedListener {
        void onTextChanged(DraggableTextView textView);
        void onTextRemoved();
    }

    public TextEditDialog(Context context) {
        this.context = context;
    }

    public void show(DraggableTextView textView, OnTextChangedListener listener) {
        this.textView = textView;
        this.listener = listener;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_text_edit, null);
        builder.setView(dialogView);

        EditText editText = dialogView.findViewById(R.id.editText);
        Spinner fontSpinner = dialogView.findViewById(R.id.fontSpinner);
        SeekBar sizeSeekBar = dialogView.findViewById(R.id.sizeSeekBar);
        Button colorButton = dialogView.findViewById(R.id.btnColor);
        SeekBar opacitySeekBar = dialogView.findViewById(R.id.opacitySeekBar);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnOk = dialogView.findViewById(R.id.btnOk);
        Button btnDelete = dialogView.findViewById(R.id.btnDelete);

        // 初始化现有值
        if (textView != null) {
            editText.setText(textView.getText());
            sizeSeekBar.setProgress((int) textView.getTextSize() - 12);
        }

        AlertDialog dialog = builder.create();

        // 文本变化监听
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (textView != null) {
                    textView.setText(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 字号调节
        sizeSeekBar.setMax(24); // 12-36号，减去12的基数
        sizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (textView != null) {
                    float textSize = 12 + progress; // 12-36
                    textView.setTextSize(textSize);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 颜色选择（简化版，后续可以扩展）
        colorButton.setOnClickListener(v -> {
            if (textView != null) {
                textView.setTextColor(Color.WHITE); // 默认白色，可以后续扩展颜色选择器
            }
        });

        // 透明度调节
        opacitySeekBar.setMax(50); // 50%-100%
        opacitySeekBar.setProgress(100);
        opacitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (textView != null) {
                    float alpha = 0.5f + (progress / 100.0f); // 0.5-1.0
                    textView.setAlpha(alpha);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 按钮事件
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnOk.setOnClickListener(v -> {
            if (listener != null && textView != null) {
                listener.onTextChanged(textView);
            }
            dialog.dismiss();
        });

        btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTextRemoved();
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    // 创建新文字的对话框
    public void showNewTextDialog(OnTextChangedListener listener) {
        this.listener = listener;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_text_new, null);
        builder.setView(dialogView);

        EditText editText = dialogView.findViewById(R.id.editText);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnAdd = dialogView.findViewById(R.id.btnAdd);

        AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnAdd.setOnClickListener(v -> {
            String text = editText.getText().toString().trim();
            if (!text.isEmpty()) {
                // 创建新的文字视图
                DraggableTextView newTextView = createNewTextView(text);
                if (listener != null) {
                    listener.onTextChanged(newTextView);
                }
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private DraggableTextView createNewTextView(String text) {
        DraggableTextView textView = new DraggableTextView(context);
        textView.setText(text);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(24);
        textView.setShadowLayer(3, 1, 1, Color.BLACK);

        // 计算文字宽度
        Paint paint = new Paint();
        paint.setTextSize(textView.getTextSize());
        Rect bounds = new Rect();
        paint.getTextBounds(text, 0, text.length(), bounds);

        // 设置初始位置（居中）
        return textView;
    }
}