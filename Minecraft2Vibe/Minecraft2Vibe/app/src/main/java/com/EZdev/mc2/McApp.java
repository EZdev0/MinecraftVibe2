package com.EZdev.mc2;

import android.app.Application;
import android.content.Intent;
import android.util.Log;
import java.io.PrintWriter;
import java.io.StringWriter;

public class McApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            Log.e("McApp", "CRASH", e);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String stackTrace = sw.toString();

            Intent intent = new Intent(getApplicationContext(), CrashHandlerActivity.class);
            intent.putExtra("error", stackTrace);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        });
    }
}
