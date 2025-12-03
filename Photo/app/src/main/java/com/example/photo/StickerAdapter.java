package com.example.photo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.List;

public class StickerAdapter extends BaseAdapter {

    private Context context;
    private List<StickerItem> stickerItems;
    private LayoutInflater inflater;

    public StickerAdapter(Context context, List<StickerItem> stickerItems) {
        this.context = context;
        this.stickerItems = stickerItems;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return stickerItems != null ? stickerItems.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return stickerItems != null ? stickerItems.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_sticker, parent, false);
            holder = new ViewHolder();
            holder.imageView = convertView.findViewById(R.id.stickerImage);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        StickerItem stickerItem = stickerItems.get(position);
        if (stickerItem != null) {
            holder.imageView.setImageResource(stickerItem.getResId());
        }

        return convertView;
    }

    static class ViewHolder {
        ImageView imageView;
    }
}