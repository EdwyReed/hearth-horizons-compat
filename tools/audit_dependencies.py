"""Read installed metadata and the App's public project/file cache (read-only).
No account/session rows or credentials are queried. Never modify the App DB.
"""
from pathlib import Path
from collections import defaultdict
import hashlib,json,sqlite3,tomllib,zipfile,re
ROOT=Path(__file__).resolve().parents[1]
PROFILE=Path(r'C:\Users\petyl\AppData\Roaming\ModrinthApp\profiles\NeoForge 1.21.1')
APPDB=PROFILE.parents[1]/'app.db'
namespaces=set(json.loads((ROOT/'qa-recipe-mods.json').read_text())['referenced_namespaces'])
namespaces-={'minecraft','c','forge','fabric','neoforge','hearth_horizons','hearth_horizons_compat','jei','streamsreflowing'}
namespaces|={'cluttered','moonlight','hexerei','create','farmersdelight','farm_and_charm','pamhc2foodextended','brewinandchewin'}
mods=[];owners=defaultdict(list)
db=sqlite3.connect('file:'+str(APPDB)+'?mode=ro',uri=True)
def cached(kind,key):
    row=db.execute('SELECT json(data) FROM cache WHERE data_type=? AND (id=? OR alias=?) LIMIT 1',(kind,key,key)).fetchone()
    return json.loads(row[0]) if row and row[0] else None
for jar in sorted((PROFILE/'mods').glob('*.jar')):
    if jar.name.startswith('hearth-horizons-compat-'):continue
    with zipfile.ZipFile(jar) as z:
        meta=None
        for path in ['META-INF/neoforge.mods.toml','META-INF/mods.toml']:
            if path in z.namelist():meta=tomllib.loads(z.read(path).decode('utf8'));break
        if not meta:continue
        provided={name.split('/')[1] for name in z.namelist() if name.startswith(('assets/','data/')) and name.count('/')>=2}
        ids=[m['modId'] for m in meta.get('mods',[])]
        file=cached('file',hashlib.sha1(jar.read_bytes()).hexdigest()) or {}
        project=cached('project',file.get('project_id','')) or {}
        row={'jar':jar.name,'mod_ids':ids,'versions':{m['modId']:m.get('version','') for m in meta.get('mods',[])},
             'project_id':file.get('project_id'),'version_id':file.get('version_id'),'slug':project.get('slug'),
             'name':project.get('title',ids[0] if ids else jar.name),'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),
             'namespaces':sorted(provided)}
        mods.append(row)
        for ns in provided:owners[ns].append(row)
chosen={};unresolved=[]
for ns in sorted(namespaces):
    exact=[m for m in mods if ns in m['mod_ids']]
    candidates=exact or owners[ns]
    if len(candidates)!=1:
        # Foreign recipe overrides often mention other namespaces. The canonical
        # JAR also owns that namespace's item textures, so prefer its assets.
        direct=[m for m in candidates if ns.replace('_','') in m['jar'].lower().replace('_','').replace('-','')]
        if len(direct)==1:candidates=direct
    if len(candidates)==1:
        m=candidates[0];chosen.setdefault(m['jar'],dict(m,required_namespaces=[]))['required_namespaces'].append(ns)
    else:unresolved.append({'namespace':ns,'candidates':[m['jar'] for m in candidates]})
report={'scope':'Current culinary pack references and mandatory code/mixin targets; project links read from native App cache',
        'required_providers':list(chosen.values()),'optional_providers':['jei','streamsreflowing'],'unresolved':unresolved}
out=ROOT/'validation/cooking-audit/dependencies.json';out.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print('Required providers',len(chosen),'with Modrinth file links',sum(bool(m['project_id']) for m in chosen.values()))
print('Unresolved',unresolved)
for row in chosen.values():print(','.join(row['mod_ids']),row['project_id'] or 'NO_APP_LINK',row['slug'] or '',row['jar'])
