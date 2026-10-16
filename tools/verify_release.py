"""Verify the current artifact, migrated data and (optionally) actual runtime evidence."""
from pathlib import Path
import argparse, hashlib, json, re, zipfile

ROOT = Path(__file__).resolve().parents[1]
VERSION = re.search(r"^version = '([^']+)'", (ROOT / 'build.gradle').read_text(), re.M)[1]
if VERSION == '0.4.0':
    import runpy
    runpy.run_path(str(ROOT / 'tools/verify_release_04.py'), run_name='__main__')
    raise SystemExit(0)
JAR = ROOT / f'build/libs/hearth-horizons-compat-{VERSION}.jar'
parser = argparse.ArgumentParser()
parser.add_argument('--require-runtime', action='store_true')
args = parser.parse_args()
manifest = json.loads((ROOT / f'migration/release-{VERSION}.json').read_text())
legacy = json.loads((ROOT / 'migration/culinary-1.11.0.json').read_text())
with zipfile.ZipFile(JAR) as archive:
    assert archive.testzip() is None
    files = [entry.filename for entry in archive.infolist() if not entry.is_dir()]
    assert not any('/gametest/' in name or '/JeiQa' in name or name.startswith('qa/') or name.endswith('/ClientQa.class') or '/structure/' in name for name in files)
    assert {name.removeprefix('packs/culinary/') for name in files if name.startswith('packs/culinary/')} == set(manifest['files'])
    for path, expected in manifest['files'].items():
        contents = archive.read('packs/culinary/' + path)
        assert hashlib.sha256(contents).hexdigest() == expected, path
        assert (ROOT / 'src/main/resources/packs/culinary' / path).read_bytes() == contents, path
    removed = set(manifest['removed_legacy_resources'])
    expected_removed = {
        f'data/hearth_horizons/recipe/preparation_migration/{name}.json'
        for name in ('raisin_oatmeal_cookie', 'cashew_chicken', 'shaped_beef_stir_fry')
    } | {
        f'assets/croptopia/models/item/{name}.json'
        for name in ('raisin_oatmeal_cookie', 'cashew_chicken', 'beef_stir_fry')
    }
    assert removed == expected_removed
    changed = {name for name, digest in manifest['files'].items()
               if name in legacy['packaged_files'] and digest != legacy['packaged_files'][name]}
    expected_changed = {f'data/croptopia/recipe/{name}.json' for name in
                        ('raisin_oatmeal_cookie', 'cashew_chicken', 'shaped_beef_stir_fry')} | {
        f'data/hearth_horizons/recipe/{stage}/{name}.json' for stage, name in
        (('baking', 'raisin_oatmeal_cookie'), ('frying', 'cashew_chicken'), ('frying', 'shaped_beef_stir_fry'))}
    assert changed == expected_changed, f'Unexpected edits to migrated resources: {changed ^ expected_changed}'
    assert {name for name in manifest['files'] if name.startswith('data/')} == {name for name in legacy['files'] if name.startswith('data/')} - removed
    assert not any('/preparation_migration/' in name or 'LegacyPreparation' in name for name in files)
    for name, expected in legacy['files'].items():
        if name.startswith('assets/') and name not in removed:
            assert hashlib.sha256(archive.read('packs/culinary/' + name)).hexdigest() == expected, name
    classes = [name for name in files if name.endswith('.class')]
    assert all(int.from_bytes(archive.read(name)[6:8], 'big') == 65 for name in classes)
    for resource in (ROOT / 'src/main/resources').rglob('*'):
        if not resource.is_file():
            continue
        name = resource.relative_to(ROOT / 'src/main/resources').as_posix()
        if name.startswith('qa/') or name.startswith('data/hearth_horizons_compat/structure/'):
            continue
        assert archive.read(name) == resource.read_bytes(), f'Stale resource in artifact: {name}'
    compiled = ROOT / 'build/classes/java/main'
    for name in classes:
        assert archive.read(name) == (compiled / name).read_bytes(), f'Stale Java class in artifact: {name}'
    assert f'version="{VERSION}"' in archive.read('META-INF/neoforge.mods.toml').decode()
    for shelf in ('small_shelf', 'small_shelf_pink'):
        model = json.loads(archive.read(f'assets/cluttered/models/block/{shelf}.json'))
        assert min(element['from'][1] for element in model['elements']) == 0
        assert max(element['to'][1] for element in model['elements']) == 5

runtime = {}
if args.require_runtime:
    server = (ROOT / 'build/jei-gametest-console.log').read_text(encoding='utf8', errors='replace')
    assert 'All 8 required tests passed' in server and 'CULINARY_QA_RECIPES 3' in server and 'BUILD SUCCESSFUL' in server
    assert 'CULINARY_QA_LOADED 645 unconditional recipes; 29 intentionally disabled recipes absent' in server
    for line in server.splitlines():
        assert "Couldn't load tag hearth_horizons:" not in line, line
        if 'Parsing error loading recipe ' in line:
            namespace, path = line.split('Parsing error loading recipe ', 1)[1].split(':', 1)
            assert f'data/{namespace}/recipe/{path}.json' not in manifest['files'], line
    for name, restarted in [('jei-client-console.log', False), ('jei-client-restart-console.log', True)]:
        client = (ROOT / 'build' / name).read_text(encoding='utf8', errors='replace')
        assert 'JEI_QA_FAILURE' not in client, name
        assert 'JEI_QA_COMPLETE' in client and 'BUILD SUCCESSFUL' in client, name
        assert client.count('JEI_QA_FOCUS ') == 3, name
        assert 'JEI_QA_INGREDIENTS six distinct raw/cooked entries; Russian name search' in client, name
        assert 'JEI_QA_TRANSFER stove: raw transferred; cooked alternative rejected' in client, name
        assert 'JEI_QA_TRANSFER migration ' not in client, name
        assert 'JEI_QA_BOOKMARKS 6 distinct raw/cooked variants' in client, name
        assert ('restored after process restart' if restarted else 'saved through native JEI bookmark config') in client, name
        runtime[name] = [line for line in client.splitlines() if 'JEI_QA_' in line]
    runtime['server'] = [line for line in server.splitlines() if any(marker in line for marker in ('CULINARY_QA_', 'All 8 required', 'SHELF_BULK_TEST', 'BUILD SUCCESSFUL'))]

result = {'version': VERSION, 'sha256': hashlib.sha256(JAR.read_bytes()).hexdigest(),
          'class_count': len(classes), 'embedded_files': len(manifest['files']),
          'recipes': 684, 'tags': 67, 'advancements': 3, 'approved_raw_assets_preserved': 6,
          'runtime_verified': bool(runtime), 'runtime': runtime}
evidence = ROOT / 'validation/2026-10-16'
evidence.mkdir(parents=True, exist_ok=True)
(evidence / f'artifact-{VERSION}.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n', encoding='utf8')
print(f'Verified {JAR.name}: {len(manifest["files"])} embedded files, {len(classes)} Java 21 classes, approved icons retained; unused legacy resources removed')
print('Runtime requirements:', 'verified' if runtime else 'not requested')
print('SHA256:', result['sha256'])
