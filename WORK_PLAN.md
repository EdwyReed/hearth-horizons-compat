# Current release work — 0.4.0, 2026-10-16

User scope: soda + global cooking integrity, plus served Cluttered pastries,
bread platter and baking ingredient kit. Install the tested JAR, remove the
standalone datapack, upload the mod through Chrome with restricted visibility,
and prepare/update the Modrinth modpack version. **No public release before
Ivan validates in the morning.** The existing modpack is public: never submit
a version there if the UI cannot keep that version private/draft.

Latest user steering: **upload and verify the companion mod FIRST, then export
and upload the modpack containing that exact version. Prefer the Modrinth App
over the website wherever the app supports the operation.** Chrome remains a
fallback for creator actions unavailable in the app. Never upload the modpack
before its companion dependency is available with the intended restricted access.

User clarified: export the modpack using the Modrinth App's own `.mrpack`
export to retain project/version links. Do not substitute a hand-built ZIP.
Declare companion-mod dependencies explicitly both in NeoForge metadata and
on Modrinth, including mandatory mixin targets and built-in recipe providers.
Before exporting, verify the App recognizes the installed companion JAR as the
uploaded Modrinth project/version. After native export, inspect modrinth.index.json
for its exact remote file/hash/dependency entry; do not accept the companion only
as an anonymous JAR under overrides/mods.

- [x] Inventory all 173 installed JAR recipe resources (17,731 initial resources).
- [x] Export active recipes/resolved ingredients/food and vessel properties from
  actual RecipeManager (initial 10,596 recipes in recipe-provider QA set).
- [x] Nine registered coloured soda fluids, 250mB mixer/serving recipes, remove
  manual soda routes, real basin right-click and container return on drinking.
- [x] Dedicated skillet recipe type: real placed/held FD skillets accept it;
  actual campfire rejects it. Native JEI category added.
- [x] Exact component-bearing contents on kitchen items/placed blocks. Bread
  and baking kit retrieve four original ingredients and return the tray/bowl.
- [x] Served shapeless pastries preserve the actual food's nutrition/saturation.
- [x] Expanded isolated culinary runtime includes 58 installed JARs and dependencies.
- [x] All 15 required server tests passed for the final 751-batch / 42-cold-recipe
  checkpoint, including consistent yoghurt/kimchi/miso fermentation.
- [x] Global integrity: 751 separate raw batches and oven/pot/skillet/drying/
  fermentation stages; original resolved ingredients and final yields checked.
- [ ] Review remaining manual recipes/unknown serializers and all container
  candidates. Do not treat heuristic findings as proven defects.
- [x] Native oven-served muffin captures actual food; break/eat round trips,
  component-bearing kitchen contents and partial portion models checked.
- [x] Native visual tests, liquid identities and particles with Photon first,
  then at least one other installed shader and shaders disabled.
- [x] Photon 1.3b actual client: nine fluid identities/colours and visible bubbles;
  all ten bread/kit remaining-portion models visually inspected.
- [x] Updated native JEI list/search/R-U tests for all 751 new stages; native
  transfer and six bookmarks after restart passed in two actual client processes.
- [x] Final artifact/source manifest, packaging/license audit, install with backup.
- [ ] Verified Modrinth restricted mod upload in Chrome and modpack draft/export.
- [x] Created companion project hearth-horizons-compat with target visibility
  Unlisted; currently draft/member-only. Description and GPL metadata saved.
  Companion 0.4.0 uploaded as ciWsNX4l with primary/sources JARs, 35 required
  project dependencies and optional JEI. App recognition verified before export.
- [ ] Short plain-language final change list, explicit remaining validation gate.

Miniature manual mixer: reminded by Ivan in previous discussion, no old detailed
specification recovered. Review as part of mixing architecture; do not claim it
implemented. Colony nutrition is a separate previously identified workstream,
not silently included in this cooking release.

The verified 0.4.0 JAR is installed in the main profile, with 0.3.0 backed up at
migration_backups/20261016-034105-cooking-0.4.0. No game/world/options reload occurred.
All test worlds/configs are isolated under this repository's run directory.

Final artifact gate passed: 2879 embedded files, 55 Java21 production classes,
751 raw batches; six original approved assets unchanged. SHA256:
46819a85c5d381deb332223b8caebcd4e7a3e0e07a66187fb71e16bd4cafcc89.
Actual visual review recorded in validation/2026-10-16/visual-review-0.4.0.json.
The visual harness needed authoritative connection.teleport (server-only position
changes did not reliably update the client camera) and frame-based screenshot
phases. This was a QA fixture issue; no shader/rendering setting in the main
profile was changed.

Native App export 0.2.0 completed after companion upload and recognition.
All 174 installed JARs match remote hashes or embedded bytes; 185 remote entries.
Companion is an exact Modrinth URL, not an anonymous override. Legacy mods.zip
excluded in native picker, original retained. Export is 93,418,139 bytes.
No personal roots or nonempty credential fields found by tools/verify_mrpack.py.
Modpack creator form prepared (Alpha 0.2.0); NOT submitted because the existing
project is public and the form exposes no private/draft version control.
Public release awaits Ivan gameplay acceptance, as explicitly requested.
