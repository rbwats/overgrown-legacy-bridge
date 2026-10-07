**Beta pre-release** of Overgrown Legacy Bridge 0.4.0 for Minecraft 1.20.1, Overgrown Apoli 1.90.0+ and Overgrown Origins 1.41.0+. It loads legacy Origins 1.10.0 / Apoli 2.9.0 data packs on Overgrown Origins.

### Download

- `overgrown-legacy-bridge-0.4.0-beta.zip`: the mod jar, the optional Allaykin item-fix data pack, install steps, README and compatibility audit.
- `overgrown-legacy-bridge-0.4.0.jar`: the mod on its own.

Install the jar on the server **and** every client, after removing older bridge jars. See `INSTALL.txt` in the zip.

### Highlights since 0.3.1

- Legacy powers stay on Overgrown Origins' bound `key.origins.primary_active` (G) and `secondary_active` controls; `primary`/`secondary` aliases work.
- The original Origins built-in power IDs (`origins:water_breathing`, `origins:fall_immunity`, ...) load again, including their ID-keyed behaviors, so custom origins that reuse them work.
- `/power list`, `/power sources` and `/power clear` exist again (function files using them no longer fail to load); source-less `/power grant`/`revoke` use `apoli:command` as in legacy.
- `loading_priority`, sub-power `fabric:load_conditions`, `apoli:*_namespace(s)_loaded`, origin `upgrades`, legacy badges and named damage sources work as in legacy.
- Toggle night vision, lava speed, insomnia, fluid rendering, camera submersion, damage over time and side actions are restored; a malformed file now only affects itself.

Full details and remaining gaps: README.md and COMPATIBILITY.md.

### Verification

This build was compiled by GitHub Actions against the Overgrown Apoli and Origins releases on Modrinth. The workflow ran the dedicated-server compatibility suite before publishing; its results are attached to the workflow run. Client rendering, HUDs and physical key input still need in-game testing; please report problems with your Minecraft, Origins, Apoli and bridge versions plus `config/overgrown_legacy_bridge/compatibility-report.json`.
