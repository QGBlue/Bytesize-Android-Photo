package com.example.photo;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class TextStyleDialog {

    private Context context;
    private DraggableTextView textView;
    private OnStyleChangedListener listener;

    // 预设颜色
    private String[] presetColors = {
            "#FFFFFF", // 白色
            "#000000", // 黑色
            "#FF0000", // 红色
            "#00FF00", // 绿色
            "#0000FF", // 蓝色
            "#FFFF00", // 黄色
            "#FF00FF", // 紫色
            "#00FFFF", // 青色
            "#FF9800", // 橙色
            "#795548"  // 棕色
    };

    public interface OnStyleChangedListener {
        void onStyleChanged(DraggableTextView textView);
    }

    public TextStyleDialog(Context context) {
        this.context = context;
    }

    public void show(DraggableTextView textView, OnStyleChangedListener listener) {
        this.textView = textView;
        this.listener = listener;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_text_style, null);
        builder.setView(dialogView);

        // 获取控件
        EditText editText = dialogView.findViewById(R.id.editText);
        Spinner fontSpinner = dialogView.findViewById(R.id.fontSpinner);
        SeekBar sizeSeekBar = dialogView.findViewById(R.id.sizeSeekBar);
        LinearLayout colorContainer = dialogView.findViewById(R.id.colorContainer);
        SeekBar opacitySeekBar = dialogView.findViewById(R.id.opacitySeekBar);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        Button btnApply = dialogView.findViewById(R.id.btnApply);

        // 初始化文本
        if (textView != null) {
            editText.setText(textView.getText());

            // 设置字体选择
            String currentFont = textView.getCurrentFont();
            String[] fonts = {"默认", "粗体", "衬线体", "无衬线体", "等宽字体"};
            ArrayAdapter<String> fontAdapter = new ArrayAdapter<>(context,
                    android.R.layout.simple_spinner_item, fonts);
            fontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            fontSpinner.setAdapter(fontAdapter);

            // 设置选中当前字体
            int fontPosition = getFontPosition(currentFont);
            fontSpinner.setSelection(fontPosition);

            // 设置字号
            float currentSize = textView.getCurrentSize();
            sizeSeekBar.setMax(24); // 12-36，减12作为基数
            sizeSeekBar.setProgress((int) currentSize - 12);

            // 设置透明度
            float currentOpacity = textView.getCurrentOpacity();
            opacitySeekBar.setMax(50); // 50-100，减50作为基数
            opacitySeekBar.setProgress((int) (currentOpacity * 100) - 50);
        }

        AlertDialog dialog = builder.create();

        // 字体选择监听
        fontSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (textView != null) {
                    String font = getFontFromPosition(position);
                    textView.setTextFont(font);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 字号调节监听
        sizeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (textView != null) {
                    float size = 12 + progress; // 12-36
                    textView.setTextSizeValue(size);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 创建颜色选择器
        createColorButtons(colorContainer);

        // 透明度调节监听
        opacitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (textView != null) {
                    float opacity = 0.5f + (progress / 100.0f); // 0.5-1.0
                    textView.setTextOpacity(opacity);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 文本变化监听
        editText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (textView != null) {
                    textView.setText(s.toString());
                }
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        // 按钮事件
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnApply.setOnClickListener(v -> {
            if (listener != null && textView != null) {
                listener.onStyleChanged(textView);
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private int getFontPosition(String font) {
        switch (font) {
            case "default": return 0;
            case "bold": return 1;
            case "serif": return 2;
            case "sans": return 3;
            case "monospace": return 4;
            default: return 0;
        }
    }

    private String getFontFromPosition(int position) {
        switch (position) {
            case 0: return "default";
            case 1: return "bold";
            case 2: return "serif";
            case 3: return "sans";
            case 4: return "monospace";
            default: return "default";
        }
    }

    private void createColorButtons(LinearLayout container) {
        container.removeAllViews();

        // 每行显示5个颜色
        int colorsPerRow = 5;
        LinearLayout rowLayout = null;

        for (int i = 0; i < presetColors.length; i++) {
            if (i % colorsPerRow == 0) {
                rowLayout = new LinearLayout(context);
                rowLayout.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                container.addView(rowLayout);
            }

            final String colorHex = presetColors[i];
            Button colorButton = new Button(context);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, // 宽度
                    dpToPx(40) // 高度40dp
            );
            params.weight = 1;
            params.setMargins(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2));

            colorButton.setLayoutParams(params);
            colorButton.setBackgroundColor(Color.parseColor(colorHex));
            colorButton.setTag(colorHex);

            // 如果是白色，显示边框以便识别
            if (colorHex.equals("#FFFFFF")) {
                colorButton.setBackgroundResource(R.drawable.color_button_border);
            }

            colorButton.setOnClickListener(v -> {
                if (textView != null) {
                    String selectedColor = (String) v.getTag();
                    textView.setTextColorHex(selectedColor);
                }
            });

            if (rowLayout != null) {
                rowLayout.addView(colorButton);
            }
        }
    }

    private int dpToPx(int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}