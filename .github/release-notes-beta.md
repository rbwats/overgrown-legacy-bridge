**Beta pre-release** of Overgrown Legacy Bridge 0.6.0 for Minecraft 1.20.1, Overgrown Apoli 1.90.0+ and Overgrown Origins 1.41.0+. It loads legacy Origins 1.10.0 / Apoli 2.9.0 data packs on Overgrown Origins.

### Download

- `overgrown-legacy-bridge-0.6.0-beta.zip`: the mod jar, the optional Allaykin item-fix data pack, install steps, README and compatibility audit.
- `overgrown-legacy-bridge-0.6.0.jar`: the mod on its own.

Install the jar on the server **and** every client, after removing older bridge jars. See `INSTALL.txt` in the zip.

### Highlights since 0.5.0

- **Production fix:** several of the bridge's hooks into Overgrown's loaders named methods the way they appear in development, not in the released jars. They now name both, and every release build runs the full test suite on a production-mapped Fabric server too.
- **Legacy modifier math:** modifiers in `origins:` powers are computed the way Apoli 2.9.0 did (grouped multipliers, `add_total_late`, resource fallback), including nested lists of several modifiers; `attribute_modify_transfer` joins the same pass. Overgrown-native packs keep Overgrown's math.
- **Save migration** now also carries over remaining cooldowns, damage-over-time timers, stacking-effect counts and active `action_over_time` state.
- `origins:attribute` ignores its condition again, lava speed re-checks its condition every 10 ticks, layers without an `order` sort as they did, `/power revoke` without a source only removes command grants, and toggle night vision shows the toggle badge.

Full details and remaining gaps: README.md and COMPATIBILITY.md.

### Verification

This build was compiled by GitHub Actions against the Overgrown Apoli and Origins releases on Modrinth. The workflow ran the compatibility suite on the development server and again on a production-mapped Fabric server before publishing; results are attached to the workflow run. Client rendering (vanilla and Sodium), HUDs and physical key input still need in-game testing; please report problems with your Minecraft, Origins, Apoli and bridge versions plus `config/overgrown_legacy_bridge/compatibility-report.json`.
