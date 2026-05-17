package com.bread.isat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AchievementsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_achievements);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Achievements");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Handle insets to fix the "fullscreen" overlap with the status bar
        // This matches the manual inset handling in MainActivity
        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            mlp.topMargin = insets.top;
            v.setLayoutParams(mlp);

            return windowInsets;
        });

        ProgressBar progressBar = findViewById(R.id.progress_bar);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Handle bottom insets for the navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(recyclerView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), insets.bottom);
            return windowInsets;
        });

        loadAchievements(progressBar, recyclerView);
    }

    private void loadAchievements(ProgressBar progressBar, RecyclerView recyclerView) {
        String directory = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.preference_directory), null);
        if (directory == null) {
            Toast.makeText(this, "Game directory not set", Toast.LENGTH_SHORT).show();
            return;
        }

        Handler mainHandler = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            try {
                JSONObject unlockedAchievements = null;
                List<File> possibleSaveFilesList = new ArrayList<>();
                possibleSaveFilesList.add(new File(directory, "save/nwcompat.json"));
                possibleSaveFilesList.add(new File(directory, "www/save/nwcompat.json"));
                File parent = new File(directory).getParentFile();
                if (parent != null) {
                    possibleSaveFilesList.add(new File(parent, "save/nwcompat.json"));
                }

                for (File saveFile : possibleSaveFilesList) {
                    if (saveFile.exists()) {
                        try {
                            String content = new String(Files.readAllBytes(saveFile.toPath()), StandardCharsets.UTF_8);
                            JSONObject json = new JSONObject(content);
                            unlockedAchievements = json.optJSONObject("achievements");
                            if (unlockedAchievements != null) break;
                        } catch (Exception e) {
                            Debug.i().log(Log.WARN, "Failed to read save file %s: %s", saveFile, e);
                        }
                    }
                }

                final JSONObject finalUnlocked = unlockedAchievements;
                List<Achievement> achievements = new ArrayList<>();
                int[] unlockedCount = {0};

                try (InputStream is = getAssets().open("1677310.db.txt");
                     BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                    String content = reader.lines().collect(Collectors.joining("\n"));
                    JSONObject dbJson = new JSONObject(content);
                    JSONArray achievementList = dbJson.getJSONObject("achievement").getJSONArray("list");

                    for (int i = 0; i < achievementList.length(); i++) {
                        JSONObject ach = achievementList.getJSONObject(i);
                        String id = ach.getString("name");

                        if (!BuildConfig.DEBUG && id.equals("TEST_ACHIEVEMENT")) continue;

                        String title = ach.getString("displayName");
                        String description = ach.getString("description");
                        String iconUrl = ach.getString("icon");
                        String iconGrayUrl = ach.getString("icongray");
                        boolean isHidden = ach.optInt("hidden", 0) == 1;

                        boolean isUnlocked = false;
                        if (finalUnlocked != null) {
                            Object val = finalUnlocked.opt(id);
                            if (val instanceof Boolean)
                                isUnlocked = (Boolean) val;
                            else if (val instanceof Number)
                                isUnlocked = ((Number) val).intValue() != 0;
                            else if (val instanceof String)
                                isUnlocked = ((String) val).equalsIgnoreCase("true") || ((String) val).equals("1");
                        }

                        if (isUnlocked) unlockedCount[0]++;

                        if (iconUrl != null && !iconUrl.startsWith("http") && !iconUrl.startsWith("/")) {
                            File iconFile = new File(directory, iconUrl);
                            if (iconFile.exists()) iconUrl = iconFile.getAbsolutePath();
                        }
                        if (iconGrayUrl != null && !iconGrayUrl.startsWith("http") && !iconGrayUrl.startsWith("/")) {
                            File iconFile = new File(directory, iconGrayUrl);
                            if (iconFile.exists()) iconGrayUrl = iconFile.getAbsolutePath();
                        }

                        achievements.add(new Achievement(id, title, description, iconUrl, iconGrayUrl, isHidden, isUnlocked));
                    }
                }

                final List<Achievement> finalAchievements = achievements;
                final int total = achievements.size();
                final int unlocked = unlockedCount[0];

                mainHandler.post(() -> {
                    if (isFinishing()) return;
                    progressBar.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    if (getSupportActionBar() != null) {
                        getSupportActionBar().setSubtitle(String.format("%d / %d Unlocked", unlocked, total));
                    }
                    recyclerView.setAdapter(new AchievementAdapter(finalAchievements));
                });
            } catch (Exception e) {
                Debug.i().log(Log.ERROR, "Failed to load achievements: %s", e);
                mainHandler.post(() -> {
                    if (isFinishing()) return;
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load achievements: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
}
