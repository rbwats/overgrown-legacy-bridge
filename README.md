# Overgrown Legacy Bridge

Fabric 1.20.1 compatibility mod for Overgrown's Origins 1.41.0 and Apoli 1.90.0. Version 0.3.1 also translates legacy HUD sprite paths to the textures bundled with Overgrown's Origins and Apoli. It reads legacy origin JSON during Apoli's reload, so origin archives do not need editing. The optional `overrides/` datapack contains a targeted Allaykin fix. This project does not include Origins, Apoli, Allaykin, or other third-party mod files.

## Install

1. Remove older `overgrown-legacy-bridge` JARs from every `mods` folder.
2. Put `overgrown-legacy-bridge-0.3.1.jar` in the server's `mods` folder and each player's `mods` folder. The HUD sprite, status bar texture, and gravity adapters need the client copy.
3. If using Allaykin, package the contents of `overrides/` as a ZIP with `pack.mcmeta` at its root and put it in the server world's `datapacks` folder. Keep Allaykin.zip installed. The overlay must have higher priority than Allaykin; if the invalid `gshark:corecrafting_amethyst_core` tag warning persists, run `/datapack enable "file/<overlay-zip-name>.zip" last` and `/reload`.
4. Keep the matching Overgrown Origins, Apoli, Fabric API, origin mods, and data packs installed. Restart the server and clients.
5. Check the new server `latest.log` for `Failed to parse power`, `Failed to load function`, and `Ignoring the ... field`. Gameplay test the origins you use.

Do not install both bridge versions at once; they use the same mod ID.

## Included adapters

- Legacy `origins:set_resource` / `apoli:set_resource` actions, `sync:execute_command`, and `/power revoke` / `/power remove` syntax.
- Legacy HUD `sprite_location` paths under `origins:textures/gui/community/` map to Overgrown's `origins:textures/gui/sprites/hud_render/` assets. The old `origins:textures/gui/resource_bar.png` maps to Apoli's default bar. Custom texture paths are unchanged.
- Legacy `/resource operation <targets> <resource> = <score_targets> <objective>` syntax used by the included packs.
- Missing types and conditions for insomnia, status bar textures, mobs ignoring a holder, passive mobs, attacker distance, daylight, velocity changes, and temporary cobwebs.
- `origins:modify_gravity` modifies Apoli's calculated gravity on both client and server. Multiple active modifiers use Apoli's own modifier ordering.
- `origins:damage_taken` reads the incoming damage amount while a modify-damage action runs, restoring Moon's shield energy tiers.
- An `active_self` power with a direct `consumed_item` condition and no key, cooldown, or HUD is converted to `action_on_item_use` with the `finish` trigger. This restores Mythic's meat sickness and applies to the same pattern in future packs.
- JSON upgrades for older schemas: water ignoring, invulnerability, block use, block checks, grant/revoke source, damage type, movement speed, sprint jumps, launch, fire projectile, shield prevention, edible item, custom hurt/death sounds, and empty `and` conditions.
- Narrow repairs for the malformed `dragon_origins:mimic/chest` and `dragon_origins:mimic/chest_off` powers.
- Singleton `and.actions` values are wrapped as arrays, restoring Buzzborne's `global/eating_honey_effects` action.

Some adapters translate old behavior to the closest Overgrown behavior. In particular, legacy `launch` becomes upward velocity, sound replacement targets vanilla player sounds, and old damage source names map to vanilla damage types. The passive mob condition uses Minecraft's mob classification. These should be tested in play before relying on exact balancing.

## Overlay and remaining error

The overlay replaces only `data/gshark/powers/corecrafting.json`. It converts Allaykin's 1.20.5 item-component syntax to 1.20.1 SNBT in the crafted result, including its name, lore, model data, marker, and Mending. It removes the invalid extra `/give` command; the `item_on_item` power already grants its `result` to the crafter. This also avoids giving a core to every player on the server.

`dragon_origins:test` still fails to parse because it uses nonexistent entity actions. The supplied unchoosable test origin lists separate `dragon_origins:test/...` powers and does not reference this power. The bridge leaves this test file visible as an error rather than registering actions with incorrect behavior.

## Verification

`gradle build --offline --no-daemon` succeeds with JDK 17. A scan of the supplied mods and datapacks found 198 references to the two legacy HUD path layouts; all translated paths exist in the installed Origins or Apoli JAR. A local 53-mod server with Overgrown Origins, Apoli, four affected origin mods, and eight affected data packs (including Allaykin and the overlay) reached `Done`; it loaded 1,723 powers. The full server log with bridge 0.3.0 and the overlay reached `Done` with 2,948 powers, 115 origins, zero failed functions, and zero dropped fields. Both still show one failed power (`dragon_origins:test`). Version 0.3.1 still needs a client HUD check in game; movement, shield damage, meat eating, and core crafting also need gameplay tests.

## Build from source

Use JDK 17 and the Gradle wrapper. Place compatible Overgrown mod jars at `libs/apoli.jar` and `libs/origins.jar`; these local dependencies are ignored by Git. The tested versions were Apoli 1.90.0 (keybind fix v6) and Origins 1.41.0. Run `./gradlew build` (`gradlew.bat build` on Windows). The distributable JAR is produced under `build/libs/`.

To package the optional overlay on Windows, run `Compress-Archive -Path overrides/* -DestinationPath zz_overgrown-legacy-overrides.zip`. Verify that `pack.mcmeta` is at the ZIP root.
