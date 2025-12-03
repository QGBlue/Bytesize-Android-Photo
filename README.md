# 📸 图片编辑App

一个功能全面的Android图片编辑应用程序，支持从相机拍摄或相册选择图片，进行多种编辑操作，并保存分享。

## ✨ 功能特性

### 📁 图片管理
- ✅ 相册浏览（所有图片 + 文件夹分类）
- ✅ 相机拍摄（支持前后摄像头切换）
- ✅ 大图预览

### 🎨 编辑功能
- ✅ **基础调整**：亮度、对比度调节
- ✅ **裁剪功能**：自由裁剪 + 5种比例（1:1、4:3、16:9、3:4、9:16）
- ✅ **旋转翻转**：90°/180°旋转，水平/垂直翻转
- ✅ **文字编辑**：添加文字、样式设置（字体、颜色、大小、透明度）
- ✅ **文字变换**：拖动、缩放、旋转文字
- ✅ **滤镜效果**：10种滤镜（黑白、复古、暖色、冷色等）
- ✅ **贴纸功能**：10+贴纸，支持拖动、缩放、旋转
- ✅ **撤销重做**：滤镜操作的撤销和重做

### 💾 保存分享
- ✅ 添加水印（"训练营"）
- ✅ 保存到相册
- ✅ 直接分享到其他应用
- ✅ 兼容Android 5.0-14系统

## 🛠 技术架构

### 开发环境
- **开发语言**：Java
- **开发工具**：Android Studio
- **目标SDK**：API 34 (Android 14)
- **最低SDK**：API 21 (Android 5.0)

### 主要依赖
```gradle
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.9.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    implementation 'androidx.camera:camera-core:1.3.0'
    implementation 'androidx.camera:camera-camera2:1.3.0'
    implementation 'androidx.camera:camera-view:1.3.0'
    implementation 'com.github.bumptech.glide:glide:4.15.1'
}
📱 功能演示
编辑流程
选择图片 → 相册或相机

基础编辑 → 裁剪、旋转、亮度调节

高级编辑 → 文字、贴纸、滤镜

保存分享 → 添加水印，保存到相册

特色功能
文字编辑：支持实时预览，多种字体颜色

贴纸系统：拖拽缩放旋转，层级管理

滤镜效果：实时预览，撤销重做

相机集成：前后摄像头切换，拍照动画

D:\AndroidSDK\AndroidStudioProjects\Photo\
├── app\src\main\
│   ├── java\com\example\photo\
│   │   ├── AllImagesFragment.java
│   │   ├── CameraActivity.java
│   │   ├── CoordinateConverter.java
│   │   ├── CropHelper.java
│   │   ├── CropOverlayView.java
│   │   ├── DraggableStickerView.java
│   │   ├── DraggableTextView.java
│   │   ├── EditImageActivity.java
│   │   ├── FilterDialog.java
│   │   ├── FilterManager.java
│   │   ├── FilterProcessor.java
│   │   ├── FolderAdapter.java
│   │   ├── FolderDetailActivity.java
│   │   ├── FolderImagesFragment.java
│   │   ├── FolderItem.java
│   │   ├── GalleryActivity.java
│   │   ├── GalleryAdapter.java
│   │   ├── GalleryPagerAdapter.java
│   │   ├── ImageDetailActivity.java
│   │   ├── ImageItem.java
│   │   ├── ImageProcessor.java
│   │   ├── ImageSaver.java
│   │   ├── MainActivity.java
│   │   ├── PermissionHelper.java
│   │   ├── Splash.java
│   │   ├── StickerAdapter.java
│   │   ├── StickerDialog.java
│   │   ├── StickerItem.java
│   │   ├── TextEditDialog.java
│   │   ├── TextStyleDialog.java
│   │   ├── TextTransformDialog.java
│   │   ├── WatermarkUtils.java
│   │   └── ZoomableImageView.java
│   ├── res\  # 布局、图片、字符串等资源文件
│   └── AndroidManifest.xml
