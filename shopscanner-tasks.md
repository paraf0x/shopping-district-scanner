# ShopScanner — Agent Task Breakdown

## Anweisung an den Agenten

Lies zuerst die vollständige Spezifikation in `shopscanner-spec-v3-final.md`.
Arbeite die Tasks in der angegebenen Reihenfolge ab. Jeder Task baut auf den
vorherigen auf. Schreibe vollständigen, kompilierbaren Code — keine Platzhalter,
keine TODOs, keine Stubs.

Technologie: Purpur/Paper Plugin, Java 21, Gradle Kotlin DSL,
`purpur-api:1.21.1-R0.1-SNAPSHOT`, Adventure Components, `paper-plugin.yml`.

---

## Task 1: Projekt-Skeleton + Build-Config

### Ziel
Gradle-Projekt aufsetzen, das kompiliert und ein leeres Plugin-JAR erzeugt.

### Dateien erstellen
- `build.gradle.kts` — Gradle Build mit purpur-api Dependency
- `settings.gradle.kts` — Projektname "ShopScanner"
- `src/main/resources/paper-plugin.yml` — Plugin-Descriptor mit api-version 1.21
- `src/main/resources/config.yml` — Default-Config (alle Werte aus Spec Abschnitt "Config")
- `src/main/java/com/example/shopscanner/ShopScannerPlugin.java` — Hauptklasse, nur `onEnable`/`onDisable` mit Logger-Ausgabe + `saveDefaultConfig()`

### Akzeptanzkriterien
- `./gradlew build` erzeugt ein JAR unter `build/libs/`
- Plugin lädt auf einem Purpur 1.21.1 Server ohne Fehler
- `config.yml` wird beim ersten Start im Plugin-Ordner erzeugt
- Konsolenausgabe: "ShopScanner aktiviert" / "ShopScanner deaktiviert"

---

## Task 2: Utility-Klassen

### Ziel
Die drei Hilfsklassen erstellen, die von allen anderen Komponenten genutzt werden.

### Dateien erstellen

**`src/main/java/com/example/shopscanner/utils/LocationUtil.java`**
- `static String serialize(Location loc)` → `"worldName;x;y;z"` (Block-Koordinaten, int)
- `static Location deserialize(String str)` → Location (gibt null zurück wenn Welt nicht existiert)
- Null-safe, keine Exceptions nach außen

**`src/main/java/com/example/shopscanner/utils/ItemNameUtil.java`**
- `static String formatMaterialName(Material mat)` → `DIAMOND_BLOCK` wird zu `Diamond Block`
- Transformation: Unterstriche → Leerzeichen, jedes Wort Title Case
- Leerer Input → leerer Output

**`src/main/java/com/example/shopscanner/utils/ZoneUtil.java`**
- `static boolean isInShoppingZone(Location loc, ShopScannerPlugin plugin)` 
  - Prüft: Welt == konfigurierte Welt (config `shopping-district.world`)
  - Prüft: Environment == NORMAL
  - Prüft: Chunk-Koordinaten innerhalb Center ± Radius
  - Config-Werte: `shopping-district.center-chunk-x`, `center-chunk-z`, `radius-chunks`
- `static boolean isInScanRadius(Location lectern, Location container)`
  - Prüft: Gleiche Welt
  - Prüft: Container-Chunk innerhalb Lectern-Chunk ± 1 (3x3)

### Akzeptanzkriterien
- Alle Methoden sind statisch und pure (keine Seiteneffekte)
- LocationUtil round-trippt korrekt: `deserialize(serialize(loc)).equals(loc)` (Block-Precision)
- ZoneUtil gibt false zurück für Nether/End Locations
- ItemNameUtil behandelt Edge Cases: `AIR` → `Air`, `OAK_LOG` → `Oak Log`

---

## Task 3: ShopManager (Datenpersistenz)

### Ziel
CRUD-Operationen für Shop→Container-Zuordnungen mit YAML-Persistenz.

### Datei erstellen

**`src/main/java/com/example/shopscanner/managers/ShopManager.java`**

### Konstruktor
- Erhält `ShopScannerPlugin` Instanz
- Lädt `shops.yml` aus Plugin-Datenordner (erstellen falls nicht vorhanden)

