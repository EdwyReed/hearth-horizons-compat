"""Audit an untouched native Modrinth App export against the installed profile."""
import hashlib
import json
import re
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PROFILE = Path.home() / 'AppData/Roaming/ModrinthApp/profiles/NeoForge 1.21.1'
PACK = ROOT / 'dist/Hearth & Horizons 0.2.0.mrpack'


def digest(data, algorithm='sha512'):
    return hashlib.new(algorithm, data).hexdigest()


with zipfile.ZipFile(PACK) as archive:
    assert archive.testzip() is None
    names = archive.namelist()
    index = json.loads(archive.read('modrinth.index.json'))
    assert index['versionId'] == '0.2.0'
    assert index['dependencies'] == {'minecraft': '1.21.1', 'neoforge': '21.1.228'}
    remote = {entry['path']: entry for entry in index['files']}
    companion_path = 'mods/hearth-horizons-compat-0.4.0.jar'
    companion = remote[companion_path]
    assert companion['downloads'] == ['https://cdn.modrinth.com/data/S4Pt6hHo/versions/ciWsNX4l/hearth-horizons-compat-0.4.0.jar']
    assert companion['env'] == {'client': 'required', 'server': 'required'}
    assert 'overrides/' + companion_path not in names
    assert not any('mods.zip' in name or 'hearth-horizons-compat-0.3.' in name for name in names + list(remote))
    allowed = {'config', 'defaultconfigs', 'mods', 'resourcepacks', 'shaderpacks', 'datapacks'}
    assert all(name.split('/')[1] in allowed for name in names if name.startswith('overrides/'))
    checked = []
    for jar in PROFILE.joinpath('mods').glob('*.jar'):
        relative = 'mods/' + jar.name
        data = jar.read_bytes()
        if relative in remote:
            assert all(digest(data, alg) == value for alg, value in remote[relative]['hashes'].items()), relative
        else:
            assert archive.read('overrides/' + relative) == data, relative
        checked.append(relative)
    credential_fields = []
    pattern = re.compile(r'^\s*["\']?(token|accessToken|refreshToken|authToken|bearerToken|clientSecret|secret|password|passwordHash|apiKey|authorization|sessionToken)["\']?\s*[:=]\s*(.*?)\s*[,;]?\s*$', re.I)
    for name in names:
        if not name.startswith('overrides/config/'):
            continue
        for line in archive.read(name).decode('utf-8', errors='replace').splitlines():
            match = pattern.match(line)
            if match:
                value = match[2].strip(' ,;\"\'')
                if value.lower() not in {'', 'null', 'none', 'false', 'true', '0', 'changeme', 'your_api_key'}:
                    credential_fields.append({'file': name, 'key': match[1]})
    assert not credential_fields, credential_fields  # Never print credential values.
    report = {
        'native_app_export': True, 'zip_modified_after_export': False,
        'file': str(PACK), 'bytes': PACK.stat().st_size,
        'sha512': digest(PACK.read_bytes()), 'version': index['versionId'],
        'dependencies': index['dependencies'], 'remote_files': len(remote),
        'installed_mod_jars_verified': len(checked), 'companion': companion,
        'backup_archive_excluded': True, 'personal_roots_excluded': True,
        'nonempty_credential_fields': credential_fields,
        'upload_status': 'prepared; public submission withheld pending gameplay acceptance',
    }
    target = ROOT / 'migration/modpack-0.2.0.json'
    target.write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({key: report[key] for key in ('bytes', 'remote_files', 'installed_mod_jars_verified', 'backup_archive_excluded', 'personal_roots_excluded', 'nonempty_credential_fields')}, indent=2))
