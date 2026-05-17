package com.EZdev.mc2;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public class McApp extends Application {

    private static final int ANR_TIMEOUT = 5000;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private volatile long lastTick = 0;
    private Thread watchdogThread;

    @Override
    public void onCreate() {
        super.onCreate();

        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            Log.e("McApp", "Crash detected: " + sw.toString());

            triggerCrashHandler(getApplicationContext(), "An unexpected error occurred. Please restart the app.", false);

            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        });

        startWatchdog();
    }

    public static void triggerCrashHandler(Context context, String errorMsg, boolean canContinue) {
        try (FileOutputStream fos = context.openFileOutput("crash_log.txt", Context.MODE_PRIVATE)) {
            fos.write(errorMsg.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.e("McApp", "Failed to save crash log", e);
        }

        Intent intent = new Intent(context, CrashHandlerActivity.class);
        intent.putExtra("canContinue", canContinue);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(intent);
    }

    private void startWatchdog() {
        watchdogThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                final long currentTick = lastTick;
                uiHandler.post(() -> lastTick = System.currentTimeMillis());

                try {
                    Thread.sleep(ANR_TIMEOUT);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                if (lastTick == currentTick && lastTick != 0) {
                    // Main thread is blocked
                    StackTraceElement[] stackTrace = Looper.getMainLooper().getThread().getStackTrace();
                    StringBuilder sb = new StringBuilder("ANR detected\n");
                    for (StackTraceElement el : stackTrace) {
                        sb.append(el.toString()).append("\n");
                    }
                    Log.e("McApp", sb.toString());

                    triggerCrashHandler(getApplicationContext(), "The application has stopped responding.", true);

                    // Stop watchdog to avoid multiple triggers
                    break;
                }
            }
        });
        watchdogThread.start();
    }
}
