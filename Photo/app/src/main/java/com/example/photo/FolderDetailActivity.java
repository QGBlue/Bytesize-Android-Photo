package com.example.photo;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.photo.ImageItem;

import java.util.ArrayList;
import java.util.List;

public class FolderDetailActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private GalleryAdapter adapter;
    private List<ImageItem> imageList = new ArrayList<>();
    private String folderName;
    private String folderPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_detail);

        // 获取传递的文件夹信息
        folderName = getIntent().getStringExtra("folder_name");
        folderPath = getIntent().getStringExtra("folder_path");

        initViews();
        loadFolderImages();
    }

    private void initViews() {
        // 设置工具栏
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(folderName);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        adapter = new GalleryAdapter(this, imageList, new GalleryAdapter.OnImageClickListener() {
            @Override
            public void onImageClick(ImageItem imageItem) {
                openEditActivity(imageItem.getUri());
            }

            @Override
            public void onImageLongClick(ImageItem imageItem) {
                showImagePreview(imageItem.getUri());
            }
        });

        recyclerView.setAdapter(adapter);

        // 设置空视图
        View emptyView = findViewById(R.id.empty_view);
        adapter.setEmptyView(emptyView);
    }

    private void loadFolderImages() {
        new Thread(() -> {
            List<ImageItem> images = getImagesByFolder();
            runOnUiThread(() -> {
                imageList.clear();
                imageList.addAll(images);
                adapter.notifyDataSetChanged();

                if (images.isEmpty()) {
                    findViewById(R.id.empty_view).setVisibility(View.VISIBLE);
                } else {
                    findViewById(R.id.empty_view).setVisibility(View.GONE);
                }
            });
        }).start();
    }

    private List<ImageItem> getImagesByFolder() {
        List<ImageItem> images = new ArrayList<>();

        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.SIZE
        };

        // 根据文件夹路径筛选图片
        String selection = MediaStore.Images.Media.DATA + " LIKE ?";
        String[] selectionArgs = new String[]{folderPath + "%"};

        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder)) {

            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);
                int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    String data = cursor.getString(dataColumn);
                    long dateAdded = cursor.getLong(dateColumn);
                    long size = cursor.getLong(sizeColumn);

                    Uri contentUri = Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            Long.toString(id));

                    ImageItem imageItem = new ImageItem(id, name, contentUri, data, dateAdded, size);
                    images.add(imageItem);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() ->
                    Toast.makeText(this, "加载文件夹图片失败", Toast.LENGTH_SHORT).show());
        }

        return images;
    }

    private void openEditActivity(Uri imageUri) {
        Intent intent = new Intent(this, EditImageActivity.class);
        intent.putExtra("image_uri", imageUri.toString());
        startActivity(intent);
    }

    private void showImagePreview(Uri imageUri) {
        Intent intent = new Intent(this, ImageDetailActivity.class);
        intent.putExtra("image_uri", imageUri.toString());
        startActivity(intent);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
