# Really Useful Ribbits

**[English](README.md)** · **[Русский](README.ru.md)**

A **Minecraft 1.21.1** addon for [Ribbits](https://modrinth.com/mod/ribbits), for NeoForge and Fabric. Set a ribbit's home with a maraca, then let fishers, farmers, merchants, sorcerers and nitwits actually do something. Written in Kotlin. Install one loader's set, not both.

The UI and in-game guidebook are available in English and Russian (the language follows the client).

## Downloads

Jars live on [GitHub Releases](https://github.com/Nergan/really-useful-ribbits-mod/releases/latest). A push to `main` updates one GitHub Release page per Minecraft version and loader. Each page includes `all-jars.zip`.

Download one set and put those files in the `mods` folder.

### NeoForge

| File | Required | What it is |
| --- | --- | --- |
| `reallyusefulribbits-neoforge-1.21.1-1.1.2.jar` | Yes | this addon |
| `kotlinforforge-5.8.0-all.jar` | Yes | [Kotlin for Forge](https://modrinth.com/mod/kotlin-for-forge) |
| `Ribbits-1.21.1-NeoForge-4.1.6.jar` | Yes | [Ribbits](https://modrinth.com/mod/ribbits) |
| `geckolib-neoforge-1.21.1-4.7.6.jar` | Yes | [GeckoLib](https://modrinth.com/mod/geckolib), required by Ribbits |
| `YungsApi-1.21.1-NeoForge-5.1.5.jar` | Yes | [YUNG's API](https://modrinth.com/mod/yungs-api), required by Ribbits |
| `Patchouli-1.21.1-93-NEOFORGE.jar` | No | [Patchouli](https://modrinth.com/mod/patchouli), only if you want the guidebook |

### Fabric

| File | Required | What it is |
| --- | --- | --- |
| `reallyusefulribbits-fabric-1.21.1-1.1.2.jar` | Yes | this addon |
| `fabric-api-0.116.17+1.21.1.jar` | Yes | [Fabric API](https://modrinth.com/mod/fabric-api) |
| `fabric-language-kotlin-1.13.2+kotlin.2.1.20.jar` | Yes | [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) |
| `Ribbits-1.21.1-Fabric-4.1.6.jar` | Yes | [Ribbits](https://modrinth.com/mod/ribbits) |
| `geckolib-fabric-1.21.1-4.7.6.jar` | Yes | [GeckoLib](https://modrinth.com/mod/geckolib), required by Ribbits |
| `YungsApi-1.21.1-Fabric-5.1.5.jar` | Yes | [YUNG's API](https://modrinth.com/mod/yungs-api), required by Ribbits |
| `cloth-config-15.0.140-fabric.jar` | Yes | [Cloth Config](https://modrinth.com/mod/cloth-config), required by Ribbits |
| `Patchouli-1.21.1-93-FABRIC.jar` | No | [Patchouli](https://modrinth.com/mod/patchouli), only if you want the guidebook. Fiber is already inside this jar; do not install Fiber separately |

The release workflow builds the addon and fetches the companion jars from Modrinth. GitHub shows a SHA-256 digest next to each file on the release page. Do not install `*-sources.jar`.

Optional extras (not attached to the release, install them yourself if you want them):

- [Farmer's Delight](https://modrinth.com/mod/farmers-delight) — extra farmland and crops
- [Wandering Ribbit](https://modrinth.com/mod/wandering-ribbit) — merchants will also trade-hunt those frogs
- [Mod Menu](https://modrinth.com/mod/modmenu) on Fabric — opens the settings screen

## Features

- **A new home.** Right-click a ribbit with a **maraca** from the original Ribbits mod to set its home point where it is standing. The old farm, fishing water, and chest are forgotten, so a farmer or a fisher stops walking back to the village. Merchant ribbits sell maracas. This grants *so I guess this is my new home now…* You cannot seat a ribbit on your head.
- **Shared territory.** Every working ribbit scans a configurable radius (64 blocks by default). Several ribbits can work the same area. They do not steal each other’s reserved crop or water tile.
- **Fisher.** Binds one nearby water block and one container. Sits on the bank with the fishing animation, runs vanilla-style fishing (splashes toward the bobber, then a catch), and carries up to 4 items. A slab in the upper half of a water block is not a fishing spot. At 4 it stands up, walks to the container, deposits normal stacks, then returns to the water. Right-click outlines the ribbit, the water tile and the container. Missing a bind plays the hurt croak and angry particles.
- **Farmer.** Binds one connected farm (farmland, Farmer's Delight farmland, soul sand, sugar-cane bases, crops in water or on mud, and bush crops such as tomatoes and grapevines) and one container. Sweet berry bushes and other prickly bushes are left alone. Harvests, plants, tills, waters farmland, and sometimes coaxes glow berries onto vines. Melons, pumpkins, cane and nether wart are included. It walks up to its chest before inserting or taking items. Inventory is 4×64. Right-click sprinkles particles on the farm and the container.
- **Merchant.** 27 slots of 256. Starts with random goods plus 256 emeralds and 256 amethysts. Hunts villagers, wandering traders, wandering ribbits and any other merchant, copies a buyable item (maxed trades, no stock limits), builds up to 16 unique-item amethyst offers, then croaks at a player until you trade or ignore them. Always walks at 1.5× speed. An offer that both buys and sells amethyst shards is never created. One already saved on a merchant is replaced with 64 diamonds for 1 amethyst shard.
- **Sorcerer.** Right-click with an amethyst shard rolls one of 35 spells, see [Sorcerer spells](#sorcerer-spells). Curses survive death. Feeding the sorcerer an enchanted golden apple strips them all and stops world corruption; milk still clears ordinary effects. Endermen in range flee. Otherwise it still wanders like a normal ribbit.
- **Nitwit.** Applies Luck in the scan radius. Picks up ground loot (27 slots of 64) and sometimes carries it to a player, tossing it the way Q does.
- **Guidebook (optional).** With [Patchouli](https://modrinth.com/mod/patchouli) installed, the first join into a world gives *Really Useful Ribbits*. Craft it with a book and a giant lily pad. The book is also in the Ribbits creative tab. Without Patchouli the rest of the addon works as usual. Professions themselves come from Ribbits; this addon only gives those frogs jobs.

## Sorcerer spells

Every spell has points in the server config. A spell's chance is its points divided by the sum of the points of all spells; 0 turns a spell off. The defaults add up to 9,999,092, so 100,000 points is 1% and 1 point is 0.00001%. Together, the disasters that break the world come up in less than one roll out of a hundred. The sorcerer only rolls spells that can happen right now: no enchantment without an enchantable item, no second giant warden while one lives, and so on.

**Default chaos level: 0.** Chaos flattens the odds: each spell weighs its points to the power of `1 - chaos/100`. At 0 the chances are exactly the table below; at 100 every enabled spell is equally likely. Rare spells stay rarer than common ones at any level.

Curses survive death: effects, upside-down, size, max health, morph, nickname, shader, inverted controls, VVVVVV, fireball and lightning hands, and flight. Milk still clears ordinary effects. Everything else comes off only with an enchanted golden apple given to the sorcerer.

| Spell | Key | What it does | Points | Default chance |
| --- | --- | --- | ---: | ---: |
| Random enchantment | `enchant` | A random enchantment on a random item you carry. | 1500000 | 15% |
| Endless effect or cleanse | `effect_or_cleanse` | Half the time an endless random effect, half the time every effect is removed. Milk still clears the effect. | 1211780 | 12.12% |
| Plant growth | `grow_plants` | Bone meal on up to 64 plants around the sorcerer. | 1000000 | 10% |
| Tamed pets | `summon_pets` | 1–8 tamed pets in random colors, modded ones too. | 650000 | 6.5% |
| Super Secret Settings | `super_secret_settings` | A random screen shader from the old Super Secret Settings button. | 550000 | 5.5% |
| Weather | `weather` | Clear skies, rain or a thunderstorm. | 500000 | 5% |
| Day or night | `day_night` | Switches between day and night. | 450000 | 4.5% |
| New nickname | `nickname` | A random funny or cursed nickname, sometimes colored, in chat, above your head and in the player list. | 450000 | 4.5% |
| Size | `scale` | Grows you 1.3–2.5× (60%) or shrinks you to 0.45–0.8×, from 0.25 to 8. | 350000 | 3.5% |
| Inverted controls | `inverted_controls` | Forward is back and left is right (*How do I walk...*). | 350000 | 3.5% |
| VVVVVV gravity | `vvvvvv` | Jump flips your gravity instead of jumping, mid-air too. After death or above the world, gravity pulls down again. No fall damage (*VVVVVV*). | 350000 | 3.5% |
| Launch | `launch` | Throws you 4–16 blocks up. | 300000 | 3% |
| Upside down | `flip` | Turns your model upside down, or back. | 300000 | 3% |
| Max health | `max_health` | Max health becomes 1–128. | 300000 | 3% |
| Diamond rain | `diamond_rain` | 256 diamonds fall around the sorcerer. | 300000 | 3% |
| Summon mobs | `summon_mobs` | 1–16 mobs of one random kind, usually peaceful. | 250000 | 2.5% |
| Structure trip | `structure_trip` | Ten invisible seconds at a nearby village, portal or map structure, then back. | 200000 | 2% |
| Fireball hands | `fireballs` | Right-click air with an empty hand to throw a ghast fireball (*Once, the Fire Nation...*). | 200000 | 2% |
| Lightning hands | `lightning_hands` | Right-click air with an empty hand: lightning flies from your hand in a straight line to the server's view distance (*For the Glory of Perun!*). | 150000 | 1.5% |
| Giant cat | `giant_cat` | A tamed cat fifty times bigger, sitting (*Musya?!*). | 150000 | 1.5% |
| Creative flight | `creative_flight` | Flight as in creative mode (*True Freedom*). | 120000 | 1.2% |
| Lightning | `lightning` | Lightning next to the sorcerer. It does not hurt the frog. | 100000 | 1% |
| Morph | `morph` | Turns you into a random mob. Only humanoid mobs can still place, break and open things. | 100000 | 1% |
| Spectator hour | `spectator` | An hour in spectator mode (*This ghost will be kind, right?*). | 60000 | 0.6% |
| Random dimension | `random_dimension` | Teleports you to a safe spot in another dimension. | 30000 | 0.3% |
| Random structure | `random_structure` | Builds a random structure the server knows, modded ones too, in the sorcerer's radius. | 30000 | 0.3% |
| Ring of fire | `ignite_area` | Sets fire to the ground around the sorcerer. | 20000 | 0.2% |
| Giant phantom | `giant_phantom` | A huge glowing phantom (*Foma, is that you?*). | 20000 | 0.2% |
| Bedrock room | `bedrock_room` | Locks you in a 16×16×16 bedrock room. Five minutes later the old blocks come back (*Walled In!*). | 5000 | 0.05% |
| Lava lake in the sky | `lava_lake` | A 128×128 lake of lava at the top of the world above the sorcerer (*And so it was written in the scriptures of Ada Abdulov...*). | 1000 | 0.01% |
| Fireball rain | `fireball_rain` | 48 fireballs fall from the top of the world around the sorcerer and explode like TNT (*Apocalypse*). | 800 | 0.008% |
| Mega explosion | `mega_explosion` | A crater far stronger than TNT. The frog is unharmed (*no magic today*). | 500 | 0.005% |
| Corrupted world generation | `corrupt_worldgen` | Every chunk generated from now on gets broken shapes and wrong blocks: sculk, barriers, end stone and more (*worse than losing*). | 10 | 0.0001% |
| Ender dragon | `ender_dragon` | An ender dragon above the sorcerer (*You asked for it*). | 1 | 0.00001% |
| Giant warden | `delete_world` | A warden 100× bigger with 10 000 health hunts every player. Eternal night, a blood-red sky and fast sculk until it dies; its killer gets creative mode (*delete the world*, *But at what cost?*). | 1 | 0.00001% |

## Requirements

Minecraft 1.21.1 and Java 21 on either loader.

### NeoForge

| Component | Version |
| --- | --- |
| NeoForge | 21.1.209 (any 21.1.x should work) |
| Kotlin for Forge | 5.8.0, **NeoForge** build |
| Ribbits | 1.21.1-NeoForge-4.1.6 |
| GeckoLib | 4.7.6 (NeoForge 1.21.1) |
| YUNG's API | 1.21.1-NeoForge-5.1.5 |
| Patchouli | `1.21.1-93-neoforge`, only if you want the book |

### Fabric

| Component | Version |
| --- | --- |
| Fabric Loader | 0.16.14 or newer |
| Fabric API | 0.116.17+1.21.1 |
| Fabric Language Kotlin | 1.13.2+kotlin.2.1.20 |
| Ribbits | 1.21.1-Fabric-4.1.6 |
| GeckoLib | 4.7.6 (Fabric 1.21.1) |
| YUNG's API | 1.21.1-Fabric-5.1.5 |
| Cloth Config | 15.0.140, required by Ribbits |
| Patchouli | `1.21.1-93-fabric`, only if you want the book |

## Installation

1. Install NeoForge 21.1.x or Fabric Loader for 1.21.1.
2. Download the matching set from [the latest Release](https://github.com/Nergan/really-useful-ribbits-mod/releases/latest).
3. Put that set in `mods`. Do not mix NeoForge jars into a Fabric instance, or the other way around.
4. Optionally add the Patchouli jar for the same loader.

The addon is required on both client and server. You can also get the companion mods from [Modrinth](https://modrinth.com/mod/ribbits) instead of the GitHub release.

## Configuration

| Option | Default | Meaning |
| --- | --- | --- |
| `scan_radius` | `64` | How far a ribbit looks for water, farms, containers, luck, endermen and sorcerer plant-growth |
| `chaos_level` | `0` | How even sorcerer odds become: 0 keeps the points as they are, 100 makes every enabled spell equally likely |
| spell points | see [Sorcerer spells](#sorcerer-spells) | One key per spell, 0 to 10000000. 0 turns the spell off |

### NeoForge

In-game: Mods → Really Useful Ribbits → Config.

File: `config/reallyusefulribbits-server.toml` in the game or server directory. A copy in `<world>/serverconfig/` overrides it for one world. Spell points are the `[sorcerer.spells]` section, one key per spell. In a file from an older version, which has no such section, `chaos_level = 25` was the old default and becomes 0. A section whose points are all still the 1.1.0 or 1.1.2 defaults gets the new defaults; an edited section stays as it is.

This is a `SERVER` config: the server owns the values and syncs them to clients. In multiplayer only the server setting matters.

### Fabric

With [Mod Menu](https://modrinth.com/mod/modmenu): Mods → Really Useful Ribbits → settings. The screen edits `config/reallyusefulribbits-server.properties`; in single player the new values apply as soon as you save. A dedicated server reads that file when it starts. Keys are `scan_radius` (default 64), `chaos_level` (default 0) and one `spell.<key>` per spell, for example `spell.ender_dragon=93`. A file from an older version, which has no spell keys, is rewritten with every key; its `chaos_level=25` was the old default and becomes 0. A file whose points are all still the 1.1.0 or 1.1.2 defaults is rewritten with the new defaults. On a dedicated server the file is in the server directory. Changing it needs a restart. It is one file for the whole instance, not a per-world config.

## License

The addon code is [MPL-2.0](LICENSE). Ribbits, GeckoLib, YUNG's API, Kotlin for Forge, Fabric API, Fabric Language Kotlin and Cloth Config keep their own licenses. Patchouli is optional and keeps CC-BY-NC-SA-3.0. Vanilla Minecraft assets are not shipped.
