# Really Useful Ribbits

**[English](README.md)** · **[Русский](README.ru.md)**

A **Minecraft 1.21.1** NeoForge addon for [Ribbits](https://modrinth.com/mod/ribbits). Set a ribbit's home with a maraca, then let fishers, farmers, merchants, sorcerers and nitwits actually do something. Written in Kotlin with [Kotlin for Forge](https://modrinth.com/mod/kotlin-for-forge).

The UI and in-game guidebook are available in English and Russian (the language follows the client).

## Downloads

Jars live on [GitHub Releases](https://github.com/Nergan/really-useful-ribbits-mod/releases/latest). A push to `main` updates the files on the current version’s release.

Download these files and put them in the `mods` folder:

| File | Required | What it is |
| --- | --- | --- |
| `reallyusefulribbits-1.0.0.jar` | Yes | this addon |
| `kotlinforforge-5.8.0-all.jar` | Yes | [Kotlin for Forge](https://modrinth.com/mod/kotlin-for-forge) |
| `Ribbits-1.21.1-NeoForge-4.1.6.jar` | Yes | [Ribbits](https://modrinth.com/mod/ribbits) |
| `geckolib-neoforge-1.21.1-4.7.6.jar` | Yes | [GeckoLib](https://modrinth.com/mod/geckolib), required by Ribbits |
| `YungsApi-1.21.1-NeoForge-5.1.5.jar` | Yes | [YUNG's API](https://modrinth.com/mod/yungs-api), required by Ribbits |
| `Patchouli-1.21.1-93-NEOFORGE.jar` | No | [Patchouli](https://modrinth.com/mod/patchouli), only if you want the guidebook |

The release workflow builds the addon and fetches the companion jars from Modrinth. GitHub shows a SHA-256 digest next to each file on the release page. Do not install `*-sources.jar`.

Optional extras (not attached to the release, install them yourself if you want them):

- [Farmer's Delight](https://modrinth.com/mod/farmers-delight) — extra farmland and crops
- [Wandering Ribbit](https://modrinth.com/mod/wandering-ribbit) — merchants will also trade-hunt those frogs

## Features

- **A new home.** Right-click a ribbit with a **maraca** from the original Ribbits mod to set its home point where it is standing. Merchant ribbits sell maracas. This grants *so I guess this is my new home now…* You cannot seat a ribbit on your head.
- **Shared territory.** Every working ribbit scans a configurable radius (64 blocks by default). Several ribbits can work the same area. They do not steal each other’s reserved crop or water tile.
- **Fisher.** Binds one nearby water block and one container. Sits on the bank with the fishing animation, runs vanilla-style fishing (splashes toward the bobber, then a catch), and carries up to 4 items. At 4 it stands up, walks to the container, deposits normal stacks, then returns to the water. Right-click outlines the ribbit, the water tile and the container. Missing a bind plays the hurt croak and angry particles.
- **Farmer.** Binds one connected farm (farmland, Farmer's Delight farmland, soul sand, sugar-cane bases) and one container. Harvests, plants, tills, waters farmland, and sometimes coaxes glow berries onto vines. Melons, pumpkins, cane and nether wart are included. It walks up to its chest before inserting or taking items. Inventory is 4×64. Right-click sprinkles particles on the farm and the container.
- **Merchant.** 27 slots of 256. Starts with random goods plus 256 emeralds and 256 amethysts. Hunts villagers, wandering traders, wandering ribbits and any other merchant, copies a buyable item (maxed trades, no stock limits), builds up to 16 unique-item amethyst offers, then croaks at a player until you trade or ignore them. Always walks at 1.5× speed.
- **Sorcerer.** Right-click rolls a weighted chaos table: infinite effect or cleanse, enchant, launch, weather, day/night, peaceful summon, lightning, flip, scale, max health, grow plants, diamond rain (~1%), random dimension, or a very rare ender dragon (*You asked for it*). Endermen in range flee. Otherwise it still wanders like a normal ribbit.
- **Nitwit.** Applies Luck in the scan radius. Picks up ground loot (27 slots of 64) and sometimes carries it to a player, tossing it the way Q does.
- **Guidebook (optional).** With [Patchouli](https://modrinth.com/mod/patchouli) installed, the first join into a world gives *Really Useful Ribbits*. Craft it with a book and a giant lily pad. The book is also in the Ribbits creative tab. Without Patchouli the rest of the addon works as usual. Professions themselves come from Ribbits; this addon only gives those frogs jobs.

## Requirements

| Component | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| NeoForge | 21.1.209 (any 21.1.x should work) |
| Kotlin for Forge | 5.8.0, **NeoForge** build |
| Ribbits | 1.21.1-NeoForge-4.1.6 |
| GeckoLib | 4.7.6 (NeoForge 1.21.1) |
| YUNG's API | 1.21.1-NeoForge-5.1.5 |
| Java | 21 |
| Patchouli | any 1.21.1 build, only if you want the book |

## Installation

1. Install NeoForge 1.21.1.
2. Download the jars from [the latest Release](https://github.com/Nergan/really-useful-ribbits-mod/releases/latest).
3. Put this addon, Kotlin for Forge, Ribbits, GeckoLib and YUNG's API in `mods`.
4. Optionally add `Patchouli-1.21.1-93-NEOFORGE.jar` from the same release.

The addon is required on both client and server. You can also get the companion mods from [Modrinth](https://modrinth.com/mod/ribbits) instead of the GitHub release.

## Configuration

In-game: Mods → Really Useful Ribbits → Config.

World file: `saves/<world>/serverconfig/reallyusefulribbits-server.toml`.

Dedicated server: `world/serverconfig/reallyusefulribbits-server.toml`. This is a `SERVER` config: the server owns the values and syncs them to clients. In multiplayer only the server setting matters.

| Option | Default | Meaning |
| --- | --- | --- |
| `scan_radius` | `64` | How far a ribbit looks for water, farms, containers, luck, endermen and sorcerer plant-growth |

## License

The addon code is [MPL-2.0](LICENSE). Ribbits, GeckoLib, YUNG's API and Kotlin for Forge keep their own licenses. Patchouli is optional and keeps CC-BY-NC-SA-3.0. Vanilla Minecraft assets are not shipped.
