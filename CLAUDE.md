# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project context

ResearchTable is a NeoForge Minecraft mod (MC 1.21.1, NeoForge 21.1.219, Java 21) that adds a research/quest table block. Researches are defined at runtime via CraftTweaker (`.zs`) scripts — there is **no built-in research content in Java**; the mod is a framework, and content lives in `run/scripts/main.zs` during development.

The repo is mid-migration from a 1.12.2 Forge codebase. The `src/main/java/snownee/researchtable/plugin/` tree contains many subpackages (`jei`, `hwyla`, `top`, `gamestages`, `itemstages`, `reskillable`, `togetherforever`, `grandeconomy`) that were integrations with old mods; only `crafttweaker` and `forge` are active under NeoForge. Treat the others as historical reference — do not assume they compile or are wired in.

Maven repos for JEI and CurseForge are configured but most plugin dependencies are commented out in `build.gradle` ("disabled during migration"). Currently active runtime/compile deps: Kiwi (via curse.maven) and CraftTweaker.

## Common commands

Gradle wrapper is committed; use it for everything.

```
./gradlew build              # compile + jar
./gradlew runClient          # launch dev client (run/ is the game dir)
./gradlew runServer          # launch dev server (--nogui)
./gradlew runData            # data generation
./gradlew jar                # build mod jar only
```

`./gradlew runClient` reads CraftTweaker scripts from `run/scripts/` — edit `main.zs` to define researches when testing. `run/*` is git-ignored except for `run/scripts/`.

Version comes from `gradle.properties` (`version_major.version_minor.version_patch`) and gets `-build${BUILD_NUMBER}` appended in CI.

## Architecture

**Entry point.** `ResearchTable.java` (`@Mod("researchtable")`) wires registration via `Registration.java` (deferred registers for block / item / block entity / menu), registers the `NetworkChannel`, mod config, the `CommandResearch` event handler on the NeoForge event bus, and — only on `Dist.CLIENT` — `client.ClientInit`. There is no longer a `ResearchTableModule` (deleted during the migration); registration is centralized in `Registration.java`.

**Research model (`core/`).** A `Research` is immutable and built once (via `ResearchBuilder` from a CrT script). It owns:
- `ICondition`s — visibility/availability gates (item / fluid / energy / score, plus the `plugin/` integrations)
- `ICriterion`s — gates evaluated against the player + the table's `CompoundTag data` (e.g. `CriterionScore`, `CriterionResearches`, `CriterionResearchCount`)
- `IReward`s — split into `triggers` (fire on start) and `rewards` (fire on complete); see `RewardItems`, `RewardExecute`
- `ResearchCategory` — UI grouping with an icon
`ResearchList.LIST` is the global registry, populated by CrT at script-load time. CrT exposes `mods.researchtable.ResearchTable.builder(name, category)` via `plugin/crafttweaker/CrTPlugin.java`.

**Persistence (`core/DataStorage.java`).** Single static instance bound to the overworld `ServerLevel`. Persists to `<world>/data/researchtable.dat` as compressed NBT. Two storage formats coexist on disk:
- `__v=0` (legacy 1.12.2): `playerName -> { researchName: count }`
- `__v=1` (current): both `oldRecords` (still keyed by name, used to migrate players who log in for the first time after upgrade) and `records` (keyed by team-owner `UUID`)
Records are keyed by **team owner UUID**, resolved through `core/team/TeamHelper.provider` (a `TeamProvider` SPI — singleplayer / no-team defaults to the player's own UUID). When a player logs in with a name still present in `oldRecords`, that progress is merged into their UUID record. Always go through `DataStorage.getRecords(uuid)` / `count(...)` / `setCount(...)` rather than touching the maps.

**Networking (`network/`).** Uses NeoForge 1.21 payload registrar with channel version `"1"`. Two packets:
- `PacketResearchChanged` (C→S): start/stop/complete/submit actions against the table at a `BlockPos`. The handler re-validates distance (`<= 10` blocks), permission, and `canComplete` before mutating state — **do not trust client input**; mirror that pattern when adding new actions.
- `PacketSyncClient` (S→C): pushes the team's research count map to each member. Sent on login and whenever counts change; the client mirror lives in `DataStorage.clientData`.

**Block / GUI.** `BlockTable` + `TileTable` (inventory + energy + fluid handlers exposed via NeoForge capabilities in `Registration.registerCapabilities`). `ContainerTable` is the menu; `client/gui/GuiTable.java` is the screen. Several legacy GUI components (`ComponentResearchList`, `ComponentResearchDetail`, etc.) were removed during the migration — `GuiTable` is being rebuilt directly. Conditions render through `client/renderer/ConditionRenderer.java`.

**Score integration.** `ResearchTable.scores` / `scoreFormattingText` are set by the CrT script via `ResearchTable.scoreIndicator(...)`. When `EventOpenTable` fires, the mod reads each configured scoreboard objective for the player and writes it into the table's `CompoundTag` as `score.<objective>`, making it available to criteria like `CriterionScore`.

**Mod config.** `ModConfig` uses `ModConfigSpec` registered as `Type.COMMON`. Static mirror fields are synced in `onConfigLoad` / `onConfigReload` — read those fields, not the `ConfigValue`s.

## Conventions

- Use tabs for indentation (`.editorconfig`).
- File encoding is UTF-8, enforced in `build.gradle` for both `JavaCompile` and `javadoc`.
- Resource IDs go through `Registration.id(path)` / `NetworkChannel.id(path)` to avoid hardcoding the modid.
- When adding a new `ICondition` / `ICriterion` / `IReward`, wire it into the CrT `ResearchBuilder` in `plugin/crafttweaker/` — the Java side has no other entry point for content.
- The `plugin/<mod>/` subpackages (other than `crafttweaker` and `forge`) are stale 1.12.2 code. Do **not** "fix" them as part of unrelated work; touching them is its own migration task.
