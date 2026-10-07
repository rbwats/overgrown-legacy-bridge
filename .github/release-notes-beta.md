**Beta pre-release** of Overgrown Legacy Bridge 0.6.1 for Minecraft 1.20.1, Overgrown Apoli 1.90.0+ and Overgrown Origins 1.41.0+. It loads legacy Origins 1.10.0 / Apoli 2.9.0 data packs on Overgrown Origins.

### Download

- `overgrown-legacy-bridge-0.6.1-beta.zip`: the mod jar, the optional Allaykin item-fix data pack, install steps, README, compatibility audit and a short summary of what the bridge does and does not fix.
- `overgrown-legacy-bridge-0.6.1.jar`: the mod on its own.

Install the jar on the server **and** every client, after removing older bridge jars. See `INSTALL.txt` in the zip. Do not use 0.5.0, which does not start outside a development environment.

### Fixes since 0.6.0

- **Memory leak:** every grindstone screen opened was kept in memory together with its player (and, on the client, the world) until the game closed.
- **Velocity transfers:** an `attribute_modify_transfer` with class `modify_velocity` now applies to every axis, as legacy did, not only to the axes a `modify_velocity` power names.
- **Nested modifiers:** a legacy modifier with a `name` and a single nested `modifier` no longer loses the nested one.
- **Transfers applied once per calculation:** a handler's transfers no longer also apply to calculations run by its actions (for example a `modify_resource` self action of `modify_projectile_damage`).
- **Error safety:** a power action that throws no longer leaves transfer or damage state behind that would affect later calculations.
- `modify_camera_submersion` without `from` accepts upper-case values (`"to": "WATER"`), as legacy did.
- **Client check:** each release build now starts a client and verifies that every client-side hook applies; CI previously only exercised servers.

Full details and remaining gaps: SUMMARY.md, README.md and COMPATIBILITY.md.

### Verification

This build was compiled by GitHub Actions against the Overgrown Apoli and Origins releases on Modrinth. The workflow ran the compatibility suite on a development server and again on a production-mapped Fabric server, and started a development client to apply the client hooks, before publishing; results are attached to the workflow run. Visual results (fluid rendering with and without Sodium, HUDs) and physical key input still need in-game testing; please report problems with your Minecraft, Origins, Apoli and bridge versions plus `config/overgrown_legacy_bridge/compatibility-report.json`.
