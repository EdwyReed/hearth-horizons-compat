# Development

This is the source repository for the Hearth & Horizons companion mod, targeting
Minecraft 1.21.1, NeoForge 21.1.228 and Java 21. See README.md for gameplay and
DEPENDENCIES.md for the required providers and exact versions.

## Build

Install the dependency mods into a separate development profile. Third-party
mod JARs, Minecraft files and user worlds are not distributed in this repository.
Point Gradle at that profile explicitly:

```powershell
.\gradlew.bat -PminecraftProfile=C:/path/to/profile build
```

On Linux/macOS, use `./gradlew` with the equivalent absolute profile path. The
Windows Modrinth default is a convenience; other layouts require this property
or the `HH_MINECRAFT_PROFILE` environment variable.

The optional Python utilities use only the standard library. Some historical
audit/install tools contain the original workstation paths; review their profile
setting before running them. Installation and migration tools modify the selected
profile and are not part of a normal source build.

## Validation

Use isolated test worlds and a separate profile. Never point QA at a player world.

```powershell
.\gradlew.bat -PminecraftProfile=C:/path/to/profile -PrecipePackQa runGameTestServer
```

The recipe QA runtime set is recorded in `qa-recipe-mods.json`. Client checks also
need the relevant client dependencies. See VALIDATION.md for the actual checks,
shader versions and limits of the recorded 0.4.0 evidence. A successful build
alone does not establish gameplay or rendering acceptance.

Release verification uses generated build/runtime evidence:

```powershell
python tools/verify_release.py --require-runtime
```

Historical evidence is included for review; it is not a claim that a fresh clone
already contains the locally generated logs, caches or release binaries.

## Releases

Keep recipe identifiers, final yields and complete ItemStack components intact.
Review mixins against the exact dependency versions. Preserve upstream asset
attribution and GPL-3.0-only licensing; see README.md and LICENSE.

Upload the companion mod first, verify its exact project/version recognition in
Modrinth App, then use the App's native modpack export. Do not manually rewrite
the export ZIP. Public release still requires Ivan's gameplay acceptance.
