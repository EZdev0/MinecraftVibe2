package com.EZdev.mc2;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.media.audiofx.EnvironmentalReverb;
import java.util.Random;

public class SoundManager {
    private SoundPool soundPool;
    private Context context;
    private boolean isEnabled;
    private Random random;

    private int grassSoundId = -1;
    private int stoneSoundId = -1;
    private int woodSoundId = -1;
    private int tntSoundId = -1;
    private int waterSoundId = -1;

    private EnvironmentalReverb reverb;

    private long lastTntTime = 0;

    public SoundManager(Context context) {
        this.context = context;
        this.isEnabled = context.getSharedPreferences("McPrefs", Context.MODE_PRIVATE).getBoolean("SFX_ENABLED", true);
        this.random = new Random();

        AudioAttributes attributes = new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build();

        soundPool = new SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(attributes)
            .build();

        // Load sounds from raw resources
        // grassSoundId = soundPool.load(context, R.raw.grass, 1);
        // stoneSoundId = soundPool.load(context, R.raw.stone, 1);
        // woodSoundId = soundPool.load(context, R.raw.wood, 1);
        // tntSoundId = soundPool.load(context, R.raw.tnt, 1);
        // waterSoundId = soundPool.load(context, R.raw.water, 1);

        // Setup Reverb (Schallsystem für Höhlen)
        try {
            reverb = new EnvironmentalReverb(0, 0);
            reverb.setDecayTime(1500);
            reverb.setRoomLevel((short) -1000);
            reverb.setEnabled(true);
        } catch (Exception e) {
            android.util.Log.e("SoundManager", "Reverb not supported", e);
        }
    }

    public void updateReverb(boolean inCave) {
        if (reverb != null) {
            reverb.setEnabled(inCave);
        }
    }

    public void playSoundForBlock(byte blockId) {
        if (!isEnabled) return;

        int soundId = -1;
        float volume = 1.0f;

        // 1=Dirt/Grass, 2=Stone, 3=Wood, 4=Leaves, 5=TNT, 6=Fire, 7=Water, 9=Bedrock
        if (blockId == 1 || blockId == 4) soundId = grassSoundId;
        else if (blockId == 2 || blockId == 9) soundId = stoneSoundId;
        else if (blockId == 3) soundId = woodSoundId;
        else if (blockId == 5) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTntTime > 150) { // Limit TNT sounds (intelligent handling)
                soundId = tntSoundId;
                lastTntTime = currentTime;
                volume = 0.8f;
            } else {
                return; // Skip if too many TNTs
            }
        }
        else if (blockId == 7) soundId = waterSoundId;
        else soundId = stoneSoundId; // default

        if (soundId != -1) {
            // Random pitch/rate to avoid sounding "stiff" (0.8f to 1.2f)
            float rate = 0.8f + random.nextFloat() * 0.4f;
            soundPool.play(soundId, volume, volume, 0, 0, rate);
        }
    }

    public void toggleSFX() {
        isEnabled = !isEnabled;
        context.getSharedPreferences("McPrefs", Context.MODE_PRIVATE).edit().putBoolean("SFX_ENABLED", isEnabled).apply();
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public void release() {
        if(soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
        if(reverb != null) {
            reverb.release();
            reverb = null;
        }
    }
}
