package com.bread.isat;

import android.content.res.AssetManager;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceResponse;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class NwCompatPathHandler {
    private final String TAG = this.getClass().getSimpleName();

    private final AssetManager mAssets;
    private final String mDirectory;

    public NwCompatPathHandler(AssetManager assets, String directory) {
        mAssets = assets;
        mDirectory = directory;
    }

    public static String getMimeType(@NonNull String path) {
        int lastIndexOf = path.lastIndexOf(".");
        String extension = "";
        if (lastIndexOf != -1) {
            extension = path.substring(lastIndexOf + 1).toLowerCase();
        }

        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
    }

    @Nullable
    private InputStream handleGame(String path) {
        try {
            return Files.newInputStream(Paths.get(mDirectory, path));
        } catch (IOException e) {
            if (!(e instanceof NoSuchFileException)) {
                Debug.i().log(Log.ERROR, e.toString());
            }
        }
        return null;
    }

    @Nullable
    private InputStream handleAsset(String path) {
        InputStream is = null;

        if (BuildConfig.DEBUG) {
            try {
                is = Files.newInputStream(Paths.get(mDirectory, "assets", path));
            } catch (IOException e) {
                if (!(e instanceof NoSuchFileException)) {
                    Debug.i().log(Log.ERROR, e.toString());
                }
            }
        }

        if (is == null) {
            try {
                is = mAssets.open(path);
            } catch (IOException e) {
                if (!(e instanceof FileNotFoundException)) {
                    Debug.i().log(Log.ERROR, e.toString());
                }
            }
        }

        return is;
    }

    public WebResourceResponse handle(String path) {
        InputStream is = handleAsset(path);
        if (is == null) is = handleGame(path);
        if (is == null) {
            Debug.i().log(Log.INFO, "%s: file not found: '%s' ('%s')", TAG, path, mDirectory);
            return null;
        }

        Map<String, String> headers = new HashMap<>(2);
        headers.put("Pragma", "no-cache");
        headers.put("Cache-Control", "no-cache");

        WebResourceResponse response = new WebResourceResponse(getMimeType(path), null, is);
        response.setResponseHeaders(headers);
        return response;
    }
}
