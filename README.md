# FistPractice

A from-scratch Paper PvP Practice plugin: 1v1 duels, party duels/queues, ranked
ELO with divisions, config-driven game modes, kits + per-player layouts,
multi-arena rotation with non-blocking reset, spectating, single-elimination
tournaments, PlaceholderAPI, Discord webhooks, and a small developer API — all
built on **one shared Match Engine** (`match/MatchManager.java`) as required.

## Building

```
mvn clean package
```

Output jar: `target/FistPractice-1.0.0.jar`. Drop it into `plugins/` on a
Paper server.

> This sandbox has no internet access to Maven Central / PaperMC's repo, so
> the build could not be run and verified here. The code was written and
> manually reviewed for consistency (package/class structure, brace balance,
> cross-references between classes all checked), but **please run
> `mvn clean package` yourself and report back any compiler errors** — happy
> to fix them immediately.

## Version support (please read)

The spec asked for Paper support "modern Minecraft versions" plus the ability
to isolate version-specific code. In practice, one jar spanning **1.8 through
1.21** is not achievable once real code is written:

- Paper itself requires **Java 17** from 1.18 onward, and Java 17 bytecode
  cannot load on a Java 8 JVM (which 1.8-1.16 servers typically run on). This
  is a JVM limitation, not something any amount of plugin code can work around.
- This build targets **Paper 1.18+**, compiled to Java 17.

**The correct, standard way to let 1.8–1.21 *clients* play on this plugin**
is to run a modern Paper core (1.18+) with **ViaVersion + ViaBackwards**, which
is exactly what large practice networks (including the one that inspired this
spec) actually do. That's a server-setup step, not a plugin change.

The codebase itself contains no NMS/version-specific calls — everything is
plain Paper/Bukkit API — so it will keep working as Paper updates.

## What's fully implemented

- **Database**: SQLite (default) + MySQL/MariaDB, HikariCP pooled, fully async.
- **Game modes**: entirely config-driven (`/modes/*.yml`), nothing hardcoded —
  admins add new modes by dropping in a YAML file.
- **Kits**: kit manager + per-player kit layouts (multiple named layouts).
- **Arenas**: create/delete/setspawn/setregion/enable/disable, multi-arena
  rotation (random/sequential/priority), snapshot-based reset spread across
  ticks so it never blocks the main thread.
- **Match Engine**: single shared implementation for duels, party duels, and
  tournament matches. Countdown, best-of rounds, round/arena reset, player
  state save+restore, disconnect grace period, combat-log forfeiting, cleanup.
- **Ranked**: independent ELO per mode, provisional K-factor, configurable
  divisions with sub-ranks (e.g. "Diamond III").
- **Party system**: roles (leader/moderator/member), invites, chat, kick/
  promote/demote/transfer, size limits by permission, team split (random /
  ELO-balanced), party duels and party queueing — no external party plugin.
- **Queueing**: ranked + unranked, expanding ELO search window over time,
  supports both solo and whole-party entries.
- **Spectating**: join/leave, prevented from interacting/picking up items.
- **Tournaments**: single-elimination bracket generation, byes handled,
  auto-advances as matches complete.
- **History & leaderboards**: persisted match history (capped, auto-trimmed),
  leaderboard command sortable by ELO/wins/streak.
- **PlaceholderAPI** expansion, optional **Discord webhook** notifications.
- **Developer API** (`api/PracticeAPI.java`) plus 10 of the spec's ~30 Bukkit
  events as a complete, working pattern (see below).

## What's intentionally partial (spec §69 explicitly allows documenting this
rather than faking it)

- **GUI menus**: `Menu`/`MenuManager` framework is complete and two menus
  (Main, Party) are fully wired. The other ~12 menus listed in the spec
  (Duel/Ranked/Unranked menu, Kit Editor, Arena/Map selector, Profile,
  Statistics, Match History, Leaderboard, Spectator, Admin) are **not yet
  built as GUIs** — their commands all work from chat today. Adding a menu is
  ~30 lines following `PartyMenu.java` as a template (extend `Menu`, build()
  items from `menus.yml`, implement `onClick()`).
