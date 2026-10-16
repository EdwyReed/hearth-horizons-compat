"""Package the current release only after artifact and actual runtime verification."""
from pathlib import Path
import json, re, shutil, subprocess, sys, zipfile

root = Path(__file__).resolve().parents[1]
version = re.search(r"^version = '([^']+)'", (root / 'build.gradle').read_text(), re.M)[1]
subprocess.run([sys.executable, str(root / 'tools/verify_release.py'), '--require-runtime'], check=True)
evidence = root / 'validation/2026-10-16'
report = json.loads((evidence / f'artifact-{version}.json').read_text(encoding='utf8'))
for name in ('jei-preparations-list.png', 'jei-raw-recipes.png', 'jei-cooked-recipes.png', 'jei-stove-transfer.png',
             'jei-expanded-skillet.png', 'jei-expanded-pot.png', 'jei-expanded-drying.png', 'jei-expanded-fermenting.png',
             'soda-photon_v1.3b.zip-top.png','kitchen-photon_v1.3b.zip-portions.png',
             'soda-ComplementaryReimagined_r5.7.1.zip-top.png','soda-vanilla-top.png'):
    source = root / 'run/client/screenshots' / name
    assert source.stat().st_size > 1000, name
    shutil.copyfile(source, evidence / name)
for log, lines in report['runtime'].items():
    name = 'jei-server-results.txt' if log == 'server' else log.replace('.log', '-results.txt')
    (evidence / name).write_text('\n'.join(lines) + '\n', encoding='utf8')
dist = root / 'dist'
dist.mkdir(exist_ok=True)
jar = root / f'build/libs/hearth-horizons-compat-{version}.jar'
shutil.copyfile(jar, dist / jar.name)
for name in ('README.md', 'VALIDATION.md', 'DEPENDENCIES.md', 'LICENSE'):
    shutil.copyfile(root / name, dist / name)
with zipfile.ZipFile(dist / f'hearth-horizons-compat-{version}-sources.zip', 'w', zipfile.ZIP_DEFLATED) as archive:
    for file in sorted(root.rglob('*')):
        relative = file.relative_to(root)
        if any(part in {'.git', '.gradle', 'build', 'dist', 'run', '.idea', '__pycache__', 'migration_backups'} for part in relative.parts):
            continue
        if file.is_file():
            archive.write(file, relative.as_posix())
subprocess.run([sys.executable,str(root/'tools/package_sources_jar.py')],check=True)
print('Release:', dist / jar.name)
print('SHA256:', report['sha256'])
print('Validated: 15 server GameTests; all registered raw JEI list/R-U/slots; native transfer and six bookmarks after restart; nine soda colours/bubbles with Photon, Complementary and vanilla')