### Methoden
- `void load()` — YAML laden, in `Map<String, List<String>>` parsen (Shop-Name → Liste von Location-Strings)
- `void save()` — Map zurück in YAML schreiben
- `boolean addContainer(String shopName, Location loc)` — Container hinzufügen, true bei Erfolg, false wenn schon vorhanden
- `boolean removeContainer(String shopName, Location loc)` — Container entfernen, true bei Erfolg
- `void removeContainerFromAllShops(Location loc)` — Container aus ALLEN Shops entfernen (für Auto-Deregister)
- `List<Location> getContainers(String shopName)` — Alle Container eines Shops als Location-Liste
- `boolean hasShop(String shopName)` — Existiert der Shop
- `int getContainerCount(String shopName)` — Anzahl Container eines Shops
- `Map<String, Integer> listAllShops()` — Alle Shop-Namen mit Container-Count (für /list Command)
- `List<String> findShopsContaining(Location loc)` — Welche Shops referenzieren diesen Container (für Auto-Deregister)

### Internes Datenmodell
```java
private Map<String, List<String>> shops; // Shop-Name → Liste von "world;x;y;z"
```

### Akzeptanzkriterien
- `shops.yml` wird korrekt gelesen und geschrieben
- Keine Duplikate möglich (addContainer prüft)
- save() nach jeder Mutation (add/remove)
- Leere Shops werden nicht in YAML geschrieben (aufräumen)
- Thread-safe ist NICHT nötig (alles Main-Thread)

### Integration in ShopScannerPlugin
- `ShopManager` als Feld in der Hauptklasse
- In `onEnable()`: `shopManager = new ShopManager(this); shopManager.load();`
- In `onDisable()`: `shopManager.save();`

---

## Task 4: ScannerItemManager

### Ziel
Hilfsmethoden zum Erstellen und Validieren des Scanner-Items.

### Datei erstellen

**`src/main/java/com/example/shopscanner/managers/ScannerItemManager.java`**

### Methoden (alle statisch)

- `static boolean isScanner(ItemStack item)`
  - Material == BONE
  - Hat ItemMeta mit DisplayName
  - DisplayName (als plain text) startet mit "Scanner " (mit Leerzeichen)
  - Text nach "Scanner " ist nicht leer
  
- `static String getShopName(ItemStack item)`
  - Voraussetzung: isScanner == true
  - Gibt den Teil nach "Scanner " zurück
  - Gibt null zurück wenn kein gültiger Scanner

- `static ItemStack createScanner(String shopName)`
  - Material: BONE, Amount: 1
  - DisplayName: `Component.text("Scanner " + shopName)` (Adventure, kein Legacy)
  - Lore: `Component.text("Shop: " + shopName, NamedTextColor.GRAY)`
  - Gibt fertigen ItemStack zurück

### Hinweis zur DisplayName-Prüfung
- Adventure Component → PlainTextComponentSerializer verwenden um den reinen Text zu extrahieren
- Vergleich case-sensitive auf "Scanner " Prefix
- `PlainTextComponentSerializer.plainText().serialize(meta.displayName())`

### Akzeptanzkriterien
- Ein im Amboss auf "Scanner Test" umbenannter Knochen wird erkannt
- Ein per `createScanner("Test")` erstellter Knochen wird erkannt
- `getShopName()` gibt exakt den Shop-Namen zurück ohne "Scanner " Prefix
- Normale Knochen ohne Namen werden ignoriert
- Knochen namens "Scanner" (ohne Leerzeichen/Suffix) werden ignoriert
- Knochen namens "Scanner " (mit Leerzeichen aber leerem Suffix) werden ignoriert

---

## Task 5: ContainerScanner (Scan-Logik)

### Ziel
Inventare von Containern auslesen und Items zählen.

### Datei erstellen

**`src/main/java/com/example/shopscanner/scanner/ContainerScanner.java`**

### Datenklasse für Ergebnisse

