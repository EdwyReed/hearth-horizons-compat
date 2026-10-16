# Validation — 2026-10-16

## Current 0.4.0 release checks

All development runs use isolated worlds/configs under this repository's `run/`.
The verified 0.4.0 JAR is now installed in the main profile, with 0.3.0 backed up.
The main game, worlds and options were not restarted, reloaded or changed.
Full-profile gameplay acceptance remains pending.

- Static inventory covers all 173 installed top-level JARs. The runtime set contains
  58 culinary providers/dependencies, including Caupona, Cooking for Blockheads,
  Slice and Dice and remaining Delight add-ons. See `validation/cooking-audit`.
- Fifteen required GameTests cover shelves, embedded resource priority/decoding,
  nine actual 250mB soda routes and native bottle use, placed/held skillet acceptance
  and campfire rejection, kitchen content/NBT/portion round trips, actual native
  Farm & Charm oven output, vessel ledgers and cooked ingredients in cold recipes.
- The 751 preparation manifest preserves original resolved ingredient choices,
  final item IDs and batch yields. The cold-recipe suite checks all 42 substitutions
  against actual matching, including rejection of their former raw inputs.
- Native JEI checks cover every registered preparation's list entry, R/U and raw
  input slots; representative localized search and cooked R across all five stages;
  native stove transfer/rejection and six original bookmarks across process restart.
- Photon 1.3b native render run completed: all nine 750mB client fluids synchronized,
  hundreds of custom bubbles spawned. Viewed wide shots at two different frames and a top
  view show all nine distinct colours and moving bubbles. Viewed kitchen models
  show five portion states each for bread and ingredient kits.

These are acceptance criteria as well as checkpoint results: final packaging requires
the matching current logs, screenshots and exact source/artifact hashes. The final
artifact report records which gates completed; this document alone is not a pass.
Complementary Reimagined and shaders-disabled render checks are also mandatory.

Limits: the entire 173-mod gameplay stack, remote multiplayer, every dynamic machine
serializer's internal economics and mass-render FPS are not claimed as tested.
The 130 generated multi-output Slice and Dice cutting recipes are active but cannot
round-trip through their upstream JSON encoder, which imposes a one-output limit.
This exporter limitation is not evidence that the actual cutting recipes failed.

Historical version-specific evidence follows; it is not relabelled as current QA.

## Shelf module 0.1.0 evidence

Target: Minecraft 1.21.1, NeoForge 21.1.228, Cluttered 3.0.3, Moonlight
1.21.1-3.0.7, Hexerei 0.5.0.3. Runs use an isolated development profile under
this repository. The user's Modrinth profile and worlds were not changed.

## Passed

- Standard ModDevGradle Java 21 compilation and `build`.
- Seven required NeoForge GameTests, including:
  - Empty original shelves have no BlockEntity, even after repeated lookups.
  - Both shelf variants and all horizontal directions support right-click round trips.
  - 0 → 1 → 2 → 1 → 0 changes layout/scale, refuses a third jar and deletes storage.
  - Full ItemStack components survive registry-aware save/load; 64/65/1024 contents,
    names, dye, crow flag and unrelated custom data are preserved.
  - A retrieved jar places using the native Hexerei item, creating the actual
    HerbJarTile with all 1024 contents, colour, name and crow setting restored.
  - Contents and collision fit the shelf's own cell; blocks above/below do not prevent use.
  - Destruction drops exactly two original jars, without losing their contents.
  - 1000 occupied shelves / 2000 jars have no tickers, retain contents and release
    all storage after being emptied. This is a lifecycle stress test, not an FPS benchmark.
- A real, separate Minecraft client displayed the lowered shelves and native jars.
- Client state/content synchronisation for empty/single/double shelves and deletion
  of the last jar's BlockEntity passed in the integrated-server run.
- Native rendered-vertex bounds were instrumented relative to the shelf cell:
  - Single jar: min=(0.30229, 0.31250, 0.58357), max=(0.69771, 0.96711, 0.97893).
  - Two jars together: min=(0.09492, 0.31250, 0.61057), max=(0.90508, 0.87770, 0.95193).
  - The glass base meets the board at y=5/16; all rendered vertices remain inside the cell.
- Client render cache rebuilt only for the three initial jars and one reinserted jar;
  additional instrumentation recorded 1851 actual cache hits in the native Hexerei renderer.
- Distributed JAR: Java 21 class files, valid ZIP CRC, correct model coordinates;
  development automation, GameTests and their structures are excluded.
- Exact original dependency JAR SHA-256 values still match `dependency-lock.json`.

Evidence is retained in `validation/2026-10-16/` and full development logs under `build/`.

## Acceptance limits

The runtime tests use the required dependency mods, not the entire large modpack.
No separate remote/LAN client, Iris/Sodium/Entity Culling combination, shader pack or
Create contraption integration was exercised. No mass-render FPS/RAM benchmark is
claimed. Normal block destruction and filled-shelf piston refusal are implemented;
special third-party block movers need separate compatibility testing.

