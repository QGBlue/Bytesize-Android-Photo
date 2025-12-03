package com.example.photo;

import android.net.Uri;

public class FolderItem {
    private String name;
    private String path;
    private Uri firstImageUri;
    private int imageCount;

    public FolderItem(String name, String path, Uri firstImageUri, int imageCount) {
        this.name = name;
        this.path = path;
        this.firstImageUri = firstImageUri;
        this.imageCount = imageCount;
    }

    // Getters
    public String getName() { return name; }
    public String getPath() { return path; }
    public Uri getFirstImageUri() { return firstImageUri; }
    public int getImageCount() { return imageCount; }

    // Setters
    public void setImageCount(int imageCount) { this.imageCount = imageCount; }
}