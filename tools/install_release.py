"""Install the fully verified current release without reloading or stopping Minecraft."""
from pathlib import Path
from datetime import datetime
import hashlib, json, re, shutil, subprocess, sys

root = Path(__file__).resolve().parents[1]
version = re.search(r"^version = '([^']+)'", (root / 'build.gradle').read_text(), re.M)[1]
subprocess.run([sys.executable, str(root / 'tools/verify_release.py'), '--require-runtime'], check=True)
profile = Path(r'C:\Users\petyl\AppData\Roaming\ModrinthApp\profiles\NeoForge 1.21.1')
mods = (profile / 'mods').resolve()
jar = root / f'build/libs/hearth-horizons-compat-{version}.jar'
digest = hashlib.sha256(jar.read_bytes()).hexdigest()
destination = mods / jar.name
previous = list(mods.glob('hearth-horizons-compat-*.jar'))
assert len(previous) == 1, 'Review the installed version before replacing it'
old = previous[0]
assert old.resolve().parent == mods
assert old.name == 'hearth-horizons-compat-0.3.0.jar', 'This migration expects the verified 0.3.0 installation'
previous_report = json.loads((root / 'migration/activation-0.3.0.json').read_text())
assert hashlib.sha256(old.read_bytes()).hexdigest() == previous_report['sha256'], 'Installed artifact changed'
backup = root / 'migration_backups' / (datetime.now().strftime('%Y%m%d-%H%M%S') + '-cooking-0.4.0')
backup.mkdir(parents=True)
shutil.copyfile(old, backup / old.name)
staging = mods / (jar.name + '.staging')
shutil.copyfile(jar, staging)
assert hashlib.sha256(staging.read_bytes()).hexdigest() == digest
try:
    old.unlink()
    staging.replace(destination)
except Exception:
    if not old.exists():
        shutil.copyfile(backup / old.name, old)
    if staging.exists():
        staging.unlink()
    raise
assert hashlib.sha256(destination.read_bytes()).hexdigest() == digest
assert list(mods.glob('hearth-horizons-compat-*.jar')) == [destination]
assert not (profile / 'moonlight-global-datapacks/hearth_horizons_compat').exists()
report = {'version': version, 'date': datetime.now().isoformat(), 'installed': str(destination),
          'sha256': digest, 'previous_artifact_backup': str(backup / old.name),
          'main_game_reloaded_or_restarted': False, 'main_worlds_or_options_edited': False}
(root / f'migration/activation-{version}.json').write_text(json.dumps(report, indent=2) + '\n')
print(json.dumps(report, indent=2))
