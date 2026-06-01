sed -i '/public void update(float dt, WorldLogic world) {/a \
\ \ \ \ \ \ \ \ \/\/ Update Reverb in Caves\n\ \ \ \ \ \ \ \ if (activity != null && activity.soundManager != null) {\n\ \ \ \ \ \ \ \ \ \ \ \ boolean inCave = camY < 40;\n\ \ \ \ \ \ \ \ \ \ \ \ activity.soundManager.updateReverb(inCave);\n\ \ \ \ \ \ \ \ }' ./Minecraft2Vibe/Minecraft2Vibe/app/src/main/java/com/EZdev/mc2/Gameplay.java
