# ShopScanner — Finale Plugin-Spezifikation v3.0

## Zusammenfassung

Ein Purpur/Paper-Plugin für Minecraft 1.21.1. Ein Knochen mit dem Prefix "Scanner " im
Namen wird zum Shop-Werkzeug. Der Text nach "Scanner " definiert den Shop-Namen.
Container werden per Shop-Name gruppiert. Ein Scan schreibt den Inventarbericht
als Buch in ein beliebiges Lectern.

**Sicherheit:** Alle Scanner-Aktionen funktionieren nur innerhalb einer konfigurierbaren
Shopping-District-Zone (Overworld). Beim Scan werden nur Container im 3x3 Chunk-Radius
um das Lectern berücksichtigt.

---

## Kernkonzept

```
Knochen-Name:    "Scanner Diamanten"
                  ├──────┘ └──────┘
                  Prefix    Shop-Name
                  (fix)     (frei wählbar)

→ Shop-Name = "Diamanten"
→ Alle Container die mit diesem Knochen registriert werden,
   gehören zum Shop "Diamanten"
→ Scan schreibt Ergebnisse für Shop "Diamanten" ins Lectern
```

**Kein Pairing. Kein PDC-Tag. Kein Lectern-Binding.**
Alles läuft über den Item-Namen.

---

## Sicherheits-Zonen (NEU in v3)

### Zwei unabhängige Sicherheitsschichten

```
┌─────────────────────────────────────────────────────────────┐
│  SCHICHT 1: Shopping-District-Zone                          │
│  ─────────────────────────────────────                      │
│  Globaler Bereich in der Overworld.                         │
│  Definiert durch: Center-Chunk (X/Z) + Radius in Chunks.   │
│  ALLE Scanner-Aktionen (Registrieren, Deregistrieren,       │
│  Scannen) funktionieren NUR innerhalb dieser Zone.          │
│  Außerhalb: Scanner wird komplett ignoriert.                 │
│                                                             │
│  Default: Center 0,0 — Radius 5 Chunks                     │
│  → Zone von Chunk -5,-5 bis Chunk 5,5                       │
│  → Blockkoordinaten: -80,-80 bis 95,95                      │
│                                                             │
│  ┌─────────────────────────────────────────────────┐        │
│  │  SCHICHT 2: Lectern-Scan-Radius                 │        │
│  │  ──────────────────────────────                  │        │
│  │  Beim Scan: Nur Container im 3x3 Chunk-Bereich  │        │
│  │  um das Lectern werden gescannt.                 │        │
│  │  = Lectern-Chunk ± 1 in X und Z.                │        │
│  │                                                  │        │
│  │  Container die zum Shop gehören aber weiter      │        │
│  │  weg sind: im Buch als "(außerhalb Scan-Radius)" │        │
│  │  markiert, NICHT gescannt.                       │        │
│  └─────────────────────────────────────────────────┘        │
└─────────────────────────────────────────────────────────────┘
```

### Prüf-Logik pro Aktion

```
REGISTRIEREN (Shift+Linksklick auf Container):
  1. Spieler in Shopping-District-Zone?    → nein: "Nur im Shopping District möglich"
  2. Container in Shopping-District-Zone?  → nein: "Container liegt außerhalb der Zone"
  3. → Registrieren

DEREGISTRIEREN (Shift+Rechtsklick auf Container):
  1. Spieler in Shopping-District-Zone?    → nein: "Nur im Shopping District möglich"
  2. → Deregistrieren (auch wenn Container inzwischen außerhalb wäre)

SCAN (Rechtsklick auf Lectern):
  1. Spieler in Shopping-District-Zone?    → nein: "Nur im Shopping District möglich"
  2. Lectern in Shopping-District-Zone?    → nein: "Lectern liegt außerhalb der Zone"
  3. Für jeden Container des Shops:
     a. Container im 3x3 Chunk-Radius um Lectern? → nein: "(außerhalb Scan-Radius)" im Buch
     b. Chunk geladen?                             → nein: "(Chunk nicht geladen)" im Buch
     c. Block noch Container?                      → nein: Auto-Deregister
     d. → Inventar scannen
```

