package com.example.photo;

import android.net.Uri;

public class ImageItem {
    private long id;
    private String name;
    private Uri uri;
    private String path;
    private long dateAdded;
    private long size;
    private String bucketName; // 添加文件夹名称字段

    public ImageItem(long id, String name, Uri uri, String path, long dateAdded, long size) {
        this.id = id;
        this.name = name;
        this.uri = uri;
        this.path = path;
        this.dateAdded = dateAdded;
        this.size = size;
    }

    // 新构造函数，包含文件夹名称
    public ImageItem(long id, String name, Uri uri, String path, long dateAdded, long size, String bucketName) {
        this.id = id;
        this.name = name;
        this.uri = uri;
        this.path = path;
        this.dateAdded = dateAdded;
        this.size = size;
        this.bucketName = bucketName;
    }

    // Getters
    public long getId() { return id; }
    public String getName() { return name; }
    public Uri getUri() { return uri; }
    public String getPath() { return path; }
    public long getDateAdded() { return dateAdded; }
    public long getSize() { return size; }
    public String getBucketName() { return bucketName; }

    public String getFormattedSize() {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return (size / 1024) + " KB";
        } else {
            return (size / (1024 * 1024)) + " MB";
        }
    }
}