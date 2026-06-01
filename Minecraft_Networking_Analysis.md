# Minecraft Networking Architecture Analysis (Update 2026)

## 1. Minecraft Java Edition
- **Protocol:** Uses **TCP**.
- **Communication:** Reliable, ordered, but susceptible to "Head-of-Line Blocking" (one lost packet blocks all subsequent ones).
- **Voxel Handling:** Chunk updates are often transmitted with compression (Zlib).
- **Client/Server Logic:** The client calculates movements, while the server validates them (Anti-Cheat).

## 2. Minecraft Bedrock Edition
- **Protocol:** Uses **RakNet** (Custom Layer on **UDP**).
- **Why UDP?** Lower latency, critical for mobile networks. RakNet adds reliability only for essential packets.
- **NAT Traversal:** Uses STUN/TURN via Xbox Live Services.

## 3. Implementation in McVibe2 (Our Approach)
- **Hybrid System:** We combine the best of both worlds:
  - **TCP (Port 9999):** For critical actions like block placement, chat, and item events. Data loss is not acceptable here.
  - **UDP (Port 9998):** For position updates. Since these are sent ~20 times per second, it doesn't matter if a packet is lost – the next one will overwrite it anyway.
- **Smooth Movements:** We use **Linear Interpolation (LERP)**. Instead of players "jittering," they glide smoothly from the old to the new position (Target-Vector-Sync).
- **Rendering:** Name tags are rendered as **Billboards** (they always rotate to face the observer's camera).

## 4. Bandwidth Optimization
- **Delta-Sync:** Full chunks are never sent (except during the initial join). Afterward, only differences (Block X,Y,Z became ID) are transmitted.
- **Thread Safety:** All network operations run asynchronously to the render thread to prevent ANR (App Not Responding) errors.
