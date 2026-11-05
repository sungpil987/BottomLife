package com.bottomlife.rebuild;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.bottomlife.core.BottomLifeGame;
import java.io.PrintWriter;
import java.io.StringWriter;

public class AndroidLauncher extends AndroidApplication {
    private static final String TAG = "BottomLifeCrash";
    private Thread.UncaughtExceptionHandler previousHandler;
    private volatile boolean showingCrash;

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        previousHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            Log.e(TAG, "Uncaught exception on " + thread.getName(), error);
            if (showingCrash) {
                if (previousHandler != null) previousHandler.uncaughtException(thread, error);
                return;
            }
            showingCrash = true;
            StringWriter trace = new StringWriter();
            error.printStackTrace(new PrintWriter(trace));
            String details = trace.toString();
            try {
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        TextView view = new TextView(this);
                        view.setText(details);
                        view.setTextIsSelectable(true);
                        view.setPadding(24, 24, 24, 24);
                        ScrollView scroll = new ScrollView(this);
                        scroll.addView(view);
                        new AlertDialog.Builder(this)
                            .setTitle("BottomLife startup error")
                            .setMessage("Please screenshot the error details below.")
                            .setView(scroll)
                            .setPositiveButton("Close", (dialog, which) -> finish())
                            .setCancelable(false)
                            .show();
                    } catch (Throwable dialogError) {
                        Log.e(TAG, "Could not display crash dialog", dialogError);
                        finish();
                    }
                });
            } catch (Throwable handlerError) {
                Log.e(TAG, "Could not schedule crash dialog", handlerError);
                if (previousHandler != null) previousHandler.uncaughtException(thread, error);
            }
        });
        try {
            AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
            config.useImmersiveMode = true;
            initialize(new BottomLifeGame(), config);
        } catch (Throwable startupError) {
            Log.e(TAG, "Startup initialization failed", startupError);
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), startupError);
        }
    }
}
