# Overgrown Legacy Bridge 0.4.0 beta

**Status: beta.** This branch contains the expanded compatibility work for 0.4.0. [Beta source](https://github.com/rbwats/overgrown-legacy-bridge/tree/beta) · [Stable 0.3.1 source](https://github.com/rbwats/overgrown-legacy-bridge/tree/v0.3.1)

Build the beta jar from this branch using the instructions below. The mod's version and generated filename are `0.4.0`; the beta label describes this branch's testing status.

A general compatibility layer for legacy **Origins 1.10.0 / Apoli 2.9.0, Fabric Minecraft 1.20.1** JSON on Overgrown Origins. It normalizes power, action, condition, origin and layer data during reload; packs do not need to be unpacked or rewritten.

This release expands beyond the original pack-specific fixes. It adds a source-derived factory inventory, adapters for missing runtime behavior, and reusable compatibility checks. It is **not universal Java-addon compatibility**. See [the source audit and remaining gaps](COMPATIBILITY.md).

## Install

1. Remove previous bridge versions from the server and clients' `mods` folders.
2. Install `overgrown-legacy-bridge-0.4.0.jar` on both the server and every client.
3. Keep Overgrown Origins, Overgrown Apoli, Fabric API, compatible addon providers and your data packs installed. Restart.
4. After startup or `/reload`, read `config/overgrown_legacy_bridge/compatibility-report.json`. Operators can locate it with `/legacybridge report`. Native `Failed to parse power` and dropped-field messages still matter; the report checks factory availability and known nested-modifier gaps, not every possible gameplay error.

Tested dependencies: Minecraft 1.20.1, Java 17, Fabric Loader 0.19.3, Fabric API 0.92.11, Overgrown Origins 1.41.0, and Overgrown Apoli 1.90.0 (keybind fix v6). Client hooks are required for HUDs, fluid rendering, key handling and movement. Later fork versions are not certified by this release.

## General compatibility

- Schema-aware traversal covers entity, bi-entity, item and block actions; entity, bi-entity, item, block, damage, fluid and biome conditions; multiple powers; and HUD conditions. Recipes, NBT and unrelated payload objects remain intact.
- Restores toggle night vision with persisted state, legacy controls, active-power conditions and server validation.
- Keeps legacy powers on Overgrown Origins' bound controls: `key.origins.primary_active` (G) and `key.origins.secondary_active` are left unchanged, and the old `primary`/`secondary` aliases map to them. Power names, descriptions and keys are normalized for addon power types too.
- Ships the original Origins 1.10.0 built-in powers under their original IDs (`origins:water_breathing`, `origins:fall_immunity`, ...), so custom origins, conditions and commands that reference them keep working next to Overgrown's reorganized `origins:<origin>/<power>` IDs. ID-keyed legacy behavior is restored: water breathing, creeper scaring, like-water swimming, cobweb immunity, conduit power on land and underwater visibility.
- Adds legacy lava movement, insomnia calculation, fluid rendering, exact fluid-ID conditions, side actions in all four contexts, and camera submersion without a `from` filter.
- Preserves legacy damage-source flags, message IDs (so `origins:name` damage conditions match) and death translation keys; restores damage-over-time onset, interval, easy damage and enchantment delay without per-tick network updates.
- Supplies original block-material and biome-category tags plus `origins:cobwebs` and `origins:shields`. Translates held-power-ID conditions, combined effects, missing enchantment defaults, particle height and spread, `check_ability`, and translated power/origin names.
- Supports singular and plural movement/resource/gravity modifiers, legacy resource truncation and the old `continous` spelling. `attribute_modify_transfer` works for `modify_air_speed` (the built-in `origins:like_air`).
- Restores all nine `/resource operation` operators, fake scoreboard holders, floor division/remainder and resource clamping, plus `/power list`, `/power sources` and `/power clear`. A function file containing an unknown subcommand fails to load as a whole, so these matter even when unused at runtime.
- `/power grant` without a source uses `apoli:command` again. `/power revoke` without a source removes only that command grant when the power also has another source; otherwise it keeps Overgrown's remove-everywhere behavior. `/power remove` still removes every source.
- Honors `loading_priority` across packs for powers, origins and layers (a lower pack wins only with a strictly higher priority), `fabric:load_conditions` on individual sub-powers, and Apoli's `apoli:any_namespace_loaded`/`apoli:all_namespaces_loaded` resource conditions.
- Converts legacy origin `upgrades` (advancement ID plus translation-key announcement) to Overgrown's condition format, preserves layered random-exclusion lists and partial GUI-title overlays, and restores the `origins:active`/`origins:toggle` badges and legacy badge sprite paths.
- A malformed legacy file only affects itself; normalization errors are logged per file instead of aborting the reload.

Earlier HUD, grant/revoke commands, gravity, damage-context, temporary cobweb, velocity and targeted malformed-pack fixes remain included. The optional `overrides/` Allaykin pack remains separate: package it with `pack.mcmeta` at its root, enable it after Allaykin, and run `/reload`.

## Limits and verification

The automated suite exercises 364 source-derived cases in both namespaces. The last recorded run (in `tests/`, made before the 0.4.0 review fixes) passed **726/728 decode/encode/decode and idempotence checks**. The two expected failures are `attribute_modify_transfer` with a class Overgrown has no hook for. It also passed **28 behavioral regression checks** on a real dedicated server. The corpus now also contains 294 `#full` variants that set every optional legacy field; these are diagnostics that list fields the fork decodes but ignores, and they do not fail verification. **The suite has not yet been re-run for the review fixes and the 8 behavioral checks added with them; run `verifyCompatibility` with the tested jars to refresh `tests/`.** Counts describe tested examples, not every gameplay interaction. A separate integration server loaded 1,723 powers and 57 origins; its function failures and malformed test power are recorded in `tests/integration-results.json`.

Compiled addons importing old `io.github.apace100.*` APIs, multi-element nested modifier lists, exact legacy modifier-engine quirks, and arbitrary third-party factories still need ports or further adapters. Known fields Overgrown ignores (`modify_grindstone.block_condition`, `modify_player_spawn.respawn_sound`, `add_velocity` `client`/`server: false`) are listed in the compatibility report. Rendering and physical client key input still need in-game testing; headless tests do not establish visual correctness. The audit records remaining approximations and the native recipe warning seen during verification.

## Build and reproduce

Clone the beta branch, then place the tested Overgrown jars at `libs/apoli.jar` and `libs/origins.jar`. These dependencies are local inputs and are not bundled in the bridge jar. Use JDK 17.

```text
git clone --branch beta https://github.com/rbwats/overgrown-legacy-bridge.git
cd overgrown-legacy-bridge
```

On Windows, run:

```text
gradlew.bat build --no-daemon
gradlew.bat verifyCompatibility --no-daemon
```

The installable output is `build/libs/overgrown-legacy-bridge-0.4.0.jar`. On Linux or macOS, use `./gradlew` in place of `gradlew.bat`. Add `--offline` when the required Gradle and Minecraft dependencies are already cached; the recorded verification used that mode.

The verification task prepares an isolated loopback-only test server under `build/compatibility-run`, writes the test EULA acceptance, runs the corpus and behavioral tests, and stops it. It never opens your real server world. It fails on unexpected codec gaps, missing result files or failed behavioral tests. JSON results are in `tests/`.

To regenerate the inventory, clone the original Apoli `v2.9.0` source to the sibling `.reference-apoli-2.9` directory and run `python tools/audit_upstream.py`. To inspect another mod collection's Java API references, run `python tools/audit_addons.py PATH_TO_MODS --output report.json`; this read-only scanner also visits declared nested jars.

Optional integration fixtures can be supplied with `-PlegacyIntegrationModsDir=PATH` and `-PlegacyIntegrationPacksDir=PATH`. Only use the origin-related fixture jars there. The general test suite itself does not require any of those packs.

## Beta feedback

When reporting a compatibility problem, include the exact Minecraft, Origins, Apoli and bridge versions; the affected pack or addon; the power ID; relevant native loader errors; and `config/overgrown_legacy_bridge/compatibility-report.json`. For client issues, include the renderer and whether the problem affects HUDs, fluid rendering or key input. Report gameplay differences as well as loading failures: successful decoding does not prove equivalent behavior.

Upstream MIT notices for the reused Apoli tags, damage-type data and adapted damage behavior, and for the reused Origins 1.10.0 built-in powers, power names and tags, are included in the jar (`LICENSE-upstream-apoli`, `LICENSE-upstream-origins`, `NOTICE`).
