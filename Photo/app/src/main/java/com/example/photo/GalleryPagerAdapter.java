package com.example.photo;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class GalleryPagerAdapter extends FragmentStateAdapter {

    private AllImagesFragment allImagesFragment;
    private FolderImagesFragment folderImagesFragment;

    public GalleryPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public void setupFragments() {
        allImagesFragment = new AllImagesFragment();
        folderImagesFragment = new FolderImagesFragment();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return allImagesFragment != null ? allImagesFragment : new AllImagesFragment();
        } else {
            return folderImagesFragment != null ? folderImagesFragment : new FolderImagesFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}