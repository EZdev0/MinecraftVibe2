#!/bin/bash
cat << 'INNER_EOF' > Minecraft2Vibe/Minecraft2Vibe/app/src/main/AndroidManifest.xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.EZdev.mc2">

    <uses-feature android:glEsVersion="0x00020000" android:required="true" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="McVibe2"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:requestLegacyExternalStorage="true"
        android:theme="@style/Theme.AppCompat.NoActionBar">

        <activity android:name=".MainMenuActivity"
                  android:exported="true"
                  android:screenOrientation="landscape"
                  android:theme="@android:style/Theme.NoTitleBar.Fullscreen">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <activity android:name=".MainActivity"
                  android:exported="false"
                  android:screenOrientation="landscape"
                  android:theme="@android:style/Theme.NoTitleBar.Fullscreen">
        </activity>
    </application>
</manifest>
INNER_EOF