- **Custom Bukkit events**: 10 of the ~30 listed are implemented
  (`DuelRequestEvent`, `DuelAcceptEvent`, `PartyCreateEvent`,
  `PartyDisbandEvent`, `QueueJoinEvent`, `QueueLeaveEvent`, `MatchStartEvent`,
  `MatchEndEvent`, `ArenaResetEvent`, `RatingChangeEvent`). The rest
  (`DuelStartEvent`, `PartyKickEvent`, `PartyDuelStartEvent`, etc.) are a
  direct copy-paste of these patterns with different field names — happy to
  generate the full set on request.
- **Vault** economy hook is stubbed out in config (`hooks.vault: false`) but
  not implemented — nothing in the spec's feature list actually required
  currency, so it was left as a config toggle rather than invented.
- **Reset strategy**: uses an in-memory BlockData snapshot instead of a
  WorldEdit/FAWE schematic. This avoids a hard external dependency and is
  genuinely non-blocking, but a very large arena (many thousands of blocks)
  will take a few seconds to fully restore rather than being instant. Swap
  `ArenaManager.resetArena()` for a FAWE call if you add that dependency.
- **World border per mode / build height limits**: read from config
  (`GameMode.getWorldBorderRadius/getMinHeight/getMaxHeight`) but not yet
  enforced by a listener — wire a `PlayerMoveEvent` check using those values
  if you need it enforced live.

## Extending the GUI (worked example)

```java
public class DuelMenu extends Menu {
    public DuelMenu(FistPractice plugin) {
        super(plugin.getMenusConfigString("duel-menu.title", "<dark_gray>Duel Setup"),
              plugin.getMenusConfigInt("duel-menu.size", 27));
        setItem(11, Material.IRON_SWORD, "<white>Mode: NoDebuff", List.of("<gray>Click to cycle"));
        // ...
    }
    @Override
    public boolean onClick(Player player, int slot) {
        // mutate a per-player DuelSettings held in a Map<UUID, DuelSettings>, rebuild items
        return true;
    }
}
```
Then add `openDuelMenu(Player)` to `MenuManager` the same way `openPartyMenu`
works.

## Package layout

```
com.fistpractice
 ├─ api            developer-facing PracticeAPI facade
 ├─ arena          Arena, ArenaManager
 ├─ commands       one class per /command
 ├─ configuration  GameMode (config-driven), GameModeManager
 ├─ database       Database, SQLiteDatabase, MySQLDatabase, DatabaseManager
 ├─ duel           DuelSettings, DuelRequest, DuelManager
 ├─ events         custom Bukkit events
 ├─ gui            Menu, MenuManager, MainMenu, PartyMenu
 ├─ history        MatchHistoryEntry, HistoryManager
 ├─ hooks          PlaceholderHook, DiscordWebhook
 ├─ kit            Kit, KitLayout, KitManager
 ├─ listeners      connection/combat/match/gui listeners
 ├─ match          Match, MatchSide, MatchType, MatchState, MatchManager (the engine)
 ├─ party          Party, PartyRole, PartyInvite, PartyManager
 ├─ profile        PlayerProfile, ModeStats, ProfileManager
 ├─ queue          QueueEntry, QueueManager
 ├─ ranking        EloCalculator, Division, RatingManager
 ├─ spectator      SpectatorManager
 ├─ tournament     Tournament, TournamentManager
 └─ utilities      PlayerStateSnapshot, MessageUtil
```

## Config files

`config.yml`, `messages.yml` (MiniMessage), `menus.yml`, `database.yml`, plus
`modes/*.yml`, `kits/*.yml`, `arenas/*.yml` — all generated on first run.