```java
public record ContainerResult(
    Location location,
    String containerTypeName,        // "Chest", "Barrel", "Shulker Box", etc.
    ContainerStatus status,
    Map<Material, Integer> items     // null wenn nicht gescannt
) {}

public enum ContainerStatus {
    OK,                    // Erfolgreich gescannt
    EMPTY,                 // Container ist leer
    CHUNK_NOT_LOADED,      // Chunk nicht geladen
    NOT_A_CONTAINER,       // Block ist kein Container mehr → Auto-Deregister
    OUT_OF_SCAN_RADIUS     // Außerhalb 3x3 um Lectern
}
```

### Methode

```java
public static List<ContainerResult> scanContainers(
    List<Location> containerLocations,
    Location lecternLocation
)
```

### Logik pro Container

1. `ZoneUtil.isInScanRadius(lecternLocation, containerLoc)` → nein: Status `OUT_OF_SCAN_RADIUS`
2. Chunk geladen prüfen: `loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)` → nein: Status `CHUNK_NOT_LOADED`
3. `Block block = loc.getBlock()` → `block.getState() instanceof Container` → nein: Status `NOT_A_CONTAINER`
4. `Container container = (Container) block.getState()`
5. `Inventory inv = container.getInventory()`
6. Alle Slots iterieren, AIR ignorieren, `Map<Material, Integer>` aufbauen
7. Map leer → Status `EMPTY`, sonst `OK`
8. Container-Typ-Name: `block.getType()` → `ItemNameUtil.formatMaterialName()` (z.B. "Chest", "Barrel")

### Items nach Menge sortieren
- Die Map wird NICHT hier sortiert — das macht der BookGenerator
- Hier nur die rohe Map zurückgeben

### Akzeptanzkriterien
- Double Chest: Wenn eine Hälfte gescannt wird, enthält die Map den Inhalt beider Hälften
- Shulker Box: Wird als ein Item gezählt, Inhalt NICHT rekursiv
- Leere Container bekommen Status EMPTY mit leerer Map
- Korrekte Status-Zuordnung für alle Sonderfälle

---

## Task 6: BookGenerator (Buch-Ausgabe)

### Ziel
Aus Scan-Ergebnissen ein Written Book für das Lectern generieren.

### Datei erstellen

**`src/main/java/com/example/shopscanner/scanner/BookGenerator.java`**

### Methode

```java
public static ItemStack generateBook(
    String shopName,
    List<ContainerResult> results
)
```

### Buch-Aufbau

**Seite 1 — Titelseite:**
```
══════════════
  Shop Scan
  "{shopName}"
══════════════
{dd.MM.yyyy HH:mm}

{n} Container
══════════════
```

**Seite 2..N — Pro Container:**
```
{ContainerTypeName}
@ {x}, {y}, {z}
──────────────
{ItemName} x {amount}
{ItemName} x {amount}
...
```

Items sortiert nach Menge absteigend.

**Sonderfälle:**
- Status EMPTY: `"(leer)"`  
- Status CHUNK_NOT_LOADED: `"(Chunk nicht geladen)"`
- Status OUT_OF_SCAN_RADIUS: `"(außerhalb Scan-Radius)"`
- Status NOT_A_CONTAINER: `"(kein Container mehr\n - wurde entfernt)"`

### Paginierung

- Buch-Seiten sind Strings (Written Book = Legacy-Text, kein Adventure für Buch-Seiten)
- Max ~256 Zeichen pro Seite, ~14 Zeilen
- Wenn ein Container mehr Items hat als auf eine Seite passen:
  - Aktuelle Seite abschließen
  - Neue Seite mit `"{ContainerTypeName} (Forts.)"` + verbleibende Items
- Max 100 Seiten insgesamt
- Wenn überschritten: Letzte Seite = `"... weitere Container\nnicht darstellbar"`

### Written Book erstellen

```java
ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
BookMeta meta = (BookMeta) book.getItemMeta();
meta.setTitle(shopName);
meta.setAuthor("ShopScanner");
meta.addPage(page1, page2, ...);  // Strings
book.setItemMeta(meta);
return book;
```

### Akzeptanzkriterien
- Titelseite zeigt korrekten Shop-Namen und aktuelles Datum/Uhrzeit
- Items sind nach Menge absteigend sortiert
- Sonderfälle zeigen den richtigen Status-Text
- Paginierung bricht korrekt um, keine abgeschnittenen Zeilen
- Buch hat nie mehr als 100 Seiten

