package com.example.tabletopconnect;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

public class ProfileFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // --- DYNAMIC AVATAR LOGIC ---
        ImageView avatarImage = view.findViewById(R.id.profileAvatarImageView);
        double mockReputationScore = 4.9; // Try changing this to 3.5 or 1.2 to see it swap!

        if (mockReputationScore >= 4.0) {
            avatarImage.setImageResource(R.drawable.great);
        } else if (mockReputationScore >= 2.5) {
            avatarImage.setImageResource(R.drawable.happy);
        } else {
            avatarImage.setImageResource(R.drawable.sad);
        }

        // --- ACCOUNT SETTINGS BUTTON LOGIC ---
        Button settingsBtn = view.findViewById(R.id.settingsButton);
        settingsBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(intent);
        });

        // --- LOGOUT BUTTON LOGIC ---
        Button logoutBtn = view.findViewById(R.id.logoutButton);
        logoutBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
            if (getActivity() != null) {
                getActivity().finish();
            }
        });

        return view;
    }
}