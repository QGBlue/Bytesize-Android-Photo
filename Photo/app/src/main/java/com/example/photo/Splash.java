package com.example.photo;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.photo.MainActivity;
import com.example.photo.R;

/**
 * create by WUzejian on 2025/11/20
 */
public class Splash extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);
        // 模拟启动延迟
        new Handler().postDelayed(() -> {
            // 启动主界面
            Intent intent = new Intent(Splash.this, MainActivity.class);
            startActivity(intent);
            // 关闭启动页
            finish();
        }, 2000); // 2秒延迟
    }


}
