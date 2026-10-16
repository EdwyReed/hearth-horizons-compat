# Companion-mod dependencies

The companion targets the complete Hearth & Horizons culinary stack. Core API ranges
are specified in neoforge.mods.toml; recipe-only providers are required by ID.
JEI is optional; Streams Reflowing is an optional clean-water alternative.
The exact installed JAR hashes and native App project/version links are recorded in
validation/cooking-audit/dependencies.json. No account/session data is included.

## External files for this tested pack

The required Croptopia 4.2.1 NeoForge and MineColonies 1.1.1300 for Minecraft 1.21.1
are installed as external files. The Modrinth dependency picker does not provide
those tested releases: its Minecolonies project currently lists only Forge 1.18.2,
and Croptopia search returns add-ons/forks rather than this installed release.
They remain mandatory NeoForge dependencies and must be retained in the App export:

- `croptopia_1.21.1_NEO-FORGE-4.2.1.jar`
- `minecolonies-1.1.1300-1.21.1.jar`

The version notes declare these two external requirements explicitly; do not link
an unrelated fork or an incompatible Minecraft release to make a dependency row.

| Mod | NeoForge ID | Modrinth project | Installed source |
| --- | --- | --- | --- |
| Aquaculture 2 | aquaculture | [aquaculture](https://modrinth.com/project/Vl1uNAuy) | Aquaculture-1.21.1-2.7.21.jar |
| Aquaculture Delight | aquaculturedelight | [aquaculture-delight](https://modrinth.com/project/U9GJqWrI) | aquaculturedelight-1.2.0-neoforge-1.21.1.jar |
| [Let's Do] Bakery - Farm&Charm Compat | bakery | [lets-do-bakery-farmcharm-compat](https://modrinth.com/project/oNB5jhlA) | letsdo-bakery-neoforge-2.1.6.jar |
| Biomes O' Plenty | biomesoplenty | [biomes-o-plenty](https://modrinth.com/project/HXF82T3G) | BiomesOPlenty-neoforge-1.21.1-21.1.0.14.jar |
| [Let's Do] Brewery - Farm&Charm Compat | brewery | [lets-do-brewery-farmcharm-compat](https://modrinth.com/project/b7NV2plI) | letsdo-brewery-neoforge-2.1.9.jar |
| Brewin' And Chewin' | brewinandchewin | [brewin-and-chewin](https://modrinth.com/project/hIu9KJTT) | BrewinAndChewin-neoforge-4.4.2+1.21.1.jar |
| Cluttered | cluttered | [cluttered](https://modrinth.com/project/UnAEo77V) | cluttered-NeoForge-3.0.3-1.21.1.jar |
| More Delight (for Farmer's Delight) | moredelight | [more-delight](https://modrinth.com/project/znHQQtuU) | moredelight-26.04.30-1.21-neoforge.jar |
| Crabber's Delight | crabbersdelight | [crabbers-delight](https://modrinth.com/project/gBGdVBJy) | crabbersdelight-1.21.1-1.2.6.jar |
| Crate Delight | cratedelight | [crate-delight](https://modrinth.com/project/9rlXSyLg) | cratedelight-25.09.22-1.21-neoforge.jar |
| Create | create | [create](https://modrinth.com/project/LNytGWDc) | create-1.21.1-6.0.10.jar |
| Create Crafts & Additions | createaddition | [createaddition](https://modrinth.com/project/kU1G12Nn) | createaddition-1.5.10.jar |
| Create: Diesel Generators | createdieselgenerators | [create-diesel-generators](https://modrinth.com/project/ZM3tt6p1) | createdieselgenerators-1.21.1-1.3.11.jar |
| croptopia | croptopia | No matching installed-file link in App cache; provide the installed JAR separately | croptopia_1.21.1_NEO-FORGE-4.2.1.jar |
| Croptopia Delight | croptopiadelight | [croptopia-delight](https://modrinth.com/project/z2Ihxkgp) | croptopiadelight-1.0.1.jar |
| Cuisine Delight | cuisinedelight | [cuisine-delight](https://modrinth.com/project/gOQjCAAD) | cuisinedelight-1.2.8.jar |
| Cultural Delights | culturaldelights | [cultural-delights](https://modrinth.com/project/YttyNOFA) | culturaldelights-0.17.7.jar |
| Enchanted: Witchcraft | enchanted | [enchanted-witchcraft](https://modrinth.com/project/HsbpdVo9) | enchanted-neoforge-1.21.1-4.2.7.jar |
| Environmental | environmental | [environmental](https://modrinth.com/project/OqtiAZcV) | environmental-1.21.1-5.0.1.jar |
| Expanded Delight | expandeddelight | [expanded-delight](https://modrinth.com/project/e9V6wFcR) | expandeddelight-0.1.4.jar |
| [Let's Do] Farm & Charm | farm_and_charm | [lets-do-farm-charm](https://modrinth.com/project/HJetCzWo) | letsdo-farm_and_charm-neoforge-1.1.22.jar |
| Farmer's Delight | farmersdelight | [farmers-delight](https://modrinth.com/project/R2OftAxM) | FarmersDelight-1.21.1-1.3.1.jar |
| Farmer's Delight: Extended | farmersdelight_extended | [farmers-delight-extended](https://modrinth.com/project/CxjQ2P6f) | farmersdelight_extended-1.21.1-0.2.2.jar |
| [Let's Do] HerbalBrews | herbalbrews | [lets-do-herbalbrews](https://modrinth.com/project/Eh11TaTm) | letsdo-herbalbrews-neoforge-1.1.3.jar |
| Hexerei | hexerei | [hexerei](https://modrinth.com/project/IQSOHVhI) | hexerei-0.5.0.3.jar |
| Immersive Engineering | immersiveengineering | [immersiveengineering](https://modrinth.com/project/tIm2nV03) | ImmersiveEngineering-1.21.1-12.4.2-194.jar |
| minecolonies | minecolonies | No matching installed-file link in App cache; provide the installed JAR separately | minecolonies-1.1.1300-1.21.1.jar |
| Moonlight Lib | moonlight | [moonlight](https://modrinth.com/project/twkfQtEc) | moonlight-neoforge-1.21.1-3.0.7.jar |
| Ocean's Delight | oceansdelight | [oceans-delight](https://modrinth.com/project/DGiq4ZSW) | oceansdelight-neoforge-1.0.4-1.21.1.jar |
| Pam's HarvestCraft 2: Crops | pamhc2crops | [pams-harvestcraft-2-crops](https://modrinth.com/project/5xO6VNlk) | pamhc2crops-NEOFORGE-1.21.1-1.0.0.jar |
| Pam's HarvestCraft 2: Food Core | pamhc2foodcore | [pams-harvestcraft-2-food-core](https://modrinth.com/project/Z9OywhE4) | pamhc2foodcore-NEOFORGE-1.21.1-1.0.2.jar |
| Pam's HarvestCraft 2 - Food Extended | pamhc2foodextended | [pams-harvestcraft-2-food-extended](https://modrinth.com/project/bdPeAbPS) | pamhc2foodextended-NEOFORGE-1.21.1-1.0.0.jar |
| Pam's HarvestCraft 2 - Trees | pamhc2trees | [pams-harvestcraft-2-trees](https://modrinth.com/project/KDzWXxuG) | pamhc2trees-NEOFORGE-1.21.1-1.0.4.jar |
| Pineapple Delight | pineapple_delight | [pineapple-delight](https://modrinth.com/project/8wXnuOvg) | pineapple_delight-1.1.2-1.21.1-neoforge.jar |
| Rustic Delight | rusticdelight | [rustic-delight](https://modrinth.com/project/foa4fGIH) | rusticdelight-neoforge-1.21-1.6.0.jar |
| Supplementaries | supplementaries | [supplementaries](https://modrinth.com/project/fFEIiSDQ) | supplementaries-neoforge-1.21.1-3.6.4.jar |
| [Let's Do] Vinery | vinery | [lets-do-vinery](https://modrinth.com/project/1DWmBJVA) | letsdo-vinery-neoforge-1.5.3.jar |
