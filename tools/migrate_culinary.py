"""Import, normalize 1.21.1 codec formats, verify and activate the legacy pack in the mod.

Import is read-only for the Minecraft profile. Activation requires a built, verified JAR
and backs up the external packs before removing their migrated resources.
"""
from pathlib import Path
import argparse, hashlib, json, shutil, zipfile
from datetime import datetime

ROOT = Path(__file__).resolve().parents[1]
PROFILE = Path(r'C:\Users\petyl\AppData\Roaming\ModrinthApp\profiles\NeoForge 1.21.1')
DATA = PROFILE / 'moonlight-global-datapacks/hearth_horizons_compat'
RESOURCE = PROFILE / 'resourcepacks/Hearth & Horizons'
TARGET = ROOT / 'src/main/resources/packs/culinary'
MANIFEST = ROOT / 'migration/culinary-1.11.0.json'
JAR = ROOT / 'build/libs/hearth-horizons-compat-0.2.0.jar'

def digest(data):
    return hashlib.sha256(data).hexdigest()

def asset_paths():
    for base in ('models', 'textures'):
        yield from sorted((RESOURCE / f'assets/hearth_horizons/{base}/item/preparations').glob('*'))
    for item in ('raisin_oatmeal_cookie', 'cashew_chicken', 'beef_stir_fry'):
        yield RESOURCE / f'assets/croptopia/models/item/{item}.json'

def verify():
    manifest = json.loads(MANIFEST.read_text(encoding='utf8'))
    with zipfile.ZipFile(JAR) as jar:
        assert jar.testzip() is None
        for path, expected in manifest.get('packaged_files', manifest['files']).items():
            assert digest(jar.read('packs/culinary/' + path)) == expected, path
    for path, expected in manifest.get('packaged_files', manifest['files']).items():
        assert digest((TARGET / path).read_bytes()) == expected, path
    print('Verified packaged JAR hashes:', len(manifest['files']))
    return manifest

parser = argparse.ArgumentParser()
parser.add_argument('action', choices=('import', 'normalize', 'verify', 'activate'))
args = parser.parse_args()
if args.action == 'import':
    assert not MANIFEST.exists(), 'Import already recorded; do not overwrite the baseline'
    sources = [(p, p.relative_to(DATA).as_posix()) for p in sorted((DATA / 'data').rglob('*')) if p.is_file()]
    sources += [(p, p.relative_to(RESOURCE).as_posix()) for p in asset_paths()]
    assert len(sources) == 766, len(sources)
    files = {}
    for source, path in sources:
        data = source.read_bytes()
        if path.endswith('.json'):
            json.loads(data)
        destination = TARGET / path
        destination.parent.mkdir(parents=True, exist_ok=True)
        destination.write_bytes(data)
        files[path] = digest(data)
    archive_docs = ROOT / 'docs/legacy-culinary-1.11.0'
    archive_docs.mkdir(parents=True, exist_ok=True)
    for name in ('README.md', 'MOD_BACKLOG.md', 'pack.mcmeta'):
        shutil.copyfile(DATA / name, archive_docs / name)
    MANIFEST.parent.mkdir(parents=True, exist_ok=True)
    MANIFEST.write_text(json.dumps({'source_version': '1.11.0', 'date': '2026-10-16',
        'recipes': 687, 'tags': 67, 'advancements': 3, 'assets': 9, 'files': files}, indent=2) + '\n', encoding='utf8')
    print('Imported 757 data resources and 9 preparation assets without changing IDs or components')
