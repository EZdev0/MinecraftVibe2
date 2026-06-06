# MinecraftVibe2
Mc vibecoding 2 Gemini No AIDE - The ultimate voxel experience for Android.

## 🚀 New Features: Multiplayer Mode (2026 Edition)
We have implemented a complete multiplayer system based on modern P2P technologies:
- **Global & LAN Lobby:** Find players worldwide via our master server or in your local Wi-Fi.
- **Hybrid Network Protocol:**
  - **TCP:** For reliable data like block updates, items, and chat.
  - **UDP:** For ultra-fast position synchronization without jitter.
- **3D Player Models:** See your friends as animated characters in the world.
- **Name Tags:** Dynamic 3D billboards show player names above their heads.
- **Interpolation:** Smooth movements through intelligent LERP (Linear Interpolation) logic.
- **NAT Traversal:** Support for UPnP and STUN to make hosting on a smartphone easier.

## ⚙️ Settings & Optimization
Customize your experience in the settings menu:
- **Render Distance (Chunks):** Adjust how many chunks are visible around the player. Lower values improve performance on older devices.
- **Fog Enabled:** Toggles the distance-based fog effect. Fog helps hide chunk loading and improves atmosphere.
- **Fast Render (Vulkan/VSync):** Experimental mode that disables VSync to unlock maximum frame rates. Recommended for high-refresh-rate displays.
- **Music & SFX:** Independent toggles for atmospheric background music and block interaction sounds.
- **Debug Info:** Displays real-time coordinates and FPS.
- **GL Warnings:** Enables detailed reporting of OpenGL errors via the internal crash handler.
- **Rendering Optimization**: Implemented high performance memory-friendly draw optimizations (e.g. `drawLetterT` vertices optimizations).

## 🎨 UI Editor
The UI Editor allows you to freely move all controls (Buttons, Joysticks, Hotbar).
- **Usage:** Toggle UI Editor in settings, drag elements to your desired position, and press "CLOSE" to save permanently to `SharedPreferences`.
- **Adaptive Layout:** Uses optimized gravity logic to ensure positions remain consistent across different screen resolutions.

## 🛠 Technical Details
- **Architecture:** Host-Client (P2P). The host acts as the server.
- **Ports:** 9999 (TCP), 9998 (UDP), 8888 (UDP-Disc).
- **Infrastructure:** No expensive APIs or keys needed – uses free master server concepts.
- **Fire & TNT Logic:** Realistic fire spreading to WOOD and LEAVES, and chain-reaction TNT ignition. Correct Y-axis bounding boxes implemented for solid structures.

## 🔧 Crash Management
Integrated network diagnostic handler in the `CrashHandlerActivity`. If a connection error occurs, detailed logs including permission status are generated and can be copied to the clipboard with a single tap.
