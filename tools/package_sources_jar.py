"""Wrap the complete corresponding-source ZIP in Modrinth's sources-JAR format.
Only writes the source artifact; never touches the already selected primary JAR.
"""
from pathlib import Path
import re,zipfile
ROOT=Path(__file__).resolve().parents[1]
version=re.search(r"^version = '([^']+)'",(ROOT/'build.gradle').read_text(),re.M)[1]
source=ROOT/f'dist/hearth-horizons-compat-{version}-sources.zip'
target=source.with_suffix('.jar')
with zipfile.ZipFile(source) as original,zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED) as output:
    output.writestr('META-INF/MANIFEST.MF',f'Manifest-Version: 1.0\r\nImplementation-Title: Hearth Horizons Compatibility Sources\r\nImplementation-Version: {version}\r\n\r\n')
    for name in sorted(original.namelist()):output.writestr(name,original.read(name))
    helper='tools/package_sources_jar.py'
    if helper not in original.namelist():output.writestr(helper,(ROOT/helper).read_bytes())
with zipfile.ZipFile(target) as check:assert check.testzip() is None
print(target)
