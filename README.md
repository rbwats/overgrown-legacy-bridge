# Overgrown Legacy Bridge 0.6.1 beta

**Status: beta.** This branch contains the compatibility work through 0.6.1. [Beta source](https://github.com/rbwats/overgrown-legacy-bridge/tree/beta) · [Stable 0.3.1 source](https://github.com/rbwats/overgrown-legacy-bridge/tree/v0.3.1)

Build the beta jar from this branch using the instructions below. The mod's version and generated filename are `0.6.1`; the beta label describes this branch's testing status.

A general compatibility layer for legacy **Origins 1.10.0 / Apoli 2.9.0, Fabric Minecraft 1.20.1** JSON on Overgrown Origins. It normalizes power, action, condition, origin and layer data during reload; packs do not need to be unpacked or rewritten.

This release expands beyond the original pack-specific fixes. It adds a source-derived factory inventory, adapters for missing runtime behavior, and reusable compatibility checks. It is **not universal Java-addon compatibility**. See [the source audit and remaining gaps](COMPATIBILITY.md).

## Install

1. Remove previous bridge versions from the server and clients' `mods` folders.
2. Install `overgrown-legacy-bridge-0.6.1.jar` on both the server and every client.
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
- Supports singular and plural movement/resource/gravity modifiers, legacy resource truncation and the old `continous` spelling. `attribute_modify_transfer` accepts every class name legacy Apoli resolved and applies to all 16 classes legacy fed it into (air speed, velocity, jump, slipperiness, healing, falling, exhaustion, break speed, damage dealt/taken, projectile damage, status-effect amplifier/duration, experience, insomnia and attribute values).
- Restores `modify_grindstone` (Overgrown parses it but never applies it), including `block_condition`, and `modify_xp_gain`, which Overgrown also registers without an effect. `add_velocity` with both `client` and `server` false skips players as it did.
- Runs `action_on_entity_use`/`action_on_being_used` in legacy priority order around vanilla interaction: priority 0 no longer cancels vanilla (villager trading, riding and so on still happen), positive priorities can cancel it, negative ones run only if vanilla did nothing. `conditioned_attribute` and `conditioned_restrict_armor` honor `tick_rate`.
- Accepts legacy enum values in either case (`"action_result": "CONSUME"`, `"hands": ["MAIN_HAND"]`), as Calio did.
- Computes the modifiers of `origins:`-typed powers with Apoli 2.9.0's modifier engine: grouped operations, multipliers against the unmodified value, legacy resource fallback, `add_total_late` setting the value, and nested lists of several modifiers. `attribute_modify_transfer` modifiers join the same pass, as they did. Packs written for Overgrown (`apoli:` types) keep Overgrown's math.
- `origins:attribute` applies its modifiers regardless of its `condition`, as legacy did; lava speed conditions are re-checked every 10 ticks, as the original conditioned attribute was.
- Restores all nine `/resource operation` operators, fake scoreboard holders, floor division/remainder and resource clamping, plus `/power list`, `/power sources` and `/power clear`. A function file containing an unknown subcommand fails to load as a whole, so these matter even when unused at runtime.
- `/power grant` without a source uses `apoli:command` again, and `/power revoke` without a source removes only that command grant (or one made by Overgrown's own source-less grant), leaving powers from origins and actions alone, as legacy did. `/power remove` still removes every source.
- Honors `loading_priority` across packs for powers, origins and layers (a lower pack wins only with a strictly higher priority), `fabric:load_conditions` on individual sub-powers, and Apoli's `apoli:any_namespace_loaded`/`apoli:all_namespaces_loaded` resource conditions.
- Legacy origin `upgrades` (an advancement ID plus a translation-key announcement) fire when that advancement is completed, as in Origins 1.10.0, instead of being polled, so players who earned it earlier are not upgraded retroactively. The bridge also preserves layered random-exclusion lists and partial GUI-title overlays, gives layers without an `order` the order legacy derived from their load position, and restores the `origins:active`/`origins:toggle` badges (including the toggle badge on toggle night vision) and legacy badge sprite paths.
- A malformed legacy file only affects itself; normalization errors are logged per file instead of aborting the reload.
- Migrates existing players: on the first join with the bridge, a player's Origins 1.10.0 save data (chosen origins per layer, command- or action-granted powers, resource values, toggle states, power inventories such as the Shulk's, remaining cooldowns, damage-over-time and stacking-effect state) moves onto Overgrown instead of asking for a new origin. Until that join the legacy data is kept in the save.
- Fluid rendering also works with Sodium 0.5.

Earlier HUD, grant/revoke commands, gravity, damage-context, temporary cobweb, velocity and targeted malformed-pack fixes remain included. The optional `overrides/` Allaykin pack remains separate: package it with `pack.mcmeta` at its root, enable it after Allaykin, and run `/reload`.

## Limits and verification

The automated suite exercises 364 source-derived cases in both namespaces. The 0.6.0 beta release build (GitHub Actions, against Overgrown Apoli 1.90.0 and Origins 1.41.0 from Modrinth) passed **728/728 decode/encode/decode and idempotence checks** and **49/49 behavioral regression checks** on a dedicated development server, including grindstone, experience-orb, entity-use priority, tick-rate, transfer, legacy modifier-engine, save-migration and upgrade tests, and the same 728/728 and 49/49 on a production Fabric server with the published jars, where Minecraft and Overgrown use intermediary names. A mixin that only matches in development now fails the build; 0.5.0 had two such hooks and does not start outside development. From 0.6.1 the build also starts a development client and checks that every client-side mixin applies, and adds regression tests for the 0.6.1 fixes. The corpus also contains 294 `#full` variants that set every optional legacy field; these diagnostics list fields the fork decodes but ignores and do not fail verification. Many of their samples are placeholders rather than valid values for the field (for example `minecraft:stone` as a hand), so treat individual entries as leads rather than confirmed gaps. The committed `tests/*.json` files predate this run; the workflow uploads fresh ones with each release build. Counts describe tested examples, not every gameplay interaction. A separate integration server loaded 1,723 powers and 57 origins; its function failures and malformed test power are recorded in `tests/integration-results.json`.

Compiled addons importing old `io.github.apace100.*` APIs and arbitrary third-party factories still need ports or further adapters. Legacy modifier math applies when every modifier in a calculation comes from an `origins:` power; a calculation that also involves Overgrown-native powers uses Overgrown's math. Rendering and physical client key input still need in-game testing; the client check proves the hooks apply, not that the result looks right. [SUMMARY.md](SUMMARY.md) gives a short overview of what the bridge does and does not fix. The audit records remaining approximations and the native recipe warning seen during verification.

## Build and reproduce

Beta releases are built by `.github/workflows/beta-release.yml`: changing `.github/release-beta.txt` on `beta` fetches the tested Overgrown jars from Modrinth (build-time only, never bundled), builds, runs `verifyCompatibility`, and publishes a pre-release only when everything passes.


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
