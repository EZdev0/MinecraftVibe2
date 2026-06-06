import re

with open('./Minecraft2Vibe/Minecraft2Vibe/app/src/main/java/com/EZdev/mc2/Gameplay.java', 'r') as f:
    content = f.read()

# Add autoJump
if "public boolean autoJump = false;" not in content:
    content = content.replace("public boolean wantsToJump = false;", "public boolean wantsToJump = false;\n    public boolean autoJump = false;")

# Add autoJump logic
autojump_logic = '''
        if (autoJump && onGround && (joyMoveX != 0 || joyMoveY != 0)) {
            float testMoveX = (sinY * -joyMoveY + cosY * joyMoveX) * speed;
            float testMoveZ = (-cosY * -joyMoveY + sinY * joyMoveX) * speed;
            if (checkCollision(world, camX + testMoveX, camY, camZ) || checkCollision(world, camX, camY, camZ + testMoveZ)) {
                if (!checkCollision(world, camX + testMoveX, camY + 1.1f, camZ) && !checkCollision(world, camX, camY + 1.1f, camZ + testMoveZ)) {
                    wantsToJump = true;
                }
            }
        }
'''

if "float moveX =" in content and "autoJump && onGround" not in content:
    idx = content.find("        float moveX = (sinY * -joyMoveY + cosY * joyMoveX) * speed;")
    content = content[:idx] + autojump_logic + content[idx:]

with open('./Minecraft2Vibe/Minecraft2Vibe/app/src/main/java/com/EZdev/mc2/Gameplay.java', 'w') as f:
    f.write(content)

print("Gameplay patched for auto jump.")
