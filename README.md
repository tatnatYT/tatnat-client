# tatnat client

The in-game side of the [tatnat launcher](https://github.com/tatnatYT/tatnat-launcher): press **Right Shift**
for a Feather-style mod menu with a drag-and-drop HUD editor and PvP / quality-of-life mods (keystrokes,
CPS/FPS, armor and potion status, zoom, full bright, freecam, capes, nick hider, block overlay, waypoints...).
The launcher adds it automatically; you don't need to install it yourself.

## Supported

| Loader | Minecraft |
|---|---|
| Fabric | 1.14.4 – 26.3 |
| Legacy Fabric | 1.8.9 |
| Forge | 1.8.9, 1.20.1 |
| NeoForge | 1.21.1 – 26.3 |

## Layout

- `src/main` – the shared core (plain Java 8, no Minecraft imports): modules, settings, menu, HUD editor.
- `src/mcX_Y` – one platform folder per range of Minecraft versions (Mojang names), listed in `targets.json`.
- `legacy/` – Legacy Fabric build for 1.8.9; `legacy/forge/` turns it into the Forge 1.8.9 jar.
- `neoforge/`, `forge/` – NeoForge and Forge 1.20.1 builds reusing the same core and platform folders.
- `bundle.js` – builds every jar and copies them, with `builds.json`, into the launcher's `assets/mods`.

## Building

Needs JDK 21 (`JAVA_HOME`), plus JDK 25 (`JAVA25_HOME`) for 26.x, JDK 17 (`JAVA17_HOME`) for Forge 1.20.1
and JDK 8 (`JAVA8_HOME`) to run the pre-1.17 dev clients.

```bash
node build.js all          # every Fabric target into dist/
node bundle.js             # every loader, then copy into ../minecraft-launcher/assets/mods
node devtest.js 1.21.1 main  # scripted in-game test against a local dev server
```

Made by tatnat · [YouTube](https://www.youtube.com/@tatnatmc)
