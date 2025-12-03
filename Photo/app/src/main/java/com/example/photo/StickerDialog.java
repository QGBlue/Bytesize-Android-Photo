// StickerDialog.java (简版)
package com.example.photo;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class StickerDialog {

    private Context context;
    private AlertDialog dialog;
    private OnStickerSelectedListener listener;

    private List<StickerItem> stickerItems = new ArrayList<>();

    public interface OnStickerSelectedListener {
        void onStickerSelected(StickerItem stickerItem);
    }

    public StickerDialog(Context context) {
        this.context = context;
        initStickers();
    }

    private void initStickers() {
        // 初始化10个贴纸
        stickerItems.add(new StickerItem("heart", R.drawable.avator_1));
        stickerItems.add(new StickerItem("star", R.drawable.avator_2));
        stickerItems.add(new StickerItem("smile", R.drawable.avator_3));
        stickerItems.add(new StickerItem("arrow", R.drawable.avator_4));
        stickerItems.add(new StickerItem("flower", R.drawable.avator_5));
        stickerItems.add(new StickerItem("music", R.drawable.avator_6));
        stickerItems.add(new StickerItem("sun", R.drawable.avator_7));
        stickerItems.add(new StickerItem("moon", R.drawable.avator_8));
        stickerItems.add(new StickerItem("crown", R.drawable.avator_9));
        stickerItems.add(new StickerItem("bubble", R.drawable.avator_10));
    }

    public void show(OnStickerSelectedListener listener) {
        this.listener = listener;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_stickers, null);
        builder.setView(dialogView);
        builder.setTitle("选择贴纸");
        builder.setNegativeButton("取消", null);

        GridView gridView = dialogView.findViewById(R.id.stickerGrid);
        StickerAdapter adapter = new StickerAdapter(context, stickerItems);
        gridView.setAdapter(adapter);

        gridView.setOnItemClickListener((parent, view, position, id) -> {
            if (listener != null) {
                listener.onStickerSelected(stickerItems.get(position));
            }
            dialog.dismiss();
        });

        dialog = builder.create();
        dialog.show();
    }
}