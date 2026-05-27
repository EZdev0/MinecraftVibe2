# Minecraft Networking Architecture Analysis (Update 2026)

## 1. Minecraft Java Edition
- **Protocol**: Uses **TCP**.
- **Communication**: Reliable, ordered, but susceptible to "Head-of-Line Blocking" (ein verlorenes Paket blockiert alle folgenden).
- **Voxel Handling**: Chunk-Updates werden oft komprimiert (Zlib) übertragen.
- **Steve Logic**: Client berechnet Bewegungen, Server validiert sie (Anti-Cheat).

## 2. Minecraft Bedrock Edition
- **Protocol**: Uses **RakNet** (Custom Layer on **UDP**).
- **Why UDP?**: Niedrigere Latenz, wichtig für Mobile-Netzwerke. RakNet fügt Zuverlässigkeit (Reliability) nur für wichtige Pakete hinzu.
- **NAT Traversal**: Nutzt STUN/TURN über Xbox Live Services.

## 3. Implementation in McVibe2 (Our Approach)
- **Hybrides System**: Wir kombinieren die Vorteile beider Welten:
  - **TCP (Port 9999)**: Für kritische Aktionen wie Block-Platzierung, Chat und Item-Events. Hier darf kein Datenverlust auftreten.
  - **UDP (Port 9998)**: Für Positions-Updates. Da diese ca. 20x pro Sekunde gesendet werden, ist es egal, wenn mal ein Paket verloren geht – das nächste überschreibt es ohnehin.
- **Glatte Bewegungen**: Wir nutzen **Lineare Interpolation (LERP)**. Statt dass Spieler "hüpfen", gleiten sie sanft von der alten zur neuen Position (Target-Vector-Sync).
- **Rendering**: Name-Tags werden als **Billboards** gerendert (sie drehen sich immer zur Kamera des Betrachters).

## 4. Bandbreiten-Optimierung
- **Delta-Sync**: Es wird niemals ein ganzer Chunk gesendet (außer beim ersten Join). Danach werden nur noch Differenzen (Block X,Y,Z wurde ID) übertragen.
- **Thread-Sicherheit**: Alle Netzwerk-Operationen laufen asynchron zum Render-Thread, um ANR-Fehler (App Not Responding) zu vermeiden.
