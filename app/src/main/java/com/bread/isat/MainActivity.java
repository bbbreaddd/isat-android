package com.bread.isat;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            new Debug(getFilesDir().getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Fatal error: failed to init logging", Toast.LENGTH_LONG).show();
            finish();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button playButton = findViewById(R.id.button_play);

        ViewCompat.setOnApplyWindowInsetsListener(playButton, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.mandatorySystemGestures());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.bottomMargin = insets.bottom;
            v.setLayoutParams(mlp);

            return WindowInsetsCompat.CONSUMED;
        });

        var fragment = new SettingsFragment();

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_preferences, fragment)
                .commit();

        fragment.setOnPreferencesUpdateListener((preferences) -> {
            playButton.setEnabled(fragment.canPlay(this, preferences));
        });

        View framePreferences = findViewById(R.id.frame_preferences);

        ViewCompat.setOnApplyWindowInsetsListener(framePreferences, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.topMargin = insets.top;
            v.setLayoutParams(mlp);

            return WindowInsetsCompat.CONSUMED;
        });

        playButton.setEnabled(fragment.canPlay(this, PreferenceManager.getDefaultSharedPreferences(this)));
        playButton.setOnClickListener(v -> {
            startActivity(new Intent(this, GameActivity.class));
            finish();
        });

        // Precache achievement icons over the network in the background
        new Thread(() -> {
            try (java.io.InputStream is = getAssets().open("1677310.db.txt");
                 java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(is, java.nio.charset.StandardCharsets.UTF_8))) {
                
                String content = reader.lines().collect(java.util.stream.Collectors.joining("\n"));
                org.json.JSONObject dbJson = new org.json.JSONObject(content);
                org.json.JSONArray achievementList = dbJson.getJSONObject("achievement").getJSONArray("list");

                for (int i = 0; i < achievementList.length(); i++) {
                    org.json.JSONObject ach = achievementList.getJSONObject(i);
                    String iconUrl = ach.optString("icon", null);
                    String iconGrayUrl = ach.optString("icongray", null);

                    if (iconUrl != null && iconUrl.startsWith("http")) {
                        com.bumptech.glide.Glide.with(getApplicationContext()).downloadOnly().load(iconUrl).submit();
                    }
                    if (iconGrayUrl != null && iconGrayUrl.startsWith("http")) {
                        com.bumptech.glide.Glide.with(getApplicationContext()).downloadOnly().load(iconGrayUrl).submit();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
