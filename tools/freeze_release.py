"""Snapshot reviewed embedded resources and the source-resource runtime baseline.

This does not validate gameplay or approve publication. verify_release.py still
requires independently produced native test/client evidence before packaging.
"""
from pathlib import Path
import hashlib, json, re

ROOT=Path(__file__).resolve().parents[1]
version=re.search(r"^version = '([^']+)'",(ROOT/'build.gradle').read_text(),re.M)[1]
pack=ROOT/'src/main/resources/packs/culinary'
files={p.relative_to(pack).as_posix():hashlib.sha256(p.read_bytes()).hexdigest()
       for p in sorted(pack.rglob('*')) if p.is_file()}
legacy=json.loads((ROOT/'migration/culinary-1.11.0.json').read_text())
removed=sorted(set(legacy['packaged_files'])-set(files))
manifest={'version':version,'files':files,'removed_legacy_resources':removed,
          'changed_legacy_resources':sorted(p for p,h in files.items() if p in legacy['packaged_files'] and h!=legacy['packaged_files'][p]),
          'added_resources':sorted(set(files)-set(legacy['packaged_files']))}
(ROOT/f'migration/release-{version}.json').write_text(json.dumps(manifest,indent=2)+'\n')
qa=ROOT/'src/main/resources/qa/culinary-baseline.json'
qa.write_text(json.dumps({p:h for p,h in files.items() if p.startswith('data/')},indent=2)+'\n')
print(f'Snapshotted {len(files)} embedded files; {len(manifest["changed_legacy_resources"])} reviewed legacy changes, {len(removed)} removed, {len(manifest["added_resources"])} added')
