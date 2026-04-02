# ShopScanner

A Purpur/Paper 1.21+ plugin for scanning shop container inventories and generating book reports.

## Overview

ShopScanner allows server administrators to create "scanner" items that can register containers (chests, barrels, shulker boxes, etc.) to named shops and generate inventory reports as Written Books placed in lecterns.

Perfect for shopping districts where shop owners want to track their inventory across multiple containers.

## Features

- **Scanner Item**: A bone renamed to "Scanner \<shopname\>" becomes a shop management tool
- **Container Registration**: Register any container to a shop with Shift+Left-click
- **Inventory Scanning**: Scan all registered containers and generate a book report
- **Particle Highlighting**: See registered containers highlighted with particles when holding the scanner
- **Auto-Deregister**: Containers are automatically removed from shops when broken
- **Diamond Counter**: Track total diamonds across all containers (useful for sales tracking)
- **Zone Restrictions**: Limit scanner usage to a configured shopping district area
- **Safe Book Replacement**: Only overwrites empty books or same-shop scan results

## Requirements

- Purpur or Paper server 1.21.1+
- Java 21+

## Installation

1. Download `ShopScanner-1.0.0.jar` from [Releases](https://github.com/paraf0x/shopping-district-scanner/releases)
2. Place the JAR in your server's `plugins/` folder
3. Restart the server
4. Configure `plugins/ShopScanner/config.yml` as needed

## Commands

| Command | Permission | Description |
|---------|------------|-------------|
| `/shopscanner give <shopname> [player]` | `shopscanner.give` | Give a scanner item for the specified shop |
| `/shopscanner reload` | `shopscanner.admin` | Reload configuration and shop data |
| `/shopscanner list` | `shopscanner.admin` | List all shops and their container counts |

**Aliases:** `/scanner`, `/ss`

## Permissions

| Permission | Default | Description |
|------------|---------|-------------|
| `shopscanner.use` | op | Use the scanner (all actions) |
| `shopscanner.give` | op | Use /shopscanner give command |
| `shopscanner.admin` | op | Use /shopscanner reload and list commands |

## How to Use

### 1. Get a Scanner

```
/shopscanner give MyShop
```

This gives you a bone named "Scanner MyShop" with lore "Shop: MyShop".

You can also rename any bone to "Scanner \<shopname\>" using an anvil.

### 2. Register Containers

Hold the scanner and **Shift+Left-click** on any container (chest, barrel, shulker box, etc.) to register it to the shop.

- Green particles will appear around registered containers when holding the scanner
- A warning appears if you register both halves of a double chest (items may be counted twice)

### 3. Scan Inventory

1. Place a **Book and Quill** in a lectern
2. Hold your scanner and **Right-click** the lectern
3. The book is replaced with a Written Book containing the inventory report

### 4. Deregister Containers

Hold the scanner and **Shift+Right-click** on a registered container to remove it from the shop.

Containers are also automatically deregistered when broken.

## Book Report Format

**Title Page:**
```
---------------
  Shop Scan

  "MyShop"

---------------
2024-04-02 12:34

3 Containers
Diamonds: 42
---------------
```

**Container Pages:**
```
Chest
@ 100, 64, 200
---------------
Diamond x42
Iron Ingot x128
Gold Ingot x64
...
```

Items are sorted by quantity (highest first). Long item names are truncated to fit.

## Configuration

```yaml
# Chat prefix for all messages
prefix: "§6[Scanner]§r "

# Maximum containers per shop
max-containers-per-shop: 54

# Shopping District Zone
# All scanner actions only work within this zone.
shopping-district:
  world: "world"
  center-chunk-x: 0
  center-chunk-z: 0
  radius-chunks: 5  # 11x11 chunks = 176x176 blocks

# Sound effects
sounds:
  register: "block.chest.locked"
  deregister: "block.chest.close"
  scan-complete: "entity.experience_orb.pickup"
  error: "entity.villager.no"

# Container Highlighting
highlighting:
  enabled: true
  container-color: "#00FF00"
  particle-count: 8
  update-interval: 10  # ticks
```

## Controls Summary

| Action | Scanner Required | Description |
|--------|-----------------|-------------|
| Shift+Left-click container | Yes | Register container to shop |
| Shift+Right-click container | Yes | Deregister container from shop |
| Right-click lectern | Yes | Scan and generate book |
| Hold scanner | Yes | Show particle highlights |

## Technical Notes

- **Scan Radius**: Only containers within a 3x3 chunk area around the lectern are fully scanned. Containers outside this range are marked as "out of scan range".
- **Chunk Loading**: Containers in unloaded chunks are marked as "chunk not loaded".
- **Double Chests**: Scanning one half of a double chest includes items from both halves.
- **Shulker Boxes**: Counted as single items; contents are not recursively scanned.
- **Book Safety**: Scanning only works with an empty Book and Quill or a previous scan result from the same shop.

## Building from Source

```bash
git clone https://github.com/paraf0x/shopping-district-scanner.git
cd shopping-district-scanner
./gradlew build
```

The JAR will be in `build/libs/`.

## License

MIT License

## Author

paraf0x
