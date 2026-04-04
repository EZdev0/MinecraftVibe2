package com.EZdev.mc2;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

public class SoundManager {
    private SoundPool soundPool;
    private Context context;
    private boolean isEnabled;

    // We don't have real OGG files, so this is a structural stub for when resources exist
    private int grassSoundId = -1;
    private int stoneSoundId = -1;
    private int glassSoundId = -1;

    public SoundManager(Context context) {
        this.context = context;
        this.isEnabled = context.getSharedPreferences("McPrefs", Context.MODE_PRIVATE).getBoolean("MUSIC_ENABLED", true);

        AudioAttributes attributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();

        soundPool = new SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(attributes)
            .build();

        // Example: grassSoundId = soundPool.load(context, R.raw.grass_step, 1);
    }

    public void playSoundForBlock(byte blockId) {
        if (!isEnabled) return;
        int soundId = -1;
        // 1=Dirt/Grass, 2=Stone, 3=Wood, 4=Leaves, 5=TNT, 6=Fire, 7=Water
        if (blockId == 1 || blockId == 4) soundId = grassSoundId;
        else if (blockId == 2) soundId = stoneSoundId;
        else soundId = glassSoundId; // default

        if (soundId != -1) {
            soundPool.play(soundId, 1.0f, 1.0f, 0, 0, 1.0f);
        }
    }

    public void release() {
        if(soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }
}