---

## Task 7: ScannerInteractListener (Kern-Listener)

### Ziel
Alle Scanner-Interaktionen abfangen und verarbeiten.

### Datei erstellen

**`src/main/java/com/example/shopscanner/listeners/ScannerInteractListener.java`**

### Event: `PlayerInteractEvent`

### Haupt-Logik (Ablauf bei jedem PlayerInteractEvent)

```
1. Item in Main Hand holen
2. ScannerItemManager.isScanner(item) → nein: return (Event NICHT canceln)
3. Shop-Name extrahieren
4. Permission shopscanner.use prüfen → nein: Fehlermeldung + return
5. ZoneUtil.isInShoppingZone(player.getLocation()) → nein: Fehlermeldung + return
6. Action + Sneak-State bestimmen → richtige Sub-Methode aufrufen
7. Event canceln (IMMER wenn Scanner erkannt wurde, damit kein normales Verhalten)
```

### Sub-Methoden

**`handleRegister(Player player, Block block, String shopName)`**
- Auslöser: `Action.LEFT_CLICK_BLOCK` + `player.isSneaking()`
- Block in Shopping-Zone? → nein: Fehlermeldung
- `block.getState() instanceof Container` → nein: Fehlermeldung
- Max-Container-Limit prüfen (config `max-containers-per-shop`)
- Bereits registriert? → Fehlermeldung
- Double-Chest-Check: Andere Hälfte registriert? → Warnung (aber trotzdem registrieren)
- `shopManager.addContainer(shopName, block.getLocation())`
- Bestätigungs-Nachricht + Sound

**`handleDeregister(Player player, Block block, String shopName)`**
- Auslöser: `Action.RIGHT_CLICK_BLOCK` + `player.isSneaking()` + Block ist Container
- Registriert in diesem Shop? → nein: Fehlermeldung
- `shopManager.removeContainer(shopName, block.getLocation())`
- Bestätigungs-Nachricht + Sound

**`handleScan(Player player, Block block, String shopName)`**
- Auslöser: `Action.RIGHT_CLICK_BLOCK` + NICHT `player.isSneaking()` + Block ist Lectern
- Lectern in Shopping-Zone? → nein: Fehlermeldung
- Shop hat Container? → nein: Fehlermeldung
- Container-Liste holen
- `ContainerScanner.scanContainers(containers, lecternLocation)` aufrufen
- Auto-Deregister: Alle Results mit Status NOT_A_CONTAINER → `shopManager.removeContainer()`
- `BookGenerator.generateBook(shopName, results)` aufrufen
- Buch ins Lectern setzen:
  ```java
  Lectern lectern = (Lectern) block.getState();
  lectern.getInventory().setItem(0, book);
  lectern.update();
  ```
- Bestätigungs-Nachricht + Sound

### Disambiguierung Shift+Rechtsklick

```
Shift + Rechtsklick auf Block:
  → Block instanceof Lectern?  → KEIN Deregister, KEIN Scan (Shift = kein Scan)
      Eigentlich: Shift+RK auf Lectern soll NICHTS tun (weder Pair noch Scan)
  → Block ist Container?       → Deregister
  → Sonstiger Block?           → Ignorieren
```

Hinweis: In v2 gibt es kein Pairing mehr. Shift+Rechtsklick auf Lectern hat keine Funktion.

### Chat-Nachrichten

Alle Nachrichten mit Config-Prefix (`prefix` aus config.yml).
Adventure Components verwenden für Farben:
- Erfolg: `NamedTextColor.GREEN`
- Warnung: `NamedTextColor.YELLOW`
- Fehler: `NamedTextColor.RED`
- Info: `NamedTextColor.GRAY`

### Sounds

Aus Config laden, `Sound.valueOf(configString)` mit try-catch für ungültige Sound-Namen.

