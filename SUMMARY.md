# What this mod does and doesn't fix

Overgrown Origins and Overgrown Apoli are rewrites of Origins and Apoli. They read data differently, renamed or reorganized many powers, and dropped or stubbed several old behaviors. As a result, custom origins written for **Origins 1.10.0 / Apoli 2.9.0 on Minecraft 1.20.1** often fail to load on them, load with fields silently ignored, or behave differently. Overgrown Legacy Bridge corrects this while packs load, without changing the pack files. It does three things:

- **Rewrites old JSON** into a form Overgrown accepts: actions, conditions, modifiers, keys, enum spellings, upgrades, layers and `loading_priority`.
- **Restores what Overgrown lacks or does differently:**
  - the original built-in power IDs;
  - toggle night vision;
  - `modify_grindstone` and `modify_xp_gain`, which Overgrown accepts but never applies;
  - the old priority order of entity-use actions;
  - `tick_rate`;
  - every `attribute_modify_transfer` target;
  - Apoli 2.9.0's modifier math for `origins:` powers;
  - `/resource` and `/power` commands;
  - lava movement, fluid rendering, damage-over-time and the old badges.
- **Moves existing players' saved origins, powers, resources, cooldowns and timers** onto Overgrown on their first join, so nobody has to pick again.

Each release build checks the built jar against the tested Overgrown versions on a development server and on a normal (production-mapped) Fabric server. The checks are 728 decode/re-encode tests, generated from the original source, plus 49 behavior tests. From 0.6.1, the build also starts a client to confirm that the client-side hooks apply.

What it doesn't fix:

- **Compiled Java addons** that call the old `io.github.apace100` classes. Data translation can't supply removed Java APIs, so these still need real ports.
- **Third-party power and action types** from other addon mods, unless that addon also provides them for Overgrown.
- **Packs for other Minecraft or Origins versions.** Only the 1.20.1 legacy format is supported.

Some parts are approximations:

- Legacy modifier math is used only when every modifier in a calculation comes from an old `origins:` power. When old and Overgrown-native modifiers mix, Overgrown's math applies.
- Food modifiers are grouped per power.
- A `fire_projectile` burst that was in progress when the player logged out isn't resumed.
- Some particle and sound details differ.

Testing has limits:

- The tests are headless. Visual results haven't been checked in-game: fluid rendering (including the Sodium hook), HUDs and real key presses.
- Mixing with AdditionalEntityAttributes hasn't been played.
- The tests cover examples, not every interaction. Read `config/overgrown_legacy_bridge/compatibility-report.json` and the native loader warnings after loading a new pack.

Install requirements: the mod must be on the server and on every client. Version 0.5.0 must not be used, because it does not start outside a development environment.
