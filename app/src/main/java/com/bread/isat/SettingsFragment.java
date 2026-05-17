package com.bread.isat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SettingsFragment extends PreferenceFragmentCompat {
    public interface OnPreferencesUpdateListener {
        void onPreferencesUpdate(SharedPreferences preferences);
    }

    private OnPreferencesUpdateListener mListener;
    private Boolean mIsInStarsAndTime = null;

    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener = (preferences, key) -> {
        if (PREFERENCE_DIRECTORY != null && PREFERENCE_DIRECTORY.equals(key)) mIsInStarsAndTime = null;
        updatePreferences(preferences);
    };

    public String PREFERENCE_DIRECTORY;
    public String PREFERENCE_ACHIEVEMENTS;
    public String PREFERENCE_LOGS;
    public String PREFERENCE_LOGS_CLEAR;
    public String PREFERENCE_BORDERS;

    private SharedPreferences mPreferences;
    private ActivityResultLauncher<Uri> mOpenDocumentTree;
    private ActivityResultLauncher<String> mRequestPermission;

    private Activity mActivity;
    private boolean mPendingDirectoryIntent = false;

    public void setOnPreferencesUpdateListener(OnPreferencesUpdateListener listener) {
        mListener = listener;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mActivity = getActivity();

        PREFERENCE_DIRECTORY = getString(R.string.preference_directory);
        PREFERENCE_ACHIEVEMENTS = getString(R.string.preference_achievements);
        PREFERENCE_LOGS = getString(R.string.preference_logs);
        PREFERENCE_LOGS_CLEAR = getString(R.string.preference_logs_clear);
        PREFERENCE_BORDERS = getString(R.string.preference_borders);

        mPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        mPreferences.registerOnSharedPreferenceChangeListener(prefListener);

        mOpenDocumentTree = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), uri -> {
            if (uri == null) return;

            var uriPath = uri.getPath();
            if (uriPath == null) return;

            String[] pathSections = uriPath.split(":");
            Debug.i().log(Log.INFO, "selected directory: %s", uriPath);
            String directory = Environment.getExternalStorageDirectory().getPath() + "/" + pathSections[pathSections.length - 1];

            // Auto-detect www folder if present
            if (Files.exists(Paths.get(directory, "www", "index.html"))) {
                directory = Paths.get(directory, "www").toString();
                Debug.i().log(Log.INFO, "detected www folder: %s", directory);
            }

            if (!Files.exists(Paths.get(directory, "index.html"))) {
                Toast.makeText(context, "Selected directory is not a game directory (index.html not found)", Toast.LENGTH_LONG).show();
                return;
            }

            mPreferences.edit().putString(PREFERENCE_DIRECTORY, directory).apply();
            updatePreferences(mPreferences);
        });

        mRequestPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (!granted) {
                Toast.makeText(mActivity, "Storage permission is required", Toast.LENGTH_LONG).show();
            } else if (mPendingDirectoryIntent) {
                mPendingDirectoryIntent = false;
                mOpenDocumentTree.launch(null);
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mPreferences.unregisterOnSharedPreferenceChangeListener(prefListener);
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferences(mPreferences);

        if (mPendingDirectoryIntent && checkPermissions(mActivity)) {
            mPendingDirectoryIntent = false;
            mOpenDocumentTree.launch(null);
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        Preference directoryPreference = findPreference(PREFERENCE_DIRECTORY);
        Preference achievementsPreference = findPreference(PREFERENCE_ACHIEVEMENTS);
        Preference logsPreference = findPreference(PREFERENCE_LOGS);
        Preference logsClearPreference = findPreference(PREFERENCE_LOGS_CLEAR);
        
        if (directoryPreference != null) {
            directoryPreference.setOnPreferenceClickListener(preference -> {
                if (checkPermissions(mActivity)) {
                    mOpenDocumentTree.launch(null);
                } else {
                    requestPermissions();
                }
                return true;
            });
        }

        if (achievementsPreference != null) {
            achievementsPreference.setOnPreferenceClickListener(preference -> {
                showAchievementsDialog();
                return true;
            });
        }

        if (logsPreference != null) {
            logsPreference.setOnPreferenceClickListener(preference -> {
                Debug.i().save(mActivity);
                return true;
            });
        }

        if (logsClearPreference != null) {
            logsClearPreference.setOnPreferenceClickListener(preference -> {
                Debug.i().clear(mActivity, true);
                Toast.makeText(mActivity, "Logs cleared. A restart is recommended for logging to resume.", Toast.LENGTH_LONG).show();
                return true;
            });
        }

        updatePreferences(mPreferences);
    }

    private void showAchievementsDialog() {
        startActivity(new Intent(mActivity, AchievementsActivity.class));
    }

    private void updatePreferences(SharedPreferences preferences) {
        if (mListener != null) mListener.onPreferencesUpdate(preferences);

        Preference directoryPreference = findPreference(PREFERENCE_DIRECTORY);
        Preference achievementsPreference = findPreference(PREFERENCE_ACHIEVEMENTS);
        Preference logsPreference = findPreference(PREFERENCE_LOGS);
        Preference logsClearPreference = findPreference(PREFERENCE_LOGS_CLEAR);
        Preference bordersPreference = findPreference(PREFERENCE_BORDERS);
        
        String dir = preferences.getString(PREFERENCE_DIRECTORY, null);
        boolean isISAT = isInStarsAndTime(dir);

        if (achievementsPreference != null) {
            achievementsPreference.setVisible(isISAT);
            achievementsPreference.setEnabled(canPlay(mActivity, preferences));
        }

        if (bordersPreference != null) {
            bordersPreference.setVisible(isISAT);
        }

        if (directoryPreference != null) {
            directoryPreference.setSummary(String.format("Current: %s", preferences.getString(PREFERENCE_DIRECTORY, "not set")));
        }

        boolean filesPermission = checkPermissions(mActivity);
        if (logsPreference != null) {
            logsPreference.setEnabled(filesPermission);
        }
        
        if (logsClearPreference != null) {
            logsClearPreference.setEnabled(filesPermission);
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            mPendingDirectoryIntent = true;
            Toast.makeText(mActivity, "Allow all files access and then return to the app", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + BuildConfig.APPLICATION_ID)));
        } else {
            mPendingDirectoryIntent = true;
            mRequestPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    private boolean checkPermissions(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else {
            return context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }

    public boolean canPlay(Context context, SharedPreferences preferences) {
        String key = PREFERENCE_DIRECTORY != null ? PREFERENCE_DIRECTORY : context.getString(R.string.preference_directory);
        String directory = preferences.getString(key, null);
        return directory != null && !directory.isEmpty() && checkPermissions(context);
    }

    private boolean isInStarsAndTime(String directory) {
        if (mIsInStarsAndTime != null) return mIsInStarsAndTime;
        boolean result = false;
        if (directory != null && !directory.isEmpty()) {
            try {
                File indexFile = new File(directory, "index.html");
                if (indexFile.exists()) {
                    String content = new String(Files.readAllBytes(Paths.get(indexFile.getAbsolutePath())));
                    result = content.toLowerCase().contains("in stars and time");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mIsInStarsAndTime = result;
        return result;
    }
}
