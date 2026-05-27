# MinecraftVibe2
Mc vibecoding 2 Gemini No AIDE - Die ultimative Voxel-Erfahrung für Android.

## 🚀 Neue Features: Mehrspieler-Modus (2026 Edition)
Wir haben ein komplettes Multiplayer-System implementiert, das auf modernsten P2P-Technologien basiert:
- **Global & LAN Lobby**: Finde Mitspieler weltweit oder im lokalen WLAN.
- **Hybrides Netzwerk-Protokoll**:
  - **TCP**: Für zuverlässige Daten wie Block-Updates, Items und Chat.
  - **UDP**: Für ultra-schnelle Positions-Synchronisation ohne Jitter.
- **3D-Spielermodelle**: Sieh deine Freunde als animierte Charaktere in der Welt.
- **Namensschilder**: Dynamische 3D-Billboards zeigen die Namen der Spieler über ihren Köpfen an.
- **Interpolation**: Glatte Bewegungen durch intelligente LERP-Logik (Linear Interpolation).
- **NAT-Traversal**: Unterstützung für UPnP und STUN, um das Hosten auf dem Smartphone zu vereinfachen.

## 🛠 Technische Details
- **Architektur**: Host-Client (P2P). Der Host fungiert als Server.
- **Ports**: 9999 (TCP), 9998 (UDP), 8888 (UDP-Disc).
- **Infrastruktur**: Keine teuren APIs oder Keys nötig – nutzt freie Master-Server-Konzepte.

## 🎨 UI Editor & Optimierung
Der UI Editor ermöglicht das freie Verschieben aller Steuerelemente. Die Positionen werden zuverlässig in den `SharedPreferences` gespeichert. Dank optimierter Layout-Gravity gibt es keine Konflikte mehr zwischen absoluten Koordinaten und verschiedenen Bildschirmauflösungen.

## 🔧 Crash-Management
Integrierter Netzwerk-Diagnose-Handler im CrashHandler. Bei Verbindungsfehlern werden detaillierte Logs inklusive Berechtigungsstatus erstellt, die einfach per Knopfdruck in die Zwischenablage kopiert werden können.
