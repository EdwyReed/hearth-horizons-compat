# Building

Use Java 21 and Python 3.11 or newer. Dependencies are read from an installed Minecraft profile; the build does not modify it.

```sh
python tools/build_local.py --profile "/path/to/profile"
```

The script checks `dependency-lock.json` before building. Output: `build/libs/`.

Run the server tests with the recipe dependencies listed in `qa-recipe-mods.json`:

```sh
./gradlew -PminecraftProfile="/path/to/profile" -PrecipePackQa runGameTestServer
```

On Windows, use `gradlew.bat`. Test worlds stay in `run/`. Tests and fixtures are excluded from the release JAR.

To regenerate the mixer mesh, run `pnpm install --frozen-lockfile` and `pnpm generate` in `tools/mixer-model/`.