### Zone-Check Implementierung

```java
// Shopping-District-Zone Check
boolean isInShoppingZone(Location loc) {
    // Nur Overworld
    if (loc.getWorld().getEnvironment() != World.Environment.NORMAL) return false;
    // Nur konfigurierte Welt
    if (!loc.getWorld().getName().equals(configWorldName)) return false;

    int chunkX = loc.getBlockX() >> 4;  // Block → Chunk
    int chunkZ = loc.getBlockZ() >> 4;

    return Math.abs(chunkX - centerChunkX) <= radiusChunks
        && Math.abs(chunkZ - centerChunkZ) <= radiusChunks;
}

// Lectern-Scan-Radius Check (3x3 = ±1 Chunk)
boolean isInScanRadius(Location lectern, Location container) {
    int lecternChunkX = lectern.getBlockX() >> 4;
    int lecternChunkZ = lectern.getBlockZ() >> 4;
    int containerChunkX = container.getBlockX() >> 4;
    int containerChunkZ = container.getBlockZ() >> 4;

    return Math.abs(lecternChunkX - containerChunkX) <= 1
        && Math.abs(lecternChunkZ - containerChunkZ) <= 1;
}
```

---

## Identifikation des Scanner-Items

```
Prüf-Reihenfolge:
1. Material == BONE?                         → nein: ignorieren
2. Hat DisplayName?                          → nein: ignorieren
3. DisplayName startet mit "Scanner "?       → nein: ignorieren
   (Leerzeichen nach "Scanner" ist Pflicht)
4. Text nach "Scanner " ist nicht leer?      → nein: ignorieren
5. Hat Permission shopscanner.use?           → nein: Fehlermeldung
6. Spieler in Shopping-District-Zone?        → nein: Fehlermeldung
7. → Shop-Name extrahieren, weiter zur Aktion
```

**Herstellung:** Amboss — beliebigen Knochen in "Scanner MeinShop" umbenennen.
Alternativ: `/shopscanner give <shopname> [player]`

---

## Interaktionen

| Aktion                  | Input                            | Ergebnis                                          |
|--------------------------|----------------------------------|---------------------------------------------------|
| Container registrieren   | Shift + Linksklick auf Container | Container wird zu Shop hinzugefügt                |
| Container deregistrieren | Shift + Rechtsklick auf Container| Container wird aus Shop entfernt                  |
| Scan auslösen            | Rechtsklick auf Lectern          | Buch mit Inventarbericht wird ins Lectern gelegt  |

### Validierungen

| Aktion        | Prüfungen                                                          |
|---------------|---------------------------------------------------------------------|
| Registrieren  | Spieler + Container in Shopping-Zone?                              |
|               | Block ist Container (Chest/Trapped/Barrel/Shulker)?                |
|               | Container nicht bereits in diesem Shop registriert?                 |
|               | Max-Container-Limit nicht erreicht?                                 |
|               | Double-Chest: Andere Hälfte bereits registriert? → Warnung (kein Block) |
| Deregistrieren| Spieler in Shopping-Zone?                                          |
|               | Container ist in diesem Shop registriert?                           |
| Scan          | Spieler + Lectern in Shopping-Zone?                                |
|               | Block ist Lectern?                                                  |
|               | Shop hat mindestens einen Container registriert?                    |
|               | Pro Container: im 3x3 Chunk-Radius um Lectern?                    |

### Event-Handling

- Alle drei Aktionen: `PlayerInteractEvent` abfangen
- Event canceln, damit kein normales Öffnen/Interagieren passiert
- Bei Scan: Lectern-GUI darf sich NICHT öffnen (Event cancel)

---

## Daten-Persistenz

### Datei: `plugins/ShopScanner/shops.yml`

```yaml
shops:
  "Diamanten":
    containers:
      - "world;102;64;200"
      - "world;104;64;200"
      - "world;98;64;200"
  "Erze":
    containers:
      - "world;300;70;150"
      - "world;302;70;152"
  "Redstone Stuff":
    containers:
      - "world;400;64;100"
```

### Speicher-Zeitpunkte

- Nach jeder Registrierung/Deregistrierung
- Bei onDisable()

