import re

with open('./Minecraft2Vibe/Minecraft2Vibe/app/src/main/java/com/EZdev/mc2/MainMenuActivity.java', 'r') as f:
    content = f.read()

# Replace Build.VERSION with android.os.Build.VERSION explicitly everywhere
content = content.replace('Build.VERSION.SDK_INT', 'android.os.Build.VERSION.SDK_INT')
content = content.replace('Build.VERSION_CODES.M', 'android.os.Build.VERSION_CODES.M')

with open('./Minecraft2Vibe/Minecraft2Vibe/app/src/main/java/com/EZdev/mc2/MainMenuActivity.java', 'w') as f:
    f.write(content)
