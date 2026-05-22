package com.example.tabletopconnect;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        // The tabs are indexed starting at 0, just like an Array
        switch (position) {
            case 0:
                return new MySessionsFragment();
            case 1:
                return new MapFragment();
            case 2:
                return new ProfileFragment();
            default:
                return new MySessionsFragment();
        }
    }

    @Override
    public int getItemCount() {
        // We have exactly 3 tabs
        return 3;
    }
}