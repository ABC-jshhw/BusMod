
#3 Bus Mod

A small NeoForge mod for Minecraft **1.21.1** that adds a decorative and
configurable bus stop block.

## Features

- **Bus Stop block** – a small station model with a roof, back wall, bench and
  a front sign. It rotates to face the player when placed. The collision and
  selection boxes mirror the model elements, so you can walk into the shelter
  and sit on the bench area.
- **Editable stop data** – right-click the block to open a screen where you can
  set a stop name and a stop id (32 characters each). Control characters and
  `§` formatting codes are stripped before the values are stored.
- **Floating label** – the configured name and id are rendered above the block
  and are visible to every player. The label is depth-tested (it does not shine
  through walls) and is culled beyond 32 blocks. The data is saved in the chunk
  and synced from the server.
- **Permission check** – only creative-mode players and operators (permission
  level 2) may rename a stop; everyone else gets a "no permission" message.
  The server also rejects updates sent from more than 4 blocks away.
- **Crafting recipe** – iron ingots, smooth stone and oak planks:

  ```
  III
  SSS
  P P
  ```

  (`I` = iron ingot, `S` = smooth stone, `P` = oak planks)
- **Creative tab** – the block is available in the *Bus Mod* creative tab and
  in the vanilla *Functional Blocks* tab.
- **Localization** – English (`en_us`) and Simplified Chinese (`zh_cn`).

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.264 or newer
- Java 21

## Building

```bash
./gradlew build
```

The mod jar is written to `build/libs/busmod-1.0.0.jar`.

The GitHub Actions workflow in `.github/workflows/build.yml` runs the same
build on every push and uploads the resulting jar as an artifact. Compilation
is intentionally delegated to CI; the local machine only needs a JDK 21 for
syntax checks.

## Development

- Main mod class: `com.example.busmod.BusMod`
- Client hooks: `com.example.busmod.BusModClient`
- Block and block entity: `com.example.busmod.block`
- Screen and block entity renderer: `com.example.busmod.client`
- Registries: `com.example.busmod.registry`
- Network payload: `com.example.busmod.network`

Run the client with:

```bash
./gradlew runClient
```

## License

All Rights Reserved. See `TEMPLATE_LICENSE.txt` for the template license.

The project is based on the NeoForge Mod Developer Kit (MDK).
