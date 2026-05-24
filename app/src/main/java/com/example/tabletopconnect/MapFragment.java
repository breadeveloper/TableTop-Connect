package com.example.tabletopconnect;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.card.MaterialCardView;

public class MapFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // 1. Setup the Host Table Card
        MaterialCardView hostCard = view.findViewById(R.id.cardHostTable);
        hostCard.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), HostTableActivity.class);
            startActivity(intent);
        });

        // 2. Setup the Find Table / Game Radar Card (FIXED ID HERE)
        MaterialCardView findCard = view.findViewById(R.id.cardFindTable);
        findCard.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), FindTableActivity.class);
            startActivity(intent);
        });

        return view;
    }
}