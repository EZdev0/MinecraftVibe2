package com.EZdev.mc2;

import android.app.Application;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.PrintWriter;
import java.io.StringWriter;

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

            Intent intent = new Intent(getApplicationContext(), CrashHandlerActivity.class);
            intent.putExtra("error", "An unexpected error occurred. Please restart the app.");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        });

        startWatchdog();
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

                    Intent intent = new Intent(getApplicationContext(), CrashHandlerActivity.class);
                    intent.putExtra("error", "The application has stopped responding.");
                    intent.putExtra("canContinue", true); // Optional: allows continuing if it unblocks
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);

                    // Stop watchdog to avoid multiple triggers
                    break;
                }
            }
        });
        watchdogThread.start();
    }
}
