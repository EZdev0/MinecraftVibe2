# Minecraft Networking Architecture Analysis

## 1. Minecraft Java Edition
- **Protocol**: Uses **TCP** (Transmission Control Protocol).
- **Communication**: Reliable but slightly slower due to TCP overhead.
- **Server**: Uses a single-threaded main loop (usually) for game logic, with networking handled in Netty threads.
- **Voxel Handling**: Uses "Delta" updates for chunks. Only changes are sent after the initial chunk load.
- **Compression**: Uses Zlib compression for large packets (like chunk data).

## 2. Minecraft Bedrock Edition
- **Protocol**: Uses **RakNet** (built on top of **UDP**).
- **Why UDP?**: Lower latency. RakNet adds a reliability layer on top of UDP only for packets that need it.
- **NAT Traversal**: Bedrock uses various techniques (including STUN/TURN via Xbox Live) to allow players to join each other without port forwarding.
- **Mobile Focus**: Highly optimized for low bandwidth and unstable connections.

## 3. Implementation in McVibe2
- **Hybrid Approach**: Our implementation uses **TCP** for reliable events (Blocks) and **UDP** for high-frequency data (Positions). This is a "Best of both worlds" approach similar to how many modern engines (like Source or Unreal) work.
- **Lobby**: Real Minecraft uses centralized services (Xbox Live / Mojang Accounts). For McVibe2, we use LAN Broadcast and (planned) Global REST-based discovery.

## 4. Chunk Loading Online
- **Minecraft**: When a player moves, the server sends only the chunks within the player's "view distance".
- **McVibe2**: Currently, each client generates their own world based on the same seed. Only block changes (player actions) are synced. This saves massive amounts of bandwidth but requires the seed to be identical.
