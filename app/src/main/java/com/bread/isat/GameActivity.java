package com.bread.isat;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.ViewGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class GameActivity extends Activity implements AudioManager.OnAudioFocusChangeListener {
    private OmoWebView mWebView;
    private Dialog mMenuDialog;
    private Dialog mQuitDialog;
    private AudioManager mAudioManager;
    private AudioFocusRequest mFocusRequest;

    private void hideSystemUI() {
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(),
                getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        mWebView = findViewById(R.id.webView);
        mWebView.setOnCloseWindowListener(this::finishAndRemoveTask);

        ViewCompat.setOnApplyWindowInsetsListener(mWebView, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) v.getLayoutParams();

            // i fucking hate android
            mlp.leftMargin = insets.left;
            mlp.rightMargin = insets.right;

            v.setLayoutParams(mlp);

            return WindowInsetsCompat.CONSUMED;
        });

        mWebView.start();

        final CharSequence[] menuItems = new CharSequence[] { "Toggle FPS counter", "Toggle touch input",
                "Edit controls", "Toggle widescreen border", "Quit game" };

        mMenuDialog = new AlertDialog.Builder(this)
                .setTitle("Menu")
                .setItems(menuItems, (d, w) -> {
                    switch (w) {
                        case 0: // Toggle FPS counter
                            mWebView.eval("Graphics._toggleFPSCounter();");
                            break;
                        case 1: // Toggle touch input
                            mWebView.eval("TouchInput._toggleTouchInput();");
                            break;
                        case 2: // Edit controls
                            mWebView.eval("Input._editControls();");
                            break;
                        case 3: // Toggle widescreen border
                            mWebView.eval("nwcompat._toggleBorders();");
                            break;
                        case 4: // Quit game
                            mQuitDialog.show();
                            break;
                    }
                    d.dismiss();
                })
                .create();

        mQuitDialog = new AlertDialog.Builder(this)
                .setTitle("Quit game")
                .setMessage("Are you sure you want to quit?")
                .setPositiveButton("Yes", (d, w) -> finishAndRemoveTask())
                .setNegativeButton("No", (d, w) -> d.cancel())
                .create();
    }

    @Override
    protected void onDestroy() {
        if (mWebView != null) {
            try {
                ViewGroup contentView = (ViewGroup) mWebView.getParent();
                contentView.removeView(mWebView);

                // causes "Renderer process crash detected (code -1)"
                // chatgpt says it is normal when we're calling destroy
                // i didn't found any proof of it in documentation or elsewhere
                mWebView.destroy();
                mWebView = null;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();

        requestAudioFocus();

        if (mWebView != null) {
            mWebView.resumeTimers();
            mWebView.onResume();
            mWebView.eval("if (window.WebAudio && WebAudio._context) WebAudio._context.resume();");
            mWebView.eval("window.dispatchEvent(new Event('focus'));");
            mWebView.eval("window.nw.Window.get().dispatchEvent(new Event('restore'));");
        }
    }

    @Override
    protected void onPause() {
        abandonAudioFocus();

        if (mWebView != null) {
            mWebView.eval("window.nw.Window.get().dispatchEvent(new Event('minimize'));");
            mWebView.eval("window.dispatchEvent(new Event('blur'));");
            mWebView.eval("if (window.WebAudio && WebAudio._context) WebAudio._context.suspend();");
            mWebView.onPause();
            mWebView.pauseTimers();
        }

        super.onPause();
    }

    @Override
    public void onBackPressed() {
        mMenuDialog.show();
    }

    private void requestAudioFocus() {
        AudioAttributes playbackAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        mFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(this)
                .build();
        mAudioManager.requestAudioFocus(mFocusRequest);
    }

    private void abandonAudioFocus() {
        if (mFocusRequest != null) {
            mAudioManager.abandonAudioFocusRequest(mFocusRequest);
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (mWebView == null)
            return;

        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                mWebView.eval("if (window.WebAudio && WebAudio._context) WebAudio._context.resume();");
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                mWebView.eval("if (window.WebAudio && WebAudio._context) WebAudio._context.suspend();");
                break;
        }
    }
}
