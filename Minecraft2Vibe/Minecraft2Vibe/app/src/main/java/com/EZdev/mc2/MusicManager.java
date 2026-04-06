package com.EZdev.mc2;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

public class MusicManager {
    private static final String TAG = "MusicManager";
    private MediaPlayer mediaPlayer;
    private Context context;
    private boolean isPlaying = false;
    private boolean isEnabled = true;

    // A free public domain background track as fallback, normally you'd use raw resources
    private String[] trackUrls = {
        "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
        "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3"
    };

    private int currentTrack = 0;

    public MusicManager(Context context) {
        this.context = context;
        this.isEnabled = context.getSharedPreferences("McPrefs", Context.MODE_PRIVATE).getBoolean("MUSIC_ENABLED", true);
    }

    public void startMusic() {
        if (!isEnabled) return;
        try {
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(context, Uri.parse(trackUrls[currentTrack]));
                mediaPlayer.setOnCompletionListener(mp -> playNextTrack());
                mediaPlayer.prepareAsync();
                mediaPlayer.setOnPreparedListener(mp -> {
                    mp.start();
                    isPlaying = true;
                });
            } else if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
                isPlaying = true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error starting music", e);
        }
    }

    private void playNextTrack() {
        if (!isEnabled) return;
        currentTrack = (currentTrack + 1) % trackUrls.length;
        stopMusic();
        startMusic();
    }

    public void stopMusic() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
            isPlaying = false;
        }
    }

    public void toggleMusic() {
        isEnabled = !isEnabled;
        context.getSharedPreferences("McPrefs", Context.MODE_PRIVATE).edit().putBoolean("MUSIC_ENABLED", isEnabled).apply();
        if (isEnabled) startMusic();
        else stopMusic();
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}
