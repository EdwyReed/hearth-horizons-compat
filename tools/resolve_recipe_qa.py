"""Resolve every installed resource provider plus its mandatory dependencies for JEI QA."""
from pathlib import Path
import json, re, tomllib, zipfile

root = Path(__file__).resolve().parents[1]
mods = Path(r'C:\Users\petyl\AppData\Roaming\ModrinthApp\profiles\NeoForge 1.21.1\mods')
by_id, metadata = {}, {}
for jar in mods.glob('*.jar'):
    if jar.name.startswith('hearth-horizons-compat-'):
        continue
    with zipfile.ZipFile(jar) as archive:
        for entry in ('META-INF/neoforge.mods.toml', 'META-INF/mods.toml'):
            if entry in archive.namelist():
                info = tomllib.loads(archive.read(entry).decode('utf8'))
                metadata[jar.name] = info
                for mod in info.get('mods', []):
                    by_id[mod['modId']] = jar.name
                break
namespaces = {'jei', 'cluttered', 'moonlight', 'hexerei',
              'caupona', 'cookingforblockheads', 'sliceanddice',
              'chefsdelight', 'storagedelight', 'vanilladelight'}
for file in (root / 'src/main/resources/packs/culinary/data').rglob('*.json'):
    namespaces.update(re.findall(r'"#?([a-z0-9_]+):[a-z0-9_./-]+"', file.read_text(encoding='utf8')))
selected = {by_id[namespace] for namespace in namespaces if namespace in by_id}
# Kotlin for Forge supplies its mod through a nested JAR; it has no top-level
# mods.toml entry for the dependency resolver above.
if 'sliceanddice' in namespaces:
    selected.update(jar.name for jar in mods.glob('*kotlinforforge*.jar'))
while True:
    before = set(selected)
    for name in before:
        for dependencies in metadata.get(name, {}).get('dependencies', {}).values():
            for dependency in dependencies:
                required = dependency.get('type', 'required') == 'required' and dependency.get('mandatory', dependency.get('required', True))
                if required and dependency['modId'] in by_id:
                    selected.add(by_id[dependency['modId']])
    if selected == before:
        break
result = {'scope': 'Installed culinary providers, including independent Caupona, Cooking for Blockheads, Slice and Dice and remaining Delight add-ons, plus providers referenced by migrated data, JEI and shelf dependencies and their mandatory dependency closure. NeoForge discovers nested libraries.',
          'files': sorted(selected), 'referenced_namespaces': sorted(namespaces),
          'mod_ids': sorted(key for key, value in by_id.items() if value in selected)}
(root / 'qa-recipe-mods.json').write_text(json.dumps(result, indent=2) + '\n')
print('Resolved QA mod JARs:', len(selected))