### Akzeptanzkriterien
- Scanner in der Hand + Shift+Linksklick auf Kiste → Kiste registriert
- Scanner in der Hand + Shift+Rechtsklick auf Kiste → Kiste deregistriert
- Scanner in der Hand + Rechtsklick auf Lectern → Scan, Buch erscheint
- Scanner in der Hand + beliebige Aktion außerhalb Zone → Fehlermeldung
- Ohne Scanner in der Hand → Normales Verhalten, Events nicht gecancelt
- Normaler Rechtsklick auf Lectern ohne Scanner → Buch öffnet sich normal (Vanilla)
- Shift+Linksklick ohne Scanner → Normales Abbauen des Blocks

---

## Task 8: ContainerBreakListener (Auto-Deregister)

### Ziel
Container automatisch aus allen Shops entfernen wenn sie zerstört werden.

### Datei erstellen

**`src/main/java/com/example/shopscanner/listeners/ContainerBreakListener.java`**

### Events

**`BlockBreakEvent`**
```
1. Block-Position holen
2. shopManager.findShopsContaining(location) → leer: return
3. Für jeden gefundenen Shop:
   - shopManager.removeContainer(shopName, location)
   - Chat-Nachricht an den Spieler: "Container @ X,Y,Z aus Shop 'Name' entfernt"
```

**`BlockExplodeEvent`**
```
1. event.blockList() iterieren
2. Für jeden Block: shopManager.findShopsContaining(location)
3. Gefundene entfernen + ins Log schreiben (kein Spieler zum Benachrichtigen)
```

**`EntityExplodeEvent`**
```
1. event.blockList() iterieren
2. Für jeden Block: shopManager.findShopsContaining(location)
3. Gefundene entfernen + ins Log schreiben
```

### Performance-Überlegung
- `findShopsContaining()` iteriert über alle Shops und alle Container
- Bei Explosionen mit vielen Blöcken könnte das langsam sein
- Optimierung: Nur prüfen wenn der Block ein Container-Typ ist (Chest/Barrel/Shulker)
  → `block.getState() instanceof Container` VOR dem Shop-Lookup

### Akzeptanzkriterien
- Kiste abbauen → automatisch aus allen Shops entfernt + Spieler-Nachricht
- Creeper-Explosion zerstört Kiste → automatisch entfernt + Server-Log
- TNT-Explosion zerstört Kiste → automatisch entfernt + Server-Log
- Nicht-Container-Blöcke lösen keinen Lookup aus (Performance)

### Integration in ShopScannerPlugin
- In `onEnable()` registrieren:
  ```java
  getServer().getPluginManager().registerEvents(new ContainerBreakListener(shopManager, this), this);
  ```

---

## Task 9: ShopScannerCommand

### Ziel
Die drei Admin-/Utility-Commands implementieren.

### Datei erstellen

**`src/main/java/com/example/shopscanner/commands/ShopScannerCommand.java`**

### Implementiert `CommandExecutor` und `TabCompleter`

### Subcommands

**`/shopscanner give <shopname> [player]`**
- Permission: `shopscanner.give`
- `<shopname>` kann Leerzeichen enthalten → alles nach "give" (minus optionaler letzter Spielername) ist der Shopname
- Wenn kein Spieler angegeben: an den Sender geben (muss Spieler sein)
- Wenn Spieler angegeben: an den genannten Spieler geben
- `ScannerItemManager.createScanner(shopName)` aufrufen
- Ins Inventar legen, bei vollem Inventar droppen

**Vereinfachung für Shopname mit Leerzeichen:**
- Letztes Argument prüfen ob es ein Online-Spieler ist
- Wenn ja: Das ist der Ziel-Spieler, Rest ist Shopname
- Wenn nein: Alles ist Shopname, Sender ist Ziel

**`/shopscanner reload`**
- Permission: `shopscanner.admin`
- `plugin.reloadConfig()` + `shopManager.load()`
- Bestätigungs-Nachricht

**`/shopscanner list`**
- Permission: `shopscanner.admin`
- `shopManager.listAllShops()` aufrufen
- Ausgabe im Chat:
  ```
  [Scanner] Registrierte Shops:
  - Diamanten (3 Container)
  - Erze (2 Container)
  - Redstone Stuff (1 Container)
  ```

### Tab-Completion

- Erstes Argument: `["give", "reload", "list"]`
- Nach "give": Online-Spielernamen vorschlagen
- Nach "reload" / "list": nichts

