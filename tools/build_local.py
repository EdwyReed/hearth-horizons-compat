"""Build against the locked installed profile through ModDevGradle.

Create and NeoForge access transformers require the transformed development
classpath. A raw javac compile against the client distribution is insufficient.
No profile writes; dependencies and build outputs stay in Gradle/repository caches.
"""
from pathlib import Path
import argparse, hashlib, json, os, subprocess
root=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser()
p.add_argument('--profile',type=Path,default=os.environ.get('HH_MINECRAFT_PROFILE'))
p.add_argument('--jdk',type=Path,default=None,help='Optional Gradle JVM; Java 21 toolchain is still selected by the build')
a=p.parse_args()
if a.profile is None:p.error('Specify --profile or HH_MINECRAFT_PROFILE')
for name,expected in json.loads((root/'dependency-lock.json').read_text()).items():
    assert hashlib.sha256((a.profile/'mods'/name).read_bytes()).hexdigest()==expected, 'Changed dependency: '+name
env=os.environ.copy()
if a.jdk:env['JAVA_HOME']=str(a.jdk)
wrapper=root/('gradlew.bat' if os.name=='nt' else 'gradlew')
subprocess.run([str(wrapper),'build','-PminecraftProfile='+str(a.profile.resolve()),'--console=plain'],cwd=root,env=env,check=True)