The statements above describe the original 0.1.0 shelf validation. Subsequent
0.2.0 culinary migration and profile activation are recorded below and in
`migration/activation.json`; the earlier visual run has not been relabelled as a 0.2.0 test.

## Culinary module 0.2.0

- Standard Gradle `build runGameTestServer -PfullPackQa` passed with the installed
  gameplay mods in an isolated server under `run/gametest`. Client presentation
  mods are excluded from this dedicated-server test (see `build.gradle`).
- All eight required GameTests passed: the seven shelf regressions plus culinary loading.
- The live server resolved all 757 migrated resources from the required embedded
  pack, at the intended priority, and matched their packaged SHA-256 baseline.
- All 648 unconditional migrated recipes were present in RecipeManager. All 29
  explicitly disabled recipes remained absent. Other conditional recipes retain
  their original activation conditions.
- All three raw/cooked preparation stages and legacy migrations decoded. Actual
  raw outputs have no FOOD; cooked outputs retain FOOD and yields 4/1/1. Cooking
  accepts each actual raw output and rejects the corresponding cooked item.
- There were no parsing errors for any migrated recipe and no missing
  `hearth_horizons` tag errors after the format corrections.
- Runtime testing found pre-existing 1.11.0 format errors. Nine recipes now encode
  text components as serialized JSON strings, as required by Minecraft 1.21.1;
  fifteen recipes use explicit `neoforge:compound` alternative ingredients;
  the absent `culturaldelights:ginger` tag entry is optional. These 25 files are
  recorded separately in the migration manifest, including their original hashes.
  Empty ingredient slots in Brewin' and Chewin' recipes remain empty.
- The other 741 migrated files remain byte-identical, including all nine assets.
  All three PNG textures retain their original 16×16 dimensions.
- JAR CRC, Java 21 classes and exclusion of development-only QA code/resources passed.

The full gameplay-mod server still emits upstream warnings/errors for unrelated
resources (for example optional Railways loot tables and MoAnimals tags). These
are outside this migration; a globally clean modpack log is not claimed.
No new 0.2.0 client visual/JEI acceptance run is claimed. Full JEI ingredient-list,
search, R/U and transfer integration remains pending.

Profile activation installs the verified 0.2.0 JAR, removes the old standalone
compatibility datapack and nine preparation resources, and rebuilds the shared
`Hearth & Horizons.zip` resourcepack. Localization and fonts remain in the resourcepack.
Original packs and ZIP are backed up outside the profile. The exact backup path,
installed artifact hash and activation time are in `migration/activation.json`.
The running game, options and user worlds are not reloaded, restarted or edited.

## Preparations and JEI 0.3.0 — current scope

The 0.2.0 JEI backlog and legacy-migration descriptions above are historical.
Ivan confirmed that no old preparations had been created and explicitly discarded
that compatibility. The current artifact has three registered raw item IDs, 684 recipes,
67 tags and 3 advancements. The three legacy migration recipes, their custom ingredient
handler/subtype interpreter, and three old Croptopia model overrides are absent.
All six original raw-model/texture files remain byte-identical; five resources add
the new item-model links and English/Russian names. Cooked Croptopia items use their
original models. All resources are embedded in the JAR.

Eight server GameTests passed for this final scope. The live resource manager loaded
all 754 data resources from the intended pack with matching hashes. RecipeManager
contained all 645 unconditional recipes; 29 deliberately disabled recipes remained
absent. Three preparation/cooking stages retain the original ingredients, time,
container outputs and cooked yields 4/1/1. Raw outputs have no FOOD and cooked
outputs retain FOOD. Cooking accepts its real raw item and rejects the cooked dish.
The server additionally verifies that none of the discarded migration IDs exist.

The JEI release gate runs a separate development client twice, with an actual process
restart between runs. It checks six distinct ingredient-list entries, Russian name
searches, R/U recipe focus for all three raw/cooked pairs, actual raw-slot stacks,
native Farm & Charm stove transfer and rejection of cooked food as a substitute,
and six native JEI bookmarks saved and restored from disk. Artifact verification
requires both completed client logs and the passing server log.

Both native client runs passed for the final 0.3.0 artifact, including bookmark
restoration after the process restart. The final screenshots were inspected.

Runtime QA uses the 50 installed resource providers and their mandatory dependencies,
resolved in `qa-recipe-mods.json`. Aeronautics failed its service lookup on the broader
development classpath; removing it exposed invalid optional Create Dragons Plus
references in Sable's fragile-block tag. Those unrelated mods are outside the recipe
QA set. The copied development world uses a vanilla flat generator and the existing
translation pack. This is not a whole-modpack shader/physics acceptance test.
The user's main game, saves and options are not used for testing.

The release report `artifact-0.3.0.json`, two client result logs, server results and
four actual screenshots are packaged under `validation/2026-10-16`. The installer
backs up 0.2.0 and records the new artifact path/hash in `migration/activation-0.3.0.json`.
