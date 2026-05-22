package com.example.tabletopconnect;

import android.content.Intent;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MapFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Find the Host Card and launch HostTableActivity
        com.google.android.material.card.MaterialCardView hostCard = view.findViewById(R.id.cardHostTable);

        hostCard.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), HostTableActivity.class);
            startActivity(intent);
        });

        return view;
    }
}