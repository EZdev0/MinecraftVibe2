import re

with open('./Minecraft2Vibe/Minecraft2Vibe/app/src/main/java/com/EZdev/mc2/MainMenuActivity.java', 'r') as f:
    content = f.read()

# Check if android.os.Build is imported. If not, add it.
if "import android.os.Build;" not in content:
    content = re.sub(r'(import android\.os\.Handler;)', r'\1\nimport android.os.Build;', content)

with open('./Minecraft2Vibe/Minecraft2Vibe/app/src/main/java/com/EZdev/mc2/MainMenuActivity.java', 'w') as f:
    f.write(content)
