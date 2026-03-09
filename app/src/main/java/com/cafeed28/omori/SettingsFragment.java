package com.cafeed28.omori;

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

import java.nio.file.Files;
import java.nio.file.Paths;

public class SettingsFragment extends PreferenceFragmentCompat {
    public interface OnPreferencesUpdateListener {
        void onPreferencesUpdate(SharedPreferences preferences);
    }

    private OnPreferencesUpdateListener mListener;

    private final SharedPreferences.OnSharedPreferenceChangeListener prefListener = (preferences, key) -> {
        updatePreferences(preferences);
    };

    public static String PREFERENCE_DIRECTORY;
    public static String PREFERENCE_LOGS;
    public static String PREFERENCE_LOGS_CLEAR;

    private SharedPreferences mPreferences;
    private ActivityResultLauncher<Uri> mOpenDocumentTree;
    private ActivityResultLauncher<String> mRequestPermission;

    private Activity mActivity;

    public void setOnPreferencesUpdateListener(OnPreferencesUpdateListener listener) {
        mListener = listener;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mActivity = getActivity();

        PREFERENCE_DIRECTORY = getString(R.string.preference_directory);
        PREFERENCE_LOGS = getString(R.string.preference_logs);
        PREFERENCE_LOGS_CLEAR = getString(R.string.preference_logs_clear);

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
            }
            Toast.makeText(mActivity, "Restart app now", Toast.LENGTH_LONG).show();
            mActivity.finishAndRemoveTask();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        updatePreferences(mPreferences);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        Preference directoryPreference = findPreference(PREFERENCE_DIRECTORY);
        Preference logsPreference = findPreference(PREFERENCE_LOGS);
        Preference logsClearPreference = findPreference(PREFERENCE_LOGS_CLEAR);
        
        if (directoryPreference != null) {
            directoryPreference.setOnPreferenceClickListener(preference -> {
                if (checkPermissions(mActivity)) mOpenDocumentTree.launch(null);
                else requestPermissions();
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
                Toast.makeText(mActivity, "Restart app now", Toast.LENGTH_LONG).show();
                mActivity.finishAndRemoveTask();
                return true;
            });
        }

        updatePreferences(mPreferences);
    }

    private void updatePreferences(SharedPreferences preferences) {
        if (mListener != null) mListener.onPreferencesUpdate(preferences);

        Preference directoryPreference = findPreference(PREFERENCE_DIRECTORY);
        Preference logsPreference = findPreference(PREFERENCE_LOGS);
        Preference logsClearPreference = findPreference(PREFERENCE_LOGS_CLEAR);
        
        if (directoryPreference != null) {
            directoryPreference.setSummary(String.format("Current: %s", preferences.getString(PREFERENCE_DIRECTORY, "not set")));
        }

        boolean filesPermission = checkPermissions(mActivity);
        if (logsPreference != null) {
            if (!logsPreference.isEnabled() && filesPermission) {
                Toast.makeText(mActivity, "Restart app now", Toast.LENGTH_LONG).show();
                mActivity.finishAndRemoveTask();
            }
            logsPreference.setEnabled(filesPermission);
        }
        
        if (logsClearPreference != null) {
            logsClearPreference.setEnabled(filesPermission);
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Toast.makeText(mActivity, "Allow all files access", Toast.LENGTH_LONG).show();
            startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + BuildConfig.APPLICATION_ID)));
        } else {
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
        String directory = preferences.getString(SettingsFragment.PREFERENCE_DIRECTORY, null);
        return directory != null && !directory.isEmpty() && checkPermissions(context);
    }
}
