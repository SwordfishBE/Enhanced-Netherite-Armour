# Enhanced Netherite Armour

[![GitHub Release](https://img.shields.io/github/v/release/SwordfishBE/Enhanced-Netherite-Armour?display_name=release&logo=github)](https://github.com/SwordfishBE/Enhanced-Netherite-Armour/releases)
[![GitHub Downloads](https://img.shields.io/github/downloads/SwordfishBE/Enhanced-Netherite-Armour/total?logo=github)](https://github.com/SwordfishBE/Enhanced-Netherite-Armour/releases)
[![Modrinth Downloads](https://img.shields.io/modrinth/dt/lZiW97rL?logo=modrinth&logoColor=white&label=Modrinth%20downloads)](https://modrinth.com/mod/enhanced-netherite-armour)
[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1515682?logo=curseforge&logoColor=white&label=CurseForge%20downloads)](https://www.curseforge.com/minecraft/mc-mods/enhanced-netherite-armour)

Give a full Netherite set the extra heat-proof edge it deserves.

**Enhanced Netherite Armour** is a simple Fabric mod that grants **Fire Resistance** while a player is wearing:

- Netherite Helmet
- Netherite Chestplate
- Netherite Leggings
- Netherite Boots

Remove any one of those pieces, and the effect is removed again.

---

## ✨ Features

- Grants Fire Resistance while wearing a full Netherite armor set
- Removes the effect as soon as the full set is no longer worn
- Simple commented config file
- Optional player toggle with `/ena enable` and `/ena disable`
- OP-only `/ena reload` command
- Optional LuckPerms support through `fabric-permissions-api`
- Optional Mod Menu support
- Optional Cloth Config screen when Cloth Config is installed
- Optional compatibility with Armored Elytra and a matching datapack format

---

## ‼️ Supported Armor Combinations

By default, the mod works with the standard full Netherite set:

- Netherite Helmet
- Netherite Chestplate
- Netherite Leggings
- Netherite Boots

When `armoredElytraSupport` is enabled, the mod also supports:

- Netherite Helmet
- Netherite Armored Elytra
- Netherite Leggings
- Netherite Boots

---

## ‼️ Armored Elytra And Datapack Support

This mod can detect compatible armored elytra items from:

- The [**Armored Elytra**](https://modrinth.com/datapack/elytra-armor) mod
- the [**Vanilla Tweaks Armored Elytra**](https://www.vanillatweaks.net) datapack

Support for both can be enabled or disabled with one config option:

- `armoredElytraSupport`

Important:

- Armored Elytra is **not required** for this mod to run
- Cloth Config is **not required** for this mod to run
- Mod Menu is **not required** for this mod to run
- Existing supported armored elytra items can still be recognized from their item data

---

## 🎮 Commands

- `/ena`  
  Shows the current armor and toggle status

- `/ena enable`  
  Enables the effect for the player when player toggles are allowed

- `/ena disable`  
  Disables the effect for the player when player toggles are allowed

- `/ena reload`  
  Reloads the config file  
  OP only

---

## 📊 Permissions

When `useLuckPerms` is enabled and LuckPerms is installed, the mod uses these permission nodes:

- `enhancednetheritearmour.use`  
  Allows the Fire Resistance effect itself

- `enhancednetheritearmour.toggle`  
  Allows use of `/ena enable` and `/ena disable`

If LuckPerms support is disabled, the mod falls back to normal open access behavior for these features.

`/ena reload` always stays OP only.

---

## ⚙️ Config

The config file is written as commented JSON:

- `config/enhancednetheritearmour.json`

Available options:

- `enabled`  
  Master switch for the mod

- `useLuckPerms`  
  Enables LuckPerms permission checks when LuckPerms is installed

- `allowPlayerToggle`  
  Lets players use `/ena enable` and `/ena disable`

- `armoredElytraSupport`  
  Enables support for Armored Elytra items and the compatible datapack format
  
---

## ‼️ Optional Client Integration

The mod supports **Mod Menu** integration.

If **Cloth Config** is also installed, a full config screen becomes available in-game.

This means:

- Singleplayer with Mod Menu + Cloth Config: full config screen
- Dedicated server: works normally without Cloth Config
- Client without Cloth Config: mod still works, just without the config GUI

---

## ❓ Why This Mod?

Netherite is built for the Nether. Walking into lava with a full set should feel a little more rewarding.

Enhanced Netherite Armour keeps the idea simple, vanilla-friendly, and server-friendly:

- one effect
- one clear armor rule
- lightweight config
- optional permissions
- optional compatibility support

---

## 📦 Installation

| Platform   | Link |
|------------|------|
| GitHub     | [Releases](https://github.com/SwordfishBE/Enhanced-Netherite-Armour/releases) |
| Modrinth | [Enhanced Netherite Armour](https://modrinth.com/mod/enhanced-netherite-armour) |
| CurseForge | [Enhanced Netherite Armour](https://www.curseforge.com/minecraft/mc-mods/enhanced-netherite-armour) |


1. Download the latest JAR from your preferred platform above.
2. Place the JAR in your server's `mods/` folder.
3. Make sure [Fabric API](https://modrinth.com/mod/fabric-api) is also installed.
4. Start Minecraft — the config file will be created automatically.

---

## 🧱 Building from Source

```bash
git clone https://github.com/SwordfishBE/Enhanced-Netherite-Armour.git
cd Enhanced-Netherite-Armour
chmod +x gradlew
./gradlew build
# Output: build/libs/enhancednetheritearmour-<version>.jar
```

---

## 📄 License

Released under the [AGPL-3.0 License](LICENSE).