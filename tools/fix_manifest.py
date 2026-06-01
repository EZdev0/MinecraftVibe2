import re

with open('./Minecraft2Vibe/Minecraft2Vibe/app/src/main/AndroidManifest.xml', 'r') as f:
    content = f.read()

content = content.replace('    package="com.EZdev.mc2"', '')

with open('./Minecraft2Vibe/Minecraft2Vibe/app/src/main/AndroidManifest.xml', 'w') as f:
    f.write(content)