### Position-Format

`"worldName;x;y;z"` (Block-Koordinaten, ganzzahlig)

---

## Scan-Logik

### Ablauf

```
1. Shop-Name aus Scanner-Item extrahieren
2. Prüfen: Spieler + Lectern in Shopping-Zone
3. Container-Liste für diesen Shop aus YAML laden
4. Für jeden Container:
   a. Im 3x3 Chunk-Radius um Lectern? → nein: "(außerhalb Scan-Radius)" notieren, skip
   b. Chunk geladen? → nein: "(Chunk nicht geladen)" notieren, skip
   c. Block an Position holen
   d. Block ist Container? → nein: Auto-Deregistrieren + "(entfernt)" notieren, skip
   e. Inventar auslesen
   f. Items zählen: Map<Material, Integer> aufbauen
   g. Nach Menge absteigend sortieren
5. Written Book generieren
6. Buch ins Lectern setzen (vorhandenes überschreiben, egal was)
7. Lectern BlockState updaten
8. Sound abspielen
```

### Container-Typen

- `CHEST` (inkl. Double Chest via DoubleChestInventory)
- `TRAPPED_CHEST` (inkl. Double)
- `BARREL`
- `SHULKER_BOX` (alle 17 Farb-Varianten)

### Double Chests

- `((Chest) blockState).getInventory()` liefert automatisch DoubleChestInventory
- Nur eine Hälfte registrieren reicht
- Wenn beide Hälften registriert: Warnung beim Registrieren, Items werden doppelt gezählt

### Item-Zählung

- Über alle Slots iterieren
- AIR ignorieren
- Nach Material gruppieren und Amounts summieren
- Shulker-Inhalte NICHT rekursiv scannen

### Item-Namen

`DIAMOND_BLOCK` → `Diamond Block`
Transformation: Unterstriche → Leerzeichen, jedes Wort Title Case.

---

## Buch-Format

### Seite 1 — Titel

```
══════════════
  Shop Scan
  "Diamanten"
══════════════
02.04.2026 14:30

3 Container
══════════════
```

### Seite 2..N — Pro Container

```
Chest
@ 102, 64, 200
──────────────
Diamond x 128
Iron Ingot x 64
Oak Log x 32
Stick x 12
```

### Sonderfälle

```
Barrel
@ 98, 64, 200
──────────────
(leer)
```

```
Chest
@ 500, 64, 300
──────────────
(außerhalb Scan-Radius)
```

```
Chest
@ 500, 64, 300
──────────────
(Chunk nicht geladen)
```

```
@ 200, 64, 100
──────────────
(kein Container mehr
 - wurde entfernt)
```

### Buch-Limits

- Max 100 Seiten, ~14 Zeilen pro Seite
- Wenn Container mehr Items hat als eine Seite → nächste Seite
- Wenn 100 Seiten voll → letzte Seite: "... weitere nicht darstellbar"

### Lectern-Verhalten beim Scan

- Lectern leer → neues Buch rein
- Lectern hat Buch → überschreiben (egal was für ein Buch)
- Written Book erstellen mit Titel = Shop-Name, Autor = "ShopScanner"

---

## Auto-Deregistrierung

### Bei Block-Zerstörung

| Event               | Logik                                                    |
|----------------------|-----------------------------------------------------------|
| BlockBreakEvent      | Block-Position in allen Shops suchen → entfernen + melden |
| BlockExplodeEvent    | Jeder betroffene Block prüfen → entfernen + melden       |
| EntityExplodeEvent   | Jeder betroffene Block prüfen → entfernen + melden       |

- Chat-Nachricht an den Spieler (bei BlockBreak) oder ans Log (bei Explosion)
- YAML sofort speichern

### Bei Scan

- Wenn ein Container-Block nicht mehr existiert oder kein Container mehr ist:
  → Auto-Deregistrieren + Vermerk im Buch

---

## Commands

| Command                              | Permission         | Beschreibung                            |
|---------------------------------------|--------------------|-----------------------------------------|
| `/shopscanner give <shopname> [player]`| `shopscanner.give` | Gibt Scanner-Knochen für Shop "shopname"|
| `/shopscanner reload`                 | `shopscanner.admin`| Lädt shops.yml + config.yml neu         |
| `/shopscanner list`                   | `shopscanner.admin`| Listet alle Shops + Container-Counts    |

