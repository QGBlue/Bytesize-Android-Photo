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
import com.example.photo.ImageItem;

import java.util.List;

public class GalleryAdapter extends RecyclerView.Adapter<GalleryAdapter.ViewHolder> {

    private Context context;
    private List<ImageItem> imageList;
    private OnImageClickListener listener;
    private View emptyView;

    public GalleryAdapter(Context context, List<ImageItem> imageList, OnImageClickListener listener) {
        this.context = context;
        this.imageList = imageList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gallery_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ImageItem imageItem = imageList.get(position);

        // 设置图片的内容描述（无障碍功能）
        holder.imageView.setContentDescription("图片: " + imageItem.getName());

        // 使用Glide加载图片缩略图
        Glide.with(context)
                .load(imageItem.getUri())
                .override(300, 300) // 缩略图大小
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_foreground) // 临时使用系统图标
                .error(android.R.drawable.ic_dialog_alert) // 错误图标
                .into(holder.imageView);

        // 设置点击事件
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageClick(imageItem);
            }
        });

        // 设置长按事件
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onImageLongClick(imageItem);
            }
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    public void setEmptyView(View emptyView) {
        this.emptyView = emptyView;
        updateEmptyView();
    }

    private void updateEmptyView() {
        if (emptyView != null) {
            emptyView.setVisibility(getItemCount() == 0 ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public void onViewRecycled(@NonNull ViewHolder holder) {
        super.onViewRecycled(holder);
        // 清理Glide加载
        Glide.with(context).clear(holder.imageView);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }

    public interface OnImageClickListener {
        void onImageClick(ImageItem imageItem);
        void onImageLongClick(ImageItem imageItem);
    }
}