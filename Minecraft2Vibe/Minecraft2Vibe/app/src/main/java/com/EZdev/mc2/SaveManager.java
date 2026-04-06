package com.EZdev.mc2;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class SaveManager {

    private static final String TAG = "SaveManager";
    private Context context;
    private String worldName = "world1";

    public SaveManager(Context context) {
        this.context = context;
    }

    public void saveChunk(Chunk chunk) {
        try {
            File dir = new File(context.getFilesDir(), worldName);
            if (!dir.exists()) dir.mkdirs();

            File file = new File(dir, "chunk_" + chunk.chunkX + "_" + chunk.chunkZ + ".dat");
            FileOutputStream fos = new FileOutputStream(file);

            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 128; y++) {
                    fos.write(chunk.blocks[x][y]); // Writes the 16-length byte array
                }
            }
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "Error saving chunk", e);
        }
    }

    public boolean loadChunk(Chunk chunk) {
        try {
            File dir = new File(context.getFilesDir(), worldName);
            File file = new File(dir, "chunk_" + chunk.chunkX + "_" + chunk.chunkZ + ".dat");

            if (!file.exists()) return false;

            FileInputStream fis = new FileInputStream(file);
            for (int x = 0; x < 16; x++) {
                for (int y = 0; y < 128; y++) {
                    fis.read(chunk.blocks[x][y]);
                }
            }
            fis.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error loading chunk", e);
            return false;
        }
    }
}
