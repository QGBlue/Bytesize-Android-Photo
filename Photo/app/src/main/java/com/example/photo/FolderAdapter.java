package com.example.photo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {

    private Context context;
    private List<FolderItem> folderList;
    private OnFolderClickListener listener;

    public FolderAdapter(Context context, List<FolderItem> folderList, OnFolderClickListener listener) {
        this.context = context;
        this.folderList = folderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_folder, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FolderItem folderItem = folderList.get(position);

        holder.folderName.setText(folderItem.getName());
        holder.imageCount.setText(folderItem.getImageCount() + " 张图片");

        // 加载文件夹的第一张图片作为封面
        Glide.with(context)
                .load(folderItem.getFirstImageUri())
                .override(100, 100)
                .centerCrop()
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_dialog_alert)
                .into(holder.coverImage);

        // 点击文件夹可以查看该文件夹下的所有图片
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFolderClick(folderItem);
            }
        });
    }

    @Override
    public int getItemCount() {
        return folderList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView coverImage;
        TextView folderName;
        TextView imageCount;

        ViewHolder(View itemView) {
            super(itemView);
            coverImage = itemView.findViewById(R.id.cover_image);
            folderName = itemView.findViewById(R.id.folder_name);
            imageCount = itemView.findViewById(R.id.image_count);
        }
    }

    public interface OnFolderClickListener {
        void onFolderClick(FolderItem folderItem);
    }
}