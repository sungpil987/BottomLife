package com.bottomlife.rebuild;

import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Typeface;
import java.io.FileOutputStream;
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

    private void buildKoreanAtlas() throws Exception {
        String chars = " !+/:.0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
            + "밑바닥인생돈초당보석광물영혼터치획득거리노동자고용강화비용광산채굴깊이지하층확장환생필요부족성공자동수익보상상자발견희귀대박행운레벨무료판매코"
            + "지속시간최대복귀오프라인수입업그레이드구매완료한글게임시작보유진행도목표달성추가";
        StringBuilder unique = new StringBuilder();
        for(int i=0;i<chars.length();i++) if(unique.indexOf(String.valueOf(chars.charAt(i)))<0) unique.append(chars.charAt(i));
        int cols=16, cell=40, rows=(unique.length()+cols-1)/cols;
        Bitmap bitmap=Bitmap.createBitmap(cols*cell,rows*cell,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(bitmap);
        canvas.drawColor(Color.TRANSPARENT);
        Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));
        paint.setTextSize(27);
        for(int i=0;i<unique.length();i++){
            int x=(i%cols)*cell+2, y=(i/cols)*cell+30;
            canvas.drawText(String.valueOf(unique.charAt(i)),x,y,paint);
        }
        try(FileOutputStream out=openFileOutput("korean_atlas.png",MODE_PRIVATE)){
            bitmap.compress(Bitmap.CompressFormat.PNG,100,out);
        }
        bitmap.recycle();
        try(java.io.OutputStreamWriter writer=new java.io.OutputStreamWriter(openFileOutput("korean_chars.txt",MODE_PRIVATE),"UTF-8")){
            writer.write(unique.toString());
        }
    }
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
            buildKoreanAtlas();
            AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
            config.useImmersiveMode = true;
            initialize(new BottomLifeGame(), config);
        } catch (Throwable startupError) {
            Log.e(TAG, "Startup initialization failed", startupError);
            Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), startupError);
        }
    }
}
