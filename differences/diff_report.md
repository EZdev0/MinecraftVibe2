# MinecraftVibe2 Difference Report
This document highlights the missing features and logic between the provided working snapshot and the current repository state, and details how they will be restored.

## 1. Missing Logic Details
Based on the diff between the zip snapshot ("heile Version") and the current repository:
- **Gameplay.java**:
  - `jump()` missing its specific implementation (it only sets `wantsToJump`).
  - `checkCollision()` lacks proper handling to let players walk through FIRE blocks (ID 6).
  - Fire logic from the original version missing (like spreading logic).
  - Proper physics/movement logic when jumping.
  - Sinking through the blocks mapping the exact Y collision floor algorithm.
- **WorldLogic.java**:
  - Explode mechanic is incomplete/missing the `checkIgnition` logic and related checks.
  - Interaction logic for block placement and breaking (TNT and Fire mechanic logic).
- **Chunk.java**:
  - `drawLetterT` had an optimized hardcoded float array manipulation missing in the current version, restoring original mesh generation.

## 2. Plan for Fixing
1. Restore block collision rules (FIRE is non-solid).
2. Restore proper jumping physics inside `Gameplay.java`'s update method.
3. Fix the Y-Axis collision that caused players to sink through solid blocks.
4. Add the `checkIgnition` and proper explosion triggers inside `WorldLogic.java`.
5. Reimplement optimized `drawLetterT` and quad adding in `Chunk.java`.
6. Keep the multiplayer components untouched while carefully bringing back singleplayer logic from the original source.
7. Note: The particle system (`ActiveFireParticle`, `blockParticles`, etc) was discovered to be part of the current repo's feature set and intentionally kept intact while fixing the underlying interaction mechanics.
## 3. Particle System Fix
1. Although the particle system class references existed in `Gameplay.java`, the render loop in `WorldLogic.java` was missing the routines to actually iterate over `blockParticles` and `fireParticles` and draw them using OpenGL.
2. The particle drawing code using `Booster.tntVertexBuffer` and matrix scaling (relative to the `life` attribute) has been fully restored and integrated back into the `WorldLogic.render` sequence.