### `/shopscanner give` Verhalten

- Erstellt einen Knochen mit DisplayName `"Scanner <shopname>"`
- Gibt ihn dem Spieler (oder Ziel-Spieler) ins Inventar
- Shopname darf Leerzeichen enthalten: `/shopscanner give Redstone Stuff`
  → DisplayName wird `"Scanner Redstone Stuff"`

---

## Permissions

| Permission          | Default | Beschreibung                      |
|---------------------|---------|-----------------------------------|
| `shopscanner.use`   | `op`    | Scanner benutzen (alle Aktionen)  |
| `shopscanner.give`  | `op`    | give-Command                      |
| `shopscanner.admin` | `op`    | reload + list Commands            |

---

## Config (`config.yml`)

```yaml
# Chat-Präfix
prefix: "§6[Scanner]§r "

# Max Container pro Shop
max-containers-per-shop: 54

# Shopping-District-Zone
# Alle Scanner-Aktionen funktionieren NUR innerhalb dieser Zone.
# Zone = quadratischer Bereich aus Chunks in der Overworld.
shopping-district:
  # Name der Overworld (normalerweise "world")
  world: "world"
  # Center-Chunk Koordinaten
  center-chunk-x: 0
  center-chunk-z: 0
  # Radius in Chunks um den Center-Chunk
  # 5 = Zone von Chunk -5,-5 bis 5,5 (11x11 Chunks = 176x176 Blöcke)
  radius-chunks: 5

# Lectern-Scan-Radius
# Beim Scan werden nur Container im 3x3 Chunk-Bereich (±1) um das Lectern gescannt.
# Container außerhalb werden im Buch als "außerhalb Scan-Radius" markiert.
# Dieser Wert ist fest auf 1 (3x3) und nicht konfigurierbar.

# Sound-Effekte (Bukkit Sound enum names)
sounds:
  register: "BLOCK_CHEST_LOCKED"
  deregister: "BLOCK_CHEST_CLOSE"
  scan-complete: "ENTITY_EXPERIENCE_ORB_PICKUP"
  error: "ENTITY_VILLAGER_NO"
```

---

## Dateistruktur

```
src/main/java/com/example/shopscanner/
├── ShopScannerPlugin.java            # Hauptklasse
├── commands/
│   └── ShopScannerCommand.java       # /shopscanner give|reload|list
├── listeners/
│   ├── ScannerInteractListener.java  # Register, Deregister, Scan
│   └── ContainerBreakListener.java   # Auto-Deregister
├── managers/
│   └── ShopManager.java             # Shop→Container YAML CRUD
├── scanner/
│   ├── ContainerScanner.java        # Inventar auslesen + zählen
│   └── BookGenerator.java           # Written Book generieren
└── utils/
    ├── LocationUtil.java            # "world;x;y;z" ↔ Location
    ├── ItemNameUtil.java            # DIAMOND_BLOCK → Diamond Block
    └── ZoneUtil.java                # Shopping-Zone + Scan-Radius Checks
```

---

## Was NICHT im Scope ist (Phase 1)

- Shulker-Inhalt rekursiv scannen
- Async Chunk Loading für ungeladene Container
- GUI/Menü zum Verwalten
- Mehrere Spieler pro Shop (Permissions pro Shop)
- Economy-Integration
- Konfigurierbarer Lectern-Scan-Radius (fix 3x3)
- Mehrere Shopping-Zonen
- Nether/End-Unterstützung

---

## Zusammenfassung der Änderungen v2 → v3

| v2                               | v3                                        |
|----------------------------------|-------------------------------------------|
| Keine Zonen-Beschränkung         | Shopping-District-Zone (Overworld only)   |
| Scanner überall nutzbar          | Nur innerhalb der Zone                    |
| Alle Container werden gescannt   | Nur Container im 3x3 um Lectern          |
| Kein Missbrauchs-Schutz          | Doppelte Sicherheitsschicht               |