elif args.action == 'normalize':
    manifest = json.loads(MANIFEST.read_text(encoding='utf8'))
    def text_components(value):
        if isinstance(value, dict):
            return {k: (json.dumps(v, ensure_ascii=False, separators=(',', ':'))
                        if k in ('minecraft:item_name', 'minecraft:custom_name') and isinstance(v, dict)
                        else [json.dumps(line, ensure_ascii=False, separators=(',', ':')) if isinstance(line, dict) else line for line in v]
                        if k == 'minecraft:lore' else text_components(v)) for k, v in value.items()}
        if isinstance(value, list):
            return [text_components(v) for v in value]
        return value
    def compound(value):
        return {'type': 'neoforge:compound', 'children': value} if isinstance(value, list) and value else value
    changes = {}
    for path, expected in manifest['files'].items():
        file = TARGET / path
        before = file.read_bytes()
        assert digest(before) == manifest.get('packaged_files', manifest['files'])[path], path
        original = ((DATA if path.startswith('data/') else RESOURCE) / path).read_bytes()
        assert digest(original) == expected, path
        if path.endswith('.json'):
            old = json.loads(original)
            value = text_components(old)
            if '/recipe/' in path:
                if 'ingredients' in value:
                    value['ingredients'] = [compound(v) for v in value['ingredients']]
                if 'ingredient' in value:
                    value['ingredient'] = compound(value['ingredient'])
                if 'key' in value:
                    value['key'] = {k: compound(v) for k, v in value['key'].items()}
            if path == 'data/hearth_horizons/tags/item/vegetables/ginger.json':
                value['values'] = [{'id': v, 'required': False} if v == 'culturaldelights:ginger' else v for v in value['values']]
            if value != old:
                file.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
            else:
                file.write_bytes(original)
        after = file.read_bytes()
        if digest(after) != expected:
            changes[path] = digest(after)
    manifest['packaged_files'] = dict(manifest['files'], **changes)
    manifest['format_corrections'] = changes
    MANIFEST.write_text(json.dumps(manifest, indent=2) + '\n', encoding='utf8')
    qa = ROOT / 'src/main/resources/qa/culinary-baseline.json'
    qa.parent.mkdir(exist_ok=True)
    qa.write_text(json.dumps({k:v for k,v in manifest['packaged_files'].items() if k.startswith('data/')}, indent=2))
    print('Recorded format corrections:', len(changes), '; remaining files byte-identical to legacy pack')
elif args.action == 'verify':
    verify()
else:
    manifest = verify()
    # Reject intervening edits before making any profile changes.
    for path, expected in manifest['files'].items():
        source = (DATA if path.startswith('data/') else RESOURCE) / path
        assert digest(source.read_bytes()) == expected, f'Source changed: {source}'
    backup = ROOT / 'migration_backups' / datetime.now().strftime('%Y%m%d-%H%M%S')
    backup.mkdir(parents=True)
    shutil.copytree(DATA, backup / 'datapack')
    shutil.copytree(RESOURCE, backup / 'resourcepack')
    shared_zip = PROFILE / 'Hearth & Horizons.zip'
    if shared_zip.exists():
        shutil.copyfile(shared_zip, backup / shared_zip.name)
    # Install the complete artifact before removing its legacy copies.
    installed = PROFILE / 'mods' / JAR.name
    assert not list((PROFILE / 'mods').glob('hearth-horizons-compat-*.jar')), 'Review an already installed version first'
    shutil.copyfile(JAR, installed)
    assert digest(installed.read_bytes()) == digest(JAR.read_bytes())
    for path in manifest['files']:
        source = (DATA if path.startswith('data/') else RESOURCE) / path
        source.unlink()
    # Remove only the now empty, checked legacy datapack directory, including its docs.
    assert DATA.resolve() == (PROFILE.resolve() / 'moonlight-global-datapacks/hearth_horizons_compat')
    assert not any(p.is_file() for p in (DATA / 'data').rglob('*'))
    shutil.rmtree(DATA)
    readme = RESOURCE / 'README.md'
    text = readme.read_text(encoding='utf8')
    start = text.index('## Иконки заготовок')
    end = text.index('Общий ресурспак')
    readme.write_text(text[:start] + 'Рецепты совместимости и девять файлов моделей/иконок заготовок перенесены в Hearth & Horizons Compatibility 0.2.0. На сервере и каждом клиенте нужен этот мод; данный ресурспак содержит локализацию и шрифты.\n\n' + text[end:], encoding='utf8')
    credits = RESOURCE / 'CREDITS.md'
    text = credits.read_text(encoding='utf8')
    start = text.index('Три иконки сырых заготовок')
    end = text.index('Русская локализация')
    credits.write_text(text[:start] + text[end:], encoding='utf8')
    if shared_zip.exists():
        with zipfile.ZipFile(shared_zip, 'w', zipfile.ZIP_DEFLATED) as archive:
            for file in sorted(RESOURCE.rglob('*')):
                if file.is_file():
                    archive.write(file, file.relative_to(RESOURCE).as_posix())
        with zipfile.ZipFile(shared_zip) as archive:
            assert archive.testzip() is None
            assert not (set(manifest['files']) & set(archive.namelist()))
    report = {'date': datetime.now().isoformat(), 'backup': str(backup), 'installed': str(installed),
              'sha256': digest(JAR.read_bytes()), 'data_resources': 757, 'assets': 9,
              'main_game_reloaded_or_restarted': False}
    (ROOT / 'migration/activation.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf8')
    print(json.dumps(report, indent=2))
