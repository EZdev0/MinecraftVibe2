import re

with open('./Minecraft2Vibe/Minecraft2Vibe/app/src/main/java/com/EZdev/mc2/UIManager.java', 'r') as f:
    content = f.read()

# Load autoJump from prefs in constructor or setupUI
if "engine.gameplay.autoJump = prefs.getBoolean(\"AUTO_JUMP\", false);" not in content:
    content = content.replace("updateHotbarUI();\n    }", "updateHotbarUI();\n        engine.gameplay.autoJump = prefs.getBoolean(\"AUTO_JUMP\", false);\n    }")

# Add toggle to settings menu
auto_jump_btn = '''
        Button autoJumpBtn = createBtn(engine.gameplay.autoJump ? "AUTO JUMP: AN" : "AUTO JUMP: AUS", "#3498db");
        autoJumpBtn.setOnClickListener(v -> {
            engine.gameplay.autoJump = !engine.gameplay.autoJump;
            prefs.edit().putBoolean("AUTO_JUMP", engine.gameplay.autoJump).apply();
            autoJumpBtn.setText(engine.gameplay.autoJump ? "AUTO JUMP: AN" : "AUTO JUMP: AUS");
        });
        settingsPanel.addView(autoJumpBtn);
'''

if "AUTO JUMP:" not in content:
    content = content.replace("settingsPanel.addView(createHeading(\"--- SYSTEM ---\"));", "settingsPanel.addView(createHeading(\"--- STEUERUNG ---\"));\n" + auto_jump_btn + "\n        settingsPanel.addView(createHeading(\"--- SYSTEM ---\"));")

with open('./Minecraft2Vibe/Minecraft2Vibe/app/src/main/java/com/EZdev/mc2/UIManager.java', 'w') as f:
    f.write(content)

print("UIManager patched for auto jump.")
