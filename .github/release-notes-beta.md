**Beta pre-release** of Overgrown Legacy Bridge 0.5.0 for Minecraft 1.20.1, Overgrown Apoli 1.90.0+ and Overgrown Origins 1.41.0+. It loads legacy Origins 1.10.0 / Apoli 2.9.0 data packs on Overgrown Origins.

### Download

- `overgrown-legacy-bridge-0.5.0-beta.zip`: the mod jar, the optional Allaykin item-fix data pack, install steps, README and compatibility audit.
- `overgrown-legacy-bridge-0.5.0.jar`: the mod on its own.

Install the jar on the server **and** every client, after removing older bridge jars. See `INSTALL.txt` in the zip.

### Highlights since 0.4.0

- **Existing worlds migrate.** On a player's first join, their Origins 1.10.0 save data (chosen origins, command-granted powers, resource values, toggles and power inventories) moves onto Overgrown instead of prompting for a new origin. Cooldowns start ready.
- **Origin upgrades** named by an advancement fire when that advancement is completed, as in Origins 1.10.0, rather than retroactively.
- **`attribute_modify_transfer`** works for all 16 classes legacy Apoli applied it to, not just air speed.
- **`modify_grindstone`** (with `block_condition`) and **`modify_xp_gain`** work; Overgrown registered both without applying them.
- **Entity-use priority:** `action_on_entity_use`/`action_on_being_used` run in legacy priority order, so a priority-0 power no longer blocks villager trading or riding.
- `conditioned_attribute`/`conditioned_restrict_armor` `tick_rate`, `add_velocity` with both sides disabled, uppercase enum values such as `"CONSUME"`, and fluid rendering under Sodium 0.5.

Full details and remaining gaps: README.md and COMPATIBILITY.md.

### Verification

This build was compiled by GitHub Actions against the Overgrown Apoli and Origins releases on Modrinth. The workflow ran the dedicated-server compatibility suite, including new behavioral tests for each item above, before publishing; its results are attached to the workflow run. Client rendering (vanilla and Sodium), HUDs and physical key input still need in-game testing; please report problems with your Minecraft, Origins, Apoli and bridge versions plus `config/overgrown_legacy_bridge/compatibility-report.json`.
