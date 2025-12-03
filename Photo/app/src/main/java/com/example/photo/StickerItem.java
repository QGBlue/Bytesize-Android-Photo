package com.example.photo;

public class StickerItem {

    private String name;        // 贴纸名称
    private int resId;          // 资源ID
    private String category;    // 贴纸分类（可选）
    private boolean isFavorite; // 是否收藏（可选）

    public StickerItem(String name, int resId) {
        this(name, resId, "default", false);
    }

    public StickerItem(String name, int resId, String category) {
        this(name, resId, category, false);
    }

    public StickerItem(String name, int resId, String category, boolean isFavorite) {
        this.name = name;
        this.resId = resId;
        this.category = category;
        this.isFavorite = isFavorite;
    }

    // Getters 和 Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getResId() {
        return resId;
    }

    public void setResId(int resId) {
        this.resId = resId;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isFavorite() {
        return isFavorite;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    @Override
    public String toString() {
        return "StickerItem{" +
                "name='" + name + '\'' +
                ", resId=" + resId +
                ", category='" + category + '\'' +
                ", isFavorite=" + isFavorite +
                '}';
    }
}