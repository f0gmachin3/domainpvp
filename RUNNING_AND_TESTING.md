# PvP Domain – Setup, Execution, and Multi-Client Testing Guide

This guide walks through configuring, launching, and testing the **PvP Domain** Fabric mod in a local multi-client development environment with a dedicated Minecraft server.

---

## 1. Prerequisites

- **Java Development Kit**: JDK 21 (Temurin, JBR, or Oracle OpenJDK 21)
- **Minecraft Java Edition**: 1.21.1
- **Fabric Loader**: 0.16.0+
- **Fabric API**: 0.102.0+

Verify your Java version:
```bash
java -version
```
Expected output: `openjdk version "21.x.x"`

---

## 2. Project Build

To build the mod `.jar` file:
```bash
# On Windows
.\gradlew.bat build

# On Linux / macOS
./gradlew build
```
The compiled mod JAR will be generated at:
`build/libs/domainpvp-1.0.0.jar`

---

## 3. Running the Dedicated Development Server

Fabric Loom provides integrated tasks for running authoritative dedicated servers directly from Gradle:

### Step 1: Initialize the Server
```bash
.\gradlew.bat runServer
```
On initial launch, the server will generate the `run/` directory and exit, prompting for EULA acceptance.

### Step 2: Accept EULA
Open `run/eula.txt` and set:
```properties
eula=true
```

### Step 3: Configure for Multi-Client Dev Testing
For local multi-client testing without requiring two separate paid Mojang accounts, open `run/server.properties` and configure:
```properties
online-mode=false
server-port=25565
gamemode=survival
difficulty=normal
pvp=true
view-distance=10
```

### Step 4: Start the Server
```bash
.\gradlew.bat runServer
```
The server will start up, initialize the PvP Domain persistence manager, and bind to `localhost:25565`.

---

## 4. Connecting Two Test Clients

### Client 1 (Player 1: `Gladiator1`)
Launch the client using Gradle with a custom player username:
```bash
.\gradlew.bat runClient --args="--username Gladiator1"
```
Once the game loads:
1. Click **Multiplayer** -> **Direct Connection**.
2. Enter `localhost:25565` and click **Join Server**.

### Client 2 (Player 2: `Gladiator2`)
Open a second terminal window and run:
```bash
.\gradlew.bat runClient --args="--username Gladiator2"
```
Connect to `localhost:25565`.

---

## 5. End-to-End Verification Walkthrough

Follow this step-by-step checklist to test all core systems:

### Phase 1: Party Formation
1. On **Gladiator1**'s screen, type:
   ```text
   /domain create
   ```
   *Expected output*: `[PvP Domain] Party created! You are the party leader.`
2. On **Gladiator1**'s screen, invite Gladiator2:
   ```text
   /domain invite Gladiator2
   ```
   *Expected output*: `[PvP Domain] Invitation sent to Gladiator2.`
3. On **Gladiator2**'s screen, accept the invitation:
   ```text
   /domain accept
   ```
   *Expected output*: Both players receive party update notifications.

### Phase 2: Loadout Selection & Ready Check
1. Select presets (Classic, Sword, Axe, Archer, UHC):
   - By default, `CLASSIC` is equipped (Protection 2 Diamond armor, Sharpness 3 Diamond Sword, Golden Apples, Bow, Water Bucket).
2. Both players ready up:
   ```text
   /domain ready
   ```
3. Once both are ready, the server transitions the state machine to `ARENA_BUILDING`.

### Phase 3: Progressive Colosseum Construction
1. Both players receive the announcement:
   `§6[PvP Domain] §5The Domain is manifesting...`
2. The server progressively builds the Roman Colosseum foundation, walls, pillars, spectator seating, and combat floor at safe isolated coordinates `(5000, 150, 5000)` at a chunk-safe rate (25 blocks/tick).
3. Clients render perimeter portal/enchant particles and audio cues.

### Phase 4: Synchronized Countdown & Isolated Combat
1. Upon arena completion, both players are teleported to opposing spawn positions:
   - `Spawn 1`: `(5012.5, 152.0, 5000.5)`
   - `Spawn 2`: `(4988.5, 152.0, 5000.5)`
2. Original pre-arena snapshots (inventory, armor, position, health, XP) are captured and saved to disk.
3. Movement is frozen while the server broadcasts:
   - `3.. 2.. 1..` (with note block pling audio)
   - `§c§lFIGHT!` (with dragon growl sound)
4. Combat rules are strictly enforced:
   - Only the two duelists can damage each other.
   - Block breaking and placing are prohibited.
   - Stepping outside the 20-block circular perimeter clamps the player back inside.

### Phase 5: Rematch & Teardown
1. When a player's health reaches 0, the winner is authoritatively announced:
   `§6§lVICTORY! Winner: <Player>`
2. **Rematch**: Both players send rematch votes:
   - If both agree, loadouts and spawns are reset without altering the pre-arena snapshot.
3. **Exit**: When either player votes to exit or times out:
   - Arena blocks are cleared.
   - Both players are teleported back to their original world coordinates.
   - Original inventories, XP, and health states are safely restored.

