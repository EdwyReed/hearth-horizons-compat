# Hearth & Horizons Compatibility

NeoForge 1.21.1 companion mod. This repository is separate from the Modrinth profile.

Source: [EdwyReed/hearth-horizons-compat](https://github.com/EdwyReed/hearth-horizons-compat).
See [CONTRIBUTING.md](CONTRIBUTING.md) for a fresh checkout, dependency setup and
isolated validation. Development release 0.4.0 is awaiting full-profile gameplay acceptance.

## Current development release — 0.4.0

Nine Pam soda flavours now have distinct coloured machine fluids. Mix 250 mB in
Create, bottle it with a spout or right-click a fluid container with a glass bottle.
Manual soda crafting is disabled. Drinking returns one bottle and produces brief
cosmetic bubbles and a pop, without potion buffs. Open basins emit sparse bubbles;
the nearby-basin cache refreshes every 40 ticks and never loads chunks.

751 additional dishes have separate registered, inedible preparations and a cooking
stage: 286 oven, 306 cooking pot, 142 skillet, eight drying rack and nine fermentation.
Original final item IDs, batch yields and ingredient choices are retained. Ready
salads, sandwiches and assembled meals remain cold recipes; 42 reviewed recipes
require cooked meat, rice, potatoes or eggs as appropriate. Sushi and tartare retain
raw fish/meat; sushi rice is cooked. Explicit raw preparations remain raw.
Container-aware crafting returns emptied vessels once and retains vessels
carried into the finished serving. The audit includes every installed top-level
JAR's recipe resources and an isolated runtime set of 58 culinary/dependency JARs.
It does not claim a universal simulation of cooking or acceptance of unrelated mods.

Frying uses a dedicated recipe type accepted by placed and handheld Farmer's Delight
skillets, with its own JEI category. Campfires and ordinary furnaces reject those
preparations. JEI lists all registered preparations and links preparation/cooking
stages through their real item identities.

Cluttered served pastries carry their actual ingredients, nutrition, saturation and
food effects. Bread trays and baking kits preserve each ingredient's components;
right-click with an empty hand to take one ingredient. Their models show the
remaining portions; the last portion returns the original tray or bowl. Breaking
returns the actual partially used object. No internal GUI or ticker is added.

All culinary data/assets are embedded. Keep the independent localization/fonts
resourcepack; the former standalone compatibility datapack is no longer installed.
See [DEPENDENCIES.md](DEPENDENCIES.md) for mandatory providers and optional JEI.

Release order: complete tests, upload the restricted companion project/version,
install/recognize that exact JAR in Modrinth App, then export through the App's
native `.mrpack` workflow. Verify the remote hash/download entry before preparing
the modpack version. Nothing is published publicly before Ivan's validation.

## Original three preparations — introduced in 0.3.0

The three preparations have their own registered item IDs:

| Preparation | Item ID | Final dish |
| --- | --- | --- |
| Dough for four cookies | `hearth_horizons_compat:raisin_oatmeal_cookie_preparation` | 4 Croptopia raisin oatmeal cookies |
| Chicken before frying | `hearth_horizons_compat:cashew_chicken_preparation` | 1 Croptopia cashew chicken |
| Beef and vegetables before frying | `hearth_horizons_compat:beef_stir_fry_preparation` | 1 Croptopia beef stir fry |

These items have no FOOD component and no cooked-food tags. Their English/Russian
names, stage descriptions and approved raw icons are bundled in the JAR. They are
available in the Ingredients creative tab and explicitly registered in JEI's ingredient list.
Preparation recipes, final cooking recipes and their original IDs remain intact;
mixing produces a raw item, while the stove/skillet recipe consumes that distinct item.
The original input quantities, cooked yields and container outputs are retained.

JEI 19.27 integration adds the three registered preparations to the ingredient list.
Native recipe categories and transfer handlers distinguish raw/cooked items by their
real registered IDs. On 2026-10-16 Ivan confirmed that no old component-marked
preparations had ever been created and discarded legacy compatibility. The three
unused migration recipes and three old Croptopia model overrides are removed.
There is no recipe for converting a cooked dish into a raw preparation and no
legacy subtype interpreter or ingredient handler. The 0.3.0 embedded pack contained
684 recipes, 67 tags and 3 advancements, plus raw icons/models and localization.

`migration/release-0.3.0.json` records the complete embedded pack. Run
`python tools/verify_release.py --require-runtime` to verify artifact hashes and the
server/client integration evidence. `python tools/resolve_recipe_qa.py` derives the
isolated recipe-provider mod set directly from the resources and installed dependencies.

## Culinary resources — 0.2.0

The legacy compatibility datapack 1.11.0 is now embedded in this mod: 687 recipes,
67 tags and 3 advancements. All 757 resources retain their original identifiers.
741 of the 766 migrated files retain exact original bytes; 25 contain narrowly scoped
format corrections discovered by an actual 1.21.1 server reload: serialized text
components, explicit compound ingredients and an optional nonexistent ginger entry.
Nine preparation assets (three Croptopia model overrides, three raw
models and three 16×16 textures) are included as well. CustomModelData, component
markers, inedible preparations, cooking yields and existing migration recipes are unchanged.

The mandatory built-in pack `mod/hearth_horizons_compat:packs/culinary` mounts at high
priority for both server data and client assets. No separate datapack or preparation
resourcepack is needed. The existing localization resourcepack remains independent.
This culinary module targets the current full Hearth & Horizons modpack; it references
the installed food and processing mods, not only the three shelf dependencies below.

`migration/culinary-1.11.0.json` records both original and packaged hashes of all migrated files.
The original import/activation utility `tools/migrate_culinary.py` is retained for the 0.2.0 migration history.
`docs/legacy-culinary-1.11.0` preserves the original recipe documentation and backlog
as historical records; statements there about installation and future migration are superseded here.
Activation backs up both original packs and the shared resource ZIP, installs the JAR,
removes the migrated external files and rebuilds the shared resource ZIP. No game reload,
restart or world modification is performed. The new files apply on the next normal launch.

The 0.3.0 section above supersedes the original JEI backlog. Other culinary block
mechanics remain a separate backlog.

## Shelf module

The original Cluttered `small_shelf` and `small_shelf_pink` IDs gain reusable item placement.
Their model and collision move down 11/16 of a block. The board is at y=4..5/16,
with supports at y=0..4/16. Displayed objects fit in the remaining volume of the same cell.
No adjacent block is read or reserved to decide whether placement is allowed.

- Right-click with a supported item to put **one** on the shelf.
- Right-click the displayed item with an empty main hand to retrieve that item into the hand.
- A single item is centred and larger. Two are displayed side by side at a smaller scale.
- A third item is refused. There is no GUI or on-shelf access to jar contents.
- Removing the last item removes the block entity on both logical sides.
- Breaking the shelf drops each original item, preserving components and jar inventories.
- Filled shelves refuse piston movement; empty shelves retain Cluttered's original response.

Hexerei herb jars are the initial supported item. Data is kept as original ItemStacks,
including `CUSTOM_DATA.Inventory`, `ExtendedCount`, colour, name and arbitrary other
components. The mod does not unpack the jars into an alternate inventory. When retrieved
and placed normally, the original Hexerei jar operates normally.

Moonlight's `IOptionalEntityBlock` makes `hasBlockEntity()` false at `hh_items=0` and
true at `hh_items=1/2`. The mixin adds this state property to the existing Cluttered block;
old saves without the property use the default 0. No replacement block ID is registered.
There are no entity types, server tickers, GUIs, hoppers, crow integration or periodic
world scans in this module.

## Extending placement

Add item IDs to the `hearth_horizons_compat:shelf_items` item tag in a datapack.
Ordinary item rendering has a conservative default unit-cube profile.
For authored non-cube models, register their real bounds using `ObjectProfiles.register`.
`PlacementRules.Surface` owns the layout independently from the displayed item type.
The client `DisplayAdapters.register` API supports custom rendering preparation.
Adding a displayable item does not enable that item's placed-block gameplay functions.

The Hexerei display adapter caches its decoded visual representation by shelf revision
and resource-reload generation. Two client-only redirects reuse this data **only** inside
the shelf render scope and for the exact owning ItemStack. Ordinary native jar rendering
outside that scope is unchanged. Cached display objects are not registered in the world.
Layout and collision are cached until contents change. Rendering distance is 32 blocks.
No performance figures should be inferred from compilation or server tests.

## Build and test

Requires a JDK and the installed dependency JARs listed in DEPENDENCIES.md. Gradle's toolchain resolver obtains
JDK 21 if necessary. The Gradle wrapper is from the official NeoForge 1.21.1 MDK.

```powershell
.\gradlew.bat build
.\gradlew.bat runGameTestServer
```

Pass `-PminecraftProfile=C:/path/to/profile` or set `HH_MINECRAFT_PROFILE` to use another
dependency location. Builds and test worlds are written only inside this repository and
the normal Gradle cache. GameTests cover optional-BE behaviour, whole-stack round trips,
1024-count contents, right-click interaction, shrinking/recentring, third-item rejection,
same-cell bounds, occupied adjacent cells and destruction drops.

For a bootstrap build using the exact locally installed Mojang-named NeoForge distribution:

```powershell
python tools/build_local.py
```

This checks `dependency-lock.json` before compiling Java 21 bytecode. It does not alter
or distribute dependency JARs. Normal ModDevGradle remains the primary development build.

The development-only `-PshelfQa` client mode uses `run/client/saves/ShelfQA`, prepares a
small visual scene and captures `run/client/screenshots/shelf-qa.png`. It must never be
pointed at a user world. QA classes and the empty GameTest structure are excluded from
the distributed JAR (client automation and GameTest classes/structures only).

## Dependencies and distribution

Target versions: NeoForge 21.1.228, Cluttered 3.0.3, Moonlight 1.21.1-3.0.7,
Hexerei 0.5.0.3 (its declared mod version is 0.5). Exact local JAR SHA-256 values
are recorded in `dependency-lock.json`.

Install the built JAR on the host and every joining client together with those mods.
Do not remove this mod while shelves still contain items: retrieve the displayed items
first. Registry IDs and source dependency models should be reviewed before upgrading dependencies.

## Credits and license

Original compatibility code: GPL-3.0-only. Cluttered's two shelf model overrides and
ten bread/ingredient-kit portion models derive from installed Cluttered models by
YellowChuJelly/Blake (GPL 3.0). Shelf coordinates are lowered; kitchen elements are
arranged/removed to show remaining portions. Textures reference Cluttered resources.
No Hexerei, Moonlight or Another Furniture code/assets are bundled. Their APIs and
behaviour informed the integration.

The three raw preparation icons were approved by Ivan on 2026-10-15 and produced
by Image Edit from Croptopia 4.2.1 sprites. Original artwork belongs to Croptopia;
raw variants retain the original 16×16 silhouettes and transparent margins.
Cooked textures are referenced, not bundled. These inherited assets retain their
upstream attribution; the repository GPL license applies to our original code.
