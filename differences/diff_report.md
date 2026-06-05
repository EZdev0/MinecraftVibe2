# MinecraftVibe2 Difference Report
This document highlights the missing features and logic between the provided working snapshot and the current repository state, and details how they will be restored.

## 1. Missing Logic Details
Based on the diff between the zip snapshot ("heile Version") and the current repository:
- **Gameplay.java**:
  - `jump()` missing its specific implementation (it only sets `wantsToJump`).
  - `checkCollision()` lacks proper handling to let players walk through FIRE blocks (ID 6).
  - Fire logic from the original version missing (like spreading logic).
  - Proper physics/movement logic when jumping.
- **WorldLogic.java**:
  - Explode mechanic is incomplete/missing the `checkIgnition` logic and related checks.
  - Interaction logic for block placement and breaking (TNT and Fire mechanic logic).
- **Chunk.java**:
  - `drawLetterT` had an optimized hardcoded float array manipulation missing in the current version, restoring original mesh generation.
- **MyGdxGame.java** & **MainActivity.java**:
  - Main app loop initialization looks modified, but the critical logic missing is inside Gameplay and WorldLogic.

## 2. Plan for Fixing
1. Restore block collision rules (FIRE is non-solid).
2. Restore proper jumping physics inside `Gameplay.java`'s update method.
3. Add the `checkIgnition` and proper explosion triggers inside `WorldLogic.java`.
4. Keep the multiplayer components untouched while carefully bringing back singleplayer logic from the original source.
5. Reimplement optimized `drawLetterT` and quad adding in `Chunk.java`.
