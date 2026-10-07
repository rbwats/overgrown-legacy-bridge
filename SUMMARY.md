# What Overgrown Legacy Bridge fixes, and what it doesn't

Overgrown Origins and Overgrown Apoli are rewrites. Custom origins made for the original **Origins 1.10.0 / Apoli 2.9.0 on Minecraft 1.20.1** often fail to load on them, lose fields silently, or behave differently. The bridge fixes this while the data packs load, without editing them. It rewrites old JSON (actions, conditions, modifiers, keys, enum spellings, upgrades, layers, `loading_priority`) into a form Overgrown accepts. It brings back what Overgrown removed or changed: the original built-in power IDs, toggle night vision, `modify_grindstone` and `modify_xp_gain` (which Overgrown accepts but never applies), entity-use priority, `tick_rate`, all 16 `attribute_modify_transfer` targets, Apoli 2.9.0's modifier math for `origins:` powers, the old `/resource` and `/power` commands, lava movement, fluid rendering, damage-over-time and the old badges. It also moves each existing player's saved origins, powers, resources, cooldowns and timers onto Overgrown at their first join, so nobody has to pick an origin again.

Every release build is tested against the tested Overgrown versions on a development server and a normal production-mapped Fabric server. The tests are 728 decode/re-encode checks generated from the original source code and 52 behavior tests. From 0.6.1 the build also starts a client to check that the client-side hooks apply.

What it can't fix:

- **Compiled Java addons** that call the old `io.github.apace100` classes. Translating data can't supply removed Java APIs, so these need real ports.
- **Power, action or condition types from third-party addon mods**, unless that mod also provides them for Overgrown.
- **Other formats:** packs for other Minecraft or Origins versions.

What is only approximate:

- Legacy modifier math is used only when every modifier in a calculation comes from an old `origins:` power. Once Overgrown-native modifiers are mixed in, Overgrown's math decides.
- Food modifiers are grouped per power.
- A `fire_projectile` burst in progress at logout isn't resumed.
- Some particle and sound details differ.

COMPATIBILITY.md lists every known gap. What still needs in-game testing:

- Fluid rendering (with and without Sodium), HUDs and real key presses: the automated tests are headless.
- Use alongside AdditionalEntityAttributes.
- Packs in general: the tests use examples, not every pack. After adding a pack, check `config/overgrown_legacy_bridge/compatibility-report.json` and Overgrown's own load warnings.

The mod must be installed on the server and on every client. Don't use version 0.5.0, which doesn't start outside a development environment.
