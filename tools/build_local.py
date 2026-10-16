"""Compile against the exact installed, Mojang-named NeoForge distribution; no profile writes.

This reproducible bootstrap path avoids redownloading Minecraft during offline development.
The standard ModDevGradle build remains available for development runs and GameTests.
"""
from pathlib import Path
import argparse, hashlib, json, os, re, subprocess, zipfile

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--profile', type=Path, default=Path(os.environ.get('HH_MINECRAFT_PROFILE', r'C:\Users\petyl\AppData\Roaming\ModrinthApp\profiles\NeoForge 1.21.1')))
parser.add_argument('--jdk', type=Path, default=Path(r'C:\Program Files\Java\jdk-25'))
args = parser.parse_args()
for name, digest in json.loads((root / 'dependency-lock.json').read_text()).items():
    if hashlib.sha256((args.profile / 'mods' / name).read_bytes()).hexdigest() != digest:
        raise SystemExit(f'Dependency changed: {name}; review and update the lock first')
libraries = args.profile.parents[1] / 'meta' / 'libraries'
neo = libraries / 'net/neoforged/neoforge/21.1.228'
jars = [neo / 'neoforge-21.1.228-client.jar', neo / 'neoforge-21.1.228-universal.jar']
jars += sorted(libraries.rglob('*.jar'))
jars += [args.profile / 'mods' / n for n in json.loads((root / 'dependency-lock.json').read_text())]
classes = root / 'build/local/classes'
classes.mkdir(parents=True, exist_ok=True)
arguments = ['--release', '21', '-encoding', 'UTF-8', '-proc:none', '-classpath', os.pathsep.join(str(p) for p in jars), '-d', str(classes)]
arguments += [str(p) for p in sorted((root / 'src/main/java').rglob('*.java'))]
argfile = root / 'build/local/javac.args'
argfile.write_text('\n'.join('"' + s.replace('\\', '/') + '"' for s in arguments), encoding='utf8')
subprocess.run([str(args.jdk / 'bin/javac.exe'), '@' + str(argfile)], check=True)
version = re.search(r"^version = '([^']+)'", (root / 'build.gradle').read_text(), re.M)[1]
output = root / f'build/libs/hearth-horizons-compat-{version}.jar'
output.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as archive:
    for base in [classes, root / 'src/main/resources']:
        for p in sorted(base.rglob('*')):
            name = p.relative_to(base).as_posix()
            if '/gametest/' in name or '/JeiQa' in name or '/SodaQa' in name or name.endswith('/ClientQa.class') or '/structure/' in name or name.startswith('qa/'): continue
            if p.is_file(): archive.writestr(zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0)), p.read_bytes())
    archive.write(root / 'LICENSE', 'META-INF/LICENSE')
print(output)
print('SHA256', hashlib.sha256(output.read_bytes()).hexdigest())
