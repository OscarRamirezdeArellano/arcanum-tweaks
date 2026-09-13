# Arcanum Tweaks

**Unofficial addon for [Arcanum](https://www.curseforge.com/minecraft/mc-mods/arcanum)** (Minecraft 26.2, NeoForge).

Arcanum is made by HellBreecher. This addon contains no code or assets from Arcanum.

## Features

### Configurable Arcanum gear

Change the damage, mining speed, durability and armor toughness of every Arcanum weapon, tool
and armor piece, per material tier.

**Every default is the original Arcanum value.** Installed as-is, this addon changes nothing
about gameplay: it only adds the JEI pages below.

Items players already own update automatically and keep their enchantments.

### JEI pages for what Arcanum doesn't show

- **Arcanum Upgrades**: the tier upgrade recipes (Diamond → Blood → Void → Infernal Diamond).
- **Arcanum Fermenter**: fermenting recipes.
- **Firebolt Transmutation**: how to get Infernal Diamond and the whole Infernal tier.
- **Binding Rituals**: how to make the Arcane Codex and the Forbidden Grimoire, and where the
  bindings are found.

Upgrade and fermenter recipes are read from your installed Arcanum jar, so they stay in sync with
Arcanum updates. Available in English and Spanish.

## Requirements

| | |
|---|---|
| Minecraft | 26.2 |
| NeoForge | 26.2.0.82 or newer |
| Arcanum | 26.2-10.2.1.0 only (newer versions include official JEI support) |
| JEI | optional (needed for the JEI pages) |

Install on **both server and clients**.

## Configuration

`config/arcanum_tweaks-startup.toml` is created on first launch. One section per tier
(`green_sapphire`, `blood_diamond`, `void_diamond`, `infernal_diamond`, `infernal`):

| Key | Meaning |
|---|---|
| `sword_damage`, `axe_damage`, `pickaxe_damage`, `shovel_damage`, `hoe_damage`, `beating_stick_damage` | Damage as shown in-game |
| `mining_speed` | Tool mining speed |
| `durability` | Tools, swords and shears. `0` = unbreakable |
| `armor_toughness` | Armor toughness per piece |

> ⚠️ **The server and every client must use the same file.**
> The values are applied while the game starts, before a world loads, so NeoForge does not sync
> this config from the server. A client with different values will see wrong damage and mining
> times.

An example rebalance is included in [`examples/rebalance-example.toml`](examples/rebalance-example.toml):
Arcanum gear stays stronger than netherite without one-shotting everything
(swords 9 / 11 / 13 / 16 / 20 across the five tiers).

To use it: download [`rebalance-example.toml`](examples/rebalance-example.toml), rename it to
`arcanum_tweaks-startup.toml` and put it in the `config` folder of the server **and** of every
client, then restart the game.

## Building

Requires Java 25.

```
gradlew build
```

The jar is written to `build/libs/`. `gradlew runServer` starts a local test server using the
mods in `run/mods/`; the log lists every item whose values changed.

## License

MIT. See [LICENSE](LICENSE).
