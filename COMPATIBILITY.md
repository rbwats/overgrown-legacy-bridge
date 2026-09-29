# Source audit: legacy Origins on Overgrown Origins

## Baseline and evidence

The audit targets the original Minecraft **1.20.1** line, not newer Origins formats:

| Project | Examined version | Source |
| --- | --- | --- |
| Original Origins | v1.10.0, e197c91116ecd31b54c4f32721ca1cab33afb347 | [Tagged source](https://github.com/apace100/origins-fabric/tree/v1.10.0) |
| Original Apoli | v2.9.0, c5e170185a69b182277dda0b522b638d32858872 | [Tagged source](https://github.com/apace100/apoli/tree/v2.9.0) |
| Overgrown Origins | Installed 1.41.0 binary, plus Fabric-1.20.1 source at 9cb4fbd7406897c0463ac98dcb6680bf0d6565c3 | [Fork source](https://github.com/0vergrown/Origins/tree/9cb4fbd7406897c0463ac98dcb6680bf0d6565c3) |
| Overgrown Apoli | Installed 1.90.0 keybind fix v6 binary, plus source at 51a33e71ac52f66413b5c06480eb631d989dd92e | [Fork source](https://github.com/0vergrown/Apoli/tree/51a33e71ac52f66413b5c06480eb631d989dd92e) |
| AdditionalEntityAttributes | 1.20 source, used to trace original lava movement | [Original movement hooks](https://github.com/DaFuqs/AdditionalEntityAttributes/tree/1.20) |

The fork source checkouts do not exactly match the installed jars. The actual installed codec APIs, dedicated-server launch, mixin application and regression results are therefore the authority for this build. The previous `.reference-old-apoli` checkout targets 1.20.2 and was excluded from the 1.20.1 inventory.

`tools/audit_upstream.py` extracts balanced factory declarations and legacy field contexts, then generates `legacy-1.20.1-schemas.json` and `tests/upstream-codec-corpus.json`. Generic factory examples are supplied explicitly. This is a source-derived lower bound, not a formal proof that every registration, optional field and addon exists.

## What changed, and why

| Area | Source-level incompatibility | Bridge 0.4.0 behavior |
| --- | --- | --- |
| Data engine | Original SerializableData factories and class registries became codecs and typed context registries | Traverses known action/condition fields with the correct context; uses native aliases where they already work |
| Entity vs item damage | Identical factory names have different data in different contexts | Restricts entity damage conversion to entity and bi-entity actions |
| Held power condition | Original `power_type` field refers to a power ID; the fork has factory-type semantics | Converts to native `power` with the held power ID |
| Side actions | Legacy generic side wrapper is missing | Implements client/server dispatch in entity, bi-entity, block and item contexts |
| Fluid condition | Exact fluid factory is missing | Compares actual registry fluid identity, retaining still/flowing distinction |
| Material and biome category | Legacy tag-backed factories were removed | Translates to native tag conditions and bundles the original 47 material and 18 category tag files |
| Status effects | Old entity actions accept both `effect` and `effects` | Combines both without losing entries, including the empty legacy no-op |
| Defaults | Enchantment comparison, particle height and optional landing action differ | Restores legacy defaults and an explicit no-op landing action |
| Insomnia | Missing power; previous bridge mutated the real rest statistic | Applies active modifiers only to the phantom spawner's effective count after vanilla clamping; stored statistics remain unchanged |
| Lava speed | Original used an AdditionalEntityAttributes friction attribute | Restores its three 0.5 movement hooks and range 0..1 without a mandatory third-party dependency |
| Toggle night vision | Fork aliases it to always-on night vision | Uses a dedicated configuration and synchronized auxiliary toggle state, key collection/dispatch and active-condition checks |
| Fluid rendering | Original changed chunk-renderer fluid states | Restores the client region hook and rebuilds rendering when active configurations change |
| Camera submersion | Original `from` is optional; native requires it | Implements an optional filter; omitted `from` matches all fluid fog states |
| Damage sources | Original flag-based source descriptions do not equal a name-to-type lookup | Matches the same eight damage-type tag predicates, preserves registry tie order, attacker attribution and death-name translation prefix |
| Damage over time | Native conversion drops enchantment protection and changes condition reset behavior | Dedicated legacy timer restores onset, interval, easy damage, level-plus-item-count protection delay, inactive reset and respawn reset |
| Resource modification | Native singular modifier rounds; old action supports lists and truncates | Dedicated action combines modifiers and casts to integer before native bounds enforcement |
| Movement and gravity | Singular-only conversion loses plural modifiers | Supports both fields; movement modifiers acquire the movement-speed attribute |
| Keys | Built-in translation names changed | Maps old primary/secondary names, preserves third-party names and fixes `continous` |
| Text | Original power/origin strings were translation keys; native component strings can be literals | Converts string names/descriptions to translation components; leaves existing component objects intact |
| Commands | Old resource operation tree is absent; initializers can register callbacks in either order | Attaches after Fabric registration; supports all nine vanilla scoreboard operators including fake holders and swap |
| Layer overlays | Native non-origin fields are overwritten wholesale | Accumulates old exclusions unless explicitly replaced, merges partial GUI-title fields, and normalizes conditional entries |
| Assets | Original HUD sprite paths moved | Retains the previous mapping to bundled fork HUD textures and leaves custom locations unchanged |
| Diagnostics | A successful parse can conceal a missing or ignored behavior | Writes factory/context/path candidates and nested-list warnings on reload; adds `/legacybridge report` |

The general traversal does not rewrite arbitrary recipe JSON, NBT, text payloads or third-party data objects merely because they contain a `type` key. Previously targeted malformed Dragon Origins repairs and the optional Allaykin overlay remain explicit, separate exceptions.

## Coverage and how to reproduce

| Context | Source-derived cases |
| --- | ---: |
| Powers | 105 |
| Entity actions | 54 |
| Bi-entity actions | 16 |
| Block actions | 16 |
| Item actions | 14 |
| Entity conditions | 68 |
| Bi-entity conditions | 18 |
| Block conditions | 23 |
| Item conditions | 21 |
| Damage conditions | 14 |
| Fluid conditions | 7 |
| Biome conditions | 8 |
| Total | 364 |

Each example is exercised as `origins:` and `apoli:`. The dedicated server runs actual fork codecs, repeats normalization, encodes and decodes successful results, and performs 28 behavioral checks. **726/728 codec cases and 27/28 behavioral checks passed.** Both codec failures are `attribute_modify_transfer`, intentionally recorded as an expected gap rather than masked with a no-op.

Behavior checks cover all scoreboard operations, floor arithmetic, zero division, clamping, fake names, resource truncation/list combination, modifier activation/suppression, night-vision state and persistence, key dispatch, active-power semantics, material membership, fluid identity, held-power IDs, damage names/flags, timer damage intervals and enchantment delay, safe payload handling, normalization idempotence, camera fog matching, translation keys and overlay merging. They do not establish physical keyboard/network interaction or rendered client visuals.

Run `gradlew.bat verifyCompatibility --offline --no-daemon` with the tested local jars. The Gradle task fails if the gap set changes or a regression fails; a successful Gradle process alone is not accepted as a test result. The server binds to loopback and stops after the tests. `tests/codec-results.json` and `tests/regression-results.json` contain the current evidence.

A separate integration run with four origin fixture mods and eight pack/overlay archives reached `Done`, loaded **1,723 powers and 57 origins**, and retained **726/728 codec checks and 28/28 behavioral checks**. One malformed unused power (`dragon_origins:test`) failed. The minimal integration environment excluded Pehkui; 25 functions referring to its `/scale` commands failed to load. This is not a full-modpack clean-load claim. `tests/integration-results.json` records these results and diagnostic candidates.

## Remaining incompatibilities

### Java binary/API compatibility

The old `io.github.apace100.origins`, `io.github.apace100.apoli` and Calio classes are absent from the rebuilt fork. Core APIs changed from `PowerFactory`, object-per-entity `Power`, `PowerHolderComponent`/Cardinal Components and SerializableData to `PowerType<Config>`, codecs, attachments and auxiliary state. Event hooks, mixin targets, client GUI classes and network payloads also changed.

Namespace aliases repair JSON identifiers. They cannot supply removed Java classes, old inheritance contracts, Cardinal Components state, or old client/server wire protocols. Compiled addons that exercise those APIs need actual ports. Loader dependency constraints are evaluated before this bridge initializes; it does not insert broad dependency overrides or pretend to provide another mod's APIs.

`tools/audit_addons.py` reads constant-pool class references and descriptors, checks declared nested jars, and records Fabric dependencies. The supplied mod folder had **5 candidate archives among 550 including nested archives**. Those include patched mods with leftover unused classes and optional compat hooks; this is not proof those files execute or must all be removed. See `tests/java-addon-report.json` for exact classes. The scanner is reusable for any future mod collection.

### attribute_modify_transfer

Original `AttributeModifyTransferPower` selects an old Java power class through Calio's class registry. `PowerHolderComponent.modify` transfers actual attribute modifiers into that selected modifier class, even without a separately granted modifier power. The fork's handlers do not share that engine or class hierarchy. A fake factory that loads while applying nothing would misrepresent compatibility. A faithful implementation needs per-handler integration and a defined mapping for addon classes; this factory currently remains an explicit load error.

### Modifier engine and nested lists

The original value-modifier engine groups operations and uses a base snapshot; the fork evaluates its modifiers sequentially. Multiple additive-base modifiers, combinations of base/total operations, absent resource fallbacks and the original `add_total_late` quirk can differ. Existing native math is used by this bridge; exact old balancing across every modifier combination is not certified.

The original nested `modifier` is a list; the fork's record stores one child. Singleton lists are unwrapped. Multi-element nested lists are reported for porting and remain unsupported; the bridge does not silently pick their first element. Native optional-field handling can still drop malformed fields, so read native loader warnings too.

### Third-party data and version boundaries

Arbitrary addon factories, Pehkui dependencies, bespoke resource assets, old Java GUIs, Origins Classes extensions and unrelated Minecraft-version changes need their own providers or ports. This bridge targets 1.20.1 legacy core JSON. It does not generally down-convert 1.20.5 item components or recipes. The isolated native fork run still reports an invalid recipe in `origins:arachnid/master_of_webs_web_crafting`; the bridge leaves the recipe payload intact.

### Behavior and clients

Lava condition changes are evaluated during movement rather than on the original attribute power's 10-tick schedule. Combination with an independently installed AdditionalEntityAttributes mod has not been gameplay tested. Fluid rendering, HUDs and physical key handling need client testing, including alternative renderers such as Sodium. The old launch conversion preserves upward velocity and optional sound but does not reproduce every particle/sound detail. Existing passive-mob classification and addon sound replacements retain their earlier approximations.

Original worlds' saved Origins/Cardinal Components data is not a promised state migration. New bridge state is saved and synchronized using the fork's native container. Install the bridge on both sides and keep matching fork versions.

## Attribution

Upstream material/category tags, the original damage-over-time type and adapted damage behavior are MIT-licensed. The built jar includes `LICENSE-upstream-apoli` and `NOTICE`; it contains no third-party mod jars.
