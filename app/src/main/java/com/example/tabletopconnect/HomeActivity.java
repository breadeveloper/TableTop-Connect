package com.example.tabletopconnect;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.appbar.MaterialToolbar;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Find the top toolbar we created in the XML
        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);

// 2. Set up a click listener so the bell actually does something when tapped
        topAppBar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_notifications) {
                // TODO: Open the notifications screen or popup

                // For testing right now, we will simulate reading the notifications
                // by forcing the icon back to the empty bell when clicked
                item.setIcon(R.drawable.bell);
                return true;
            }
            return false;
        });

        // --- TESTING THE TOGGLE ---
        // We can manually trigger the red dot to show up by finding the item
        // and changing the icon programmatically.
        // (Later, Firebase will trigger this automatically).
        MenuItem bellItem = topAppBar.getMenu().findItem(R.id.action_notifications);
        boolean hasUnreadNotifications = true; // Pretend we checked the database

        if (hasUnreadNotifications) {
            bellItem.setIcon(R.drawable.notification);
        } else {
            bellItem.setIcon(R.drawable.bell);
        }

        // 1. Find the Views
        androidx.viewpager2.widget.ViewPager2 viewPager = findViewById(R.id.viewPager);
        com.google.android.material.tabs.TabLayout tabLayout = findViewById(R.id.tabLayout);

// 2. Set up the Adapter
        ViewPagerAdapter pagerAdapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

// 3. The Glue: Attach the TabLayout to the ViewPager
        new com.google.android.material.tabs.TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {
                    // Set the text for the tabs based on their position
                    switch (position) {
                        case 0:
                            tab.setText("My Sessions");
                            break;
                        case 1:
                            tab.setText("Map");
                            break;
                        case 2:
                            tab.setText("Profile");
                            break;
                    }
                }
        ).attach();
    }
}