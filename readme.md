# PvP Domain – Minecraft Java Edition Fabric Mod

![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x%20/%201.21.10-green.svg)
![Fabric](https://img.shields.io/badge/Fabric-Loader%20%26%20API-lightgrey.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)

**PvP Domain** is a server-authoritative multiplayer Minecraft combat mod. Two players form a party, configure validated PvP loadouts, ready up, and trigger the progressive block-by-block manifestation of a temporary combat arena (such as the Roman Colosseum).

Following a synchronized 5-to-1 countdown, players engage in isolated arena combat. When the duel finishes, players can rematch using the existing domain or exit, which completely restores their original world location, inventory, XP, and health, and safely clears the temporary arena.

---

## Architecture & Subsystems

The project strictly decouples logic across 10 modular systems:

1. **`PartyManager`**: Authoritative 2-player party lifecycle, invitations, leaves, and leader controls.
2. **`LoadoutManager`**: Competitive kit presets (Classic, Sword, Axe, Archer, UHC, Custom) and server validation.
3. **`MatchManager` & `MatchStateMachine`**: 11-state match progression with strict transition validation.
4. **`ArenaManager`**: Architectural templates, progressive block-by-block ticked construction, and cleanup.
5. **`CombatManager`**: Duelist damage isolation, perimeter boundary clamping, and block interaction blocking.
6. **`PlayerStateManager`**: Pre-arena deep state snapshots, anti-duplication safeguards, and atomic restoration.
7. **`NetworkingManager`**: Fabric Custom Payload networking with 9 C2S and 14 S2C packets.
8. **`PersistenceManager`**: Crash recovery and JSON state persistence under `domainpvp/snapshots/`.
9. **`ClientUIManager`**: Minimalist HUD overlays, countdowns, and match finish summaries.
10. **`DomainEffectManager`**: Client-side circular boundary particles, construction dust, and sound cues.

---

## State Flow

```text
IDLE
  ↓
PARTY_CREATED  (Player 1 creates party)
  ↓
WAITING_FOR_PLAYER  (Player 1 invites Player 2)
  ↓
LOADOUT_SELECTION  (Player 2 accepts; both configure kits)
  ↓
READY_CHECK  (Both players mark ready)
  ↓
ARENA_BUILDING  (Colosseum manifests block-by-block at 25 blocks/tick)
  ↓
COUNTDOWN  (Players teleport to opposing spawns; 5..4..3..2..1)
  ↓
FIGHTING  (Isolated 1v1 combat)
  ↓
MATCH_FINISHED  (Winner announced; statistics recorded)
  ↓
REMATCH (Both agree) ──→ COUNTDOWN
  ↓ (Either declines / quits)
CLEANUP  (Arena erased; pre-match states restored)
  ↓
IDLE
```

---

## Quick Start & Multi-Client Testing

Refer to [RUNNING_AND_TESTING.md](RUNNING_AND_TESTING.md) for full instructions on launching a dedicated server and connecting two clients.

```bash
# Build mod JAR
.\gradlew.bat build

# Launch server
.\gradlew.bat runServer

# Launch Client 1
.\gradlew.bat runClient --args="--username Gladiator1"

# Launch Client 2
.\gradlew.bat runClient --args="--username Gladiator2"
```

---

## In-Game Commands

- `/domain create`: Create a new 2-player party.
- `/domain invite <player>`: Send an invitation to another player.
- `/domain accept`: Accept a pending duel invitation.
- `/domain leave`: Leave your current party.
- `/domain ready`: Toggle your ready state to begin domain manifestation.

