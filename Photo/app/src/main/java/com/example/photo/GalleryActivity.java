package com.example.photo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class GalleryActivity extends AppCompatActivity {

    private static final String TAG = "GalleryActivity";
    private static final int PERMISSION_REQUEST_READ_EXTERNAL_STORAGE = 1;

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private GalleryPagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        initViews();
        checkPermissions();
    }

    private void initViews() {
        // 设置工具栏
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("选择图片");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);

        // 初始化适配器
        pagerAdapter = new GalleryPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        // 设置Tab标题 - 移除内容描述设置
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            if (position == 0) {
                tab.setText("所有图片");
            } else {
                tab.setText("文件夹");
            }
        }).attach();
        // 在Tab创建后设置内容描述
        setupTabContentDescriptions();
    }
    private void setupTabContentDescriptions() {
        // 等待TabLayout完成布局后再设置内容描述
        tabLayout.post(() -> {
            ViewGroup tabStrip = (ViewGroup) tabLayout.getChildAt(0);
            if (tabStrip != null && tabStrip.getChildCount() >= 2) {
                View tabView1 = tabStrip.getChildAt(0);
                View tabView2 = tabStrip.getChildAt(1);

                if (tabView1 != null) {
                    tabView1.setContentDescription("显示所有图片的选项卡");
                }
                if (tabView2 != null) {
                    tabView2.setContentDescription("按文件夹分类显示图片的选项卡");
                }
            }
        });
    }
    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            // 权限已授予，设置ViewPager数据
            pagerAdapter.setupFragments();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，设置ViewPager数据
                pagerAdapter.setupFragments();
            } else {
                // 权限被拒绝
                Toast.makeText(this, "需要存储权限来访问相册", Toast.LENGTH_LONG).show();
                findViewById(R.id.empty_view).setVisibility(View.VISIBLE);
            }
        }
    }

    public void openEditActivity(Uri imageUri) {
        Intent intent = new Intent(this, EditImageActivity.class);
        intent.putExtra("image_uri", imageUri.toString());
        startActivity(intent);
    }

    public void showImagePreview(Uri imageUri) {
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