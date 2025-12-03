package com.example.photo;

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
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AllImagesFragment extends Fragment {

    private RecyclerView recyclerView;
    private GalleryAdapter adapter;
    private List<ImageItem> imageList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_gallery, container, false);
        initViews(view);
        loadImages();
        return view;
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        adapter = new GalleryAdapter(getContext(), imageList, new GalleryAdapter.OnImageClickListener() {
            @Override
            public void onImageClick(ImageItem imageItem) {
                if (getActivity() instanceof GalleryActivity) {
                    ((GalleryActivity) getActivity()).openEditActivity(imageItem.getUri());
                }
            }

            @Override
            public void onImageLongClick(ImageItem imageItem) {
                if (getActivity() instanceof GalleryActivity) {
                    ((GalleryActivity) getActivity()).showImagePreview(imageItem.getUri());
                }
            }
        });

        recyclerView.setAdapter(adapter);
    }

    private void loadImages() {
        new Thread(() -> {
            List<ImageItem> images = getAllImages();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    imageList.clear();
                    imageList.addAll(images);
                    adapter.notifyDataSetChanged();
                });
            }
        }).start();
    }

    private List<ImageItem> getAllImages() {
        List<ImageItem> images = new ArrayList<>();

        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        };

        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = getActivity().getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder)) {

            if (cursor != null) {
                int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED);
                int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
                int bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);

                while (cursor.moveToNext()) {
                    long id = cursor.getLong(idColumn);
                    String name = cursor.getString(nameColumn);
                    String data = cursor.getString(dataColumn);
                    long dateAdded = cursor.getLong(dateColumn);
                    long size = cursor.getLong(sizeColumn);
                    String bucketName = cursor.getString(bucketColumn);

                    Uri contentUri = Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            Long.toString(id));

                    ImageItem imageItem = new ImageItem(id, name, contentUri, data, dateAdded, size, bucketName);
                    images.add(imageItem);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return images;
    }
}