### Integration in ShopScannerPlugin

In `onEnable()`:
```java
getCommand("shopscanner").setExecutor(new ShopScannerCommand(this, shopManager));
getCommand("shopscanner").setTabCompleter(new ShopScannerCommand(this, shopManager));
```

In `paper-plugin.yml` den Command registrieren:
```yaml
commands:
  shopscanner:
    description: "ShopScanner Verwaltung"
    usage: "/shopscanner <give|reload|list>"
    permission: shopscanner.use
```

Hinweis: Commands in paper-plugin.yml müssen unter einem top-level `commands:` Key stehen.
Alternativ: Wenn paper-plugin.yml keine Commands unterstützt, eine zusätzliche `plugin.yml`
nur für die Command-Registrierung anlegen, oder Brigadier/Lifecycle-Events verwenden.

### Akzeptanzkriterien
- `/shopscanner give TestShop` gibt dem Spieler einen Knochen namens "Scanner TestShop"
- `/shopscanner give Redstone Stuff PlayerName` gibt PlayerName einen Scanner "Scanner Redstone Stuff"
- `/shopscanner reload` lädt Config + Shops neu
- `/shopscanner list` zeigt alle Shops mit Container-Counts
- Tab-Completion funktioniert für Subcommands und Spielernamen
- Fehlende Permissions → Fehlermeldung

---

## Task 10: Integration + Finaler Test

### Ziel
Alle Komponenten in der Hauptklasse verdrahten und End-to-End verifizieren.

### Datei aktualisieren

**`src/main/java/com/example/shopscanner/ShopScannerPlugin.java`**

### onEnable() — Vollständig

```java
@Override
public void onEnable() {
    // Config
    saveDefaultConfig();

    // Manager
    shopManager = new ShopManager(this);
    shopManager.load();

    // Listeners
    getServer().getPluginManager().registerEvents(
        new ScannerInteractListener(this, shopManager), this);
    getServer().getPluginManager().registerEvents(
        new ContainerBreakListener(this, shopManager), this);

    // Commands
    ShopScannerCommand cmd = new ShopScannerCommand(this, shopManager);
    getCommand("shopscanner").setExecutor(cmd);
    getCommand("shopscanner").setTabCompleter(cmd);

    getLogger().info("ShopScanner aktiviert!");
}
```

### onDisable() — Vollständig

```java
@Override
public void onDisable() {
    if (shopManager != null) {
        shopManager.save();
    }
    getLogger().info("ShopScanner deaktiviert!");
}
```

### Getter

```java
public ShopManager getShopManager() { return shopManager; }
```

### End-to-End Testszenarien (manuell auf Testserver)

```
1. Server starten → Plugin lädt, config.yml + shops.yml werden erstellt
2. /shopscanner give TestShop → Knochen "Scanner TestShop" im Inventar
3. Kiste platzieren → Shift+Linksklick mit Scanner → "Container registriert"
4. Zweite Kiste platzieren → Shift+Linksklick → "Container registriert"
5. Items in die Kisten legen
6. Lectern platzieren → Rechtsklick mit Scanner → Buch erscheint mit Inventarbericht
7. Buch öffnen (ohne Scanner) → Scan-Ergebnis lesbar
8. Nochmal scannen → Buch wird mit neuen Daten überschrieben
9. Shift+Rechtsklick auf erste Kiste → "Container deregistriert"
10. Erneut scannen → Nur noch eine Kiste im Buch
11. Kiste abbauen → "Container aus Shop 'TestShop' entfernt"
12. /shopscanner list → "TestShop (1 Container)"
13. /shopscanner reload → "Config neu geladen"
14. Außerhalb der Shopping-Zone Scanner benutzen → "Nur im Shopping District möglich"
15. Container 5 Chunks vom Lectern entfernt → Scan zeigt "(außerhalb Scan-Radius)"
```

### Akzeptanzkriterien
- Alle 15 Testszenarien bestehen
- Keine Exceptions im Server-Log
- Config-Werte werden korrekt gelesen (Prefix, Sounds, Zone, Max-Container)
- Server startet und stoppt sauber mit dem Plugin
