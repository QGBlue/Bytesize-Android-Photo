package com.example.photo;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FolderImagesFragment extends Fragment {

    private RecyclerView recyclerView;
    private FolderAdapter adapter;
    private List<FolderItem> folderList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_folders, container, false);
        initViews(view);
        loadFolders();
        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new FolderAdapter(getContext(), folderList, new FolderAdapter.OnFolderClickListener() {
            @Override
            public void onFolderClick(FolderItem folderItem) {
                // 跳转到文件夹详情页面
                openFolderDetail(folderItem);
            }
        });

        recyclerView.setAdapter(adapter);
    }

    private void loadFolders() {
        new Thread(() -> {
            List<FolderItem> folders = getFolders();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    folderList.clear();
                    folderList.addAll(folders);
                    adapter.notifyDataSetChanged();
                });
            }
        }).start();
    }

    private List<FolderItem> getFolders() {
        Map<String, FolderItem> folderMap = new HashMap<>();

        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
        };

        String sortOrder = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " ASC";

        try (Cursor cursor = getActivity().getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder)) {

            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                int bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String data = cursor.getString(dataColumn);
                    String bucketName = cursor.getString(bucketColumn);
                    long dateAdded = cursor.getLong(dateColumn);

                    Uri contentUri = Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            Long.toString(id));

                    // 获取文件夹路径
                    String folderPath = data.substring(0, data.lastIndexOf("/"));

                    if (!folderMap.containsKey(bucketName)) {
                        FolderItem folderItem = new FolderItem(bucketName, folderPath, contentUri, 1);
                        folderMap.put(bucketName, folderItem);
                    } else {
                        FolderItem folderItem = folderMap.get(bucketName);
                        folderItem.setImageCount(folderItem.getImageCount() + 1);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new ArrayList<>(folderMap.values());
    }

    private void openFolderDetail(FolderItem folderItem) {
        Intent intent = new Intent(getActivity(), FolderDetailActivity.class);
        intent.putExtra("folder_name", folderItem.getName());
        intent.putExtra("folder_path", folderItem.getPath());
        startActivity(intent);
    }
}