"""0.4 release gate: exact artifact plus independently produced native evidence."""
from pathlib import Path
import argparse, hashlib, json, re, tomllib, zipfile
ROOT=Path(__file__).resolve().parents[1]
VERSION='0.4.0'
JAR=ROOT/f'build/libs/hearth-horizons-compat-{VERSION}.jar'
parser=argparse.ArgumentParser();parser.add_argument('--require-runtime',action='store_true');args=parser.parse_args()
manifest=json.loads((ROOT/f'migration/release-{VERSION}.json').read_text())
previous=json.loads((ROOT/'migration/release-0.3.0.json').read_text())
definitions=json.loads((ROOT/'src/main/resources/culinary-preparations.json').read_text())
digest=lambda b:hashlib.sha256(b).hexdigest()
with zipfile.ZipFile(JAR) as archive:
    assert archive.testzip() is None
    files=[e.filename for e in archive.infolist() if not e.is_dir()]
    assert not any('/gametest/' in n or '/JeiQa' in n or '/SodaQa' in n or n.startswith('qa/') or n.endswith('/ClientQa.class') or '/structure/' in n for n in files)
    packed={n.removeprefix('packs/culinary/') for n in files if n.startswith('packs/culinary/')}
    assert packed==set(manifest['files'])
    for path,expected in manifest['files'].items():
        contents=archive.read('packs/culinary/'+path)
        assert digest(contents)==expected,path
        assert (ROOT/'src/main/resources/packs/culinary'/path).read_bytes()==contents,path
    assert set(manifest['removed_legacy_resources'])==set(previous['removed_legacy_resources'])
    assert not any('/preparation_migration/' in n or 'LegacyPreparation' in n for n in files)
    approved=[n for n in previous['files'] if n.endswith('.png') or n.startswith('assets/hearth_horizons_compat/models/item/')]
    assert len(approved)==6
    for n in approved:assert digest(archive.read('packs/culinary/'+n))==previous['files'][n],n
    classes=[n for n in files if n.endswith('.class')]
    for n in classes:
        contents=archive.read(n)
        assert int.from_bytes(contents[6:8],'big')==65,n
        assert contents==(ROOT/'build/classes/java/main'/n).read_bytes(),'Stale class: '+n
    for resource in (ROOT/'src/main/resources').rglob('*'):
        if not resource.is_file():continue
        n=resource.relative_to(ROOT/'src/main/resources').as_posix()
        if n.startswith(('qa/','data/hearth_horizons_compat/structure/')):continue
        assert archive.read(n)==resource.read_bytes(),'Stale resource: '+n
    metadata=tomllib.loads(archive.read('META-INF/neoforge.mods.toml').decode())
    assert metadata['mods'][0]['version']==VERSION
    required={r['modId'] for r in metadata['dependencies']['hearth_horizons_compat'] if r['type']=='required'}
    deps=json.loads((ROOT/'validation/cooking-audit/dependencies.json').read_text())
    assert not deps['unresolved']
    for provider in deps['required_providers']:assert set(provider['mod_ids']) & required,provider['jar']
    assert 'jei' not in required
    for shelf in ('small_shelf','small_shelf_pink'):
        model=json.loads(archive.read(f'assets/cluttered/models/block/{shelf}.json'))
        assert min(e['from'][1] for e in model['elements'])==0
        assert max(e['to'][1] for e in model['elements'])==5
    assert len({d['raw_path'] for d in definitions})==len(definitions)
    for d in definitions:
        ns,path=d['source_recipe'].split(':',1)
        cold=json.loads(archive.read(f'packs/culinary/data/{ns}/recipe/{path}.json'))
        assert cold['type']=='hearth_horizons_compat:cold_preparation'
        assert cold['result']=={'id':'hearth_horizons_compat:'+d['raw_path'],'count':1}
        hot=json.loads(archive.read(f'packs/culinary/data/hearth_horizons_compat/recipe/cooking/{ns}/{path}.json'))
        assert hot['result']=={'id':d['result'],'count':d['count']}
        assert hot['type'] not in ('minecraft:campfire_cooking','minecraft:smelting')
runtime={}
if args.require_runtime:
    def log(name,markers):
        content=(ROOT/'build'/name).read_text(encoding='utf8',errors='replace')
        assert 'BUILD SUCCESSFUL' in content,name
        assert not any(m in content for m in ('JEI_QA_FAILURE','required tests failed','SODA_QA_FAILURE')),name
        for marker in markers:assert marker in content,(name,marker)
        runtime[name]=[s for s in content.splitlines() if any(m in s for m in ('_QA','required tests','COOKING_AUDIT_RUNTIME','BUILD SUCCESSFUL'))]
        return content
    cold_count=len(json.loads((ROOT/'validation/cooking-audit/cold-meat-changes.json').read_text()))
    server=log('integrity-server.log',['All 15 required tests passed',f'INTEGRITY_QA all {len(definitions)}',f'COLD_MEAT_QA {cold_count}','OVEN_SERVING_QA','CONTAINER_QA','KITCHEN_QA','SODA_QA','SKILLET_QA','CULINARY_QA_RECIPES 3'])
    for line in server.splitlines():
        assert "Couldn't load tag hearth_horizons:" not in line,line
        if 'Parsing error loading recipe ' in line:
            rid=line.split('Parsing error loading recipe ',1)[1].split()[0];ns,path=rid.split(':',1)
            assert f'data/{ns}/recipe/{path}.json' not in manifest['files'],line
    for name,restart in [('jei-client-console.log',False),('jei-client-restart-console.log',True)]:
        client=log(name,['JEI_QA_COMPLETE',f'JEI_QA_EXPANDED {len(definitions)}','JEI_QA_TRANSFER stove: raw transferred; cooked alternative rejected','JEI_QA_BOOKMARKS 6 distinct raw/cooked variants','restored after process restart' if restart else 'saved through native JEI bookmark config'])
        assert client.count('JEI_QA_FOCUS ')==3,name
    for name,shader in [('soda-photon-final.log','photon_v1.3b.zip'),('soda-complementary-final.log','ComplementaryReimagined_r5.7.1.zip'),('soda-vanilla-final.log','vanilla')]:
        content=log(name,[f'SODA_QA_COMPLETE shader={shader} clientFlavours=9','SODA_QA_SYNC all nine'])
        if shader!='vanilla':assert f'SODA_QA_SHADER active={shader}' in content
        for suffix in ('180','200','top'):assert (ROOT/f'run/client/screenshots/soda-{shader}-{suffix}.png').stat().st_size>1000
    log('kitchen-photon-visual.log',['KITCHEN_QA_SCREENSHOT','SODA_QA_SHADER active=photon_v1.3b.zip'])
    visual=json.loads((ROOT/f'validation/2026-10-16/visual-review-{VERSION}.json').read_text())
    for row in visual['screenshots']:assert digest((ROOT/row['path']).read_bytes())==row['sha256'],row['path']
    assert set(visual['render_modes'])=={'photon_v1.3b.zip','ComplementaryReimagined_r5.7.1.zip','vanilla'}
result={'version':VERSION,'sha256':digest(JAR.read_bytes()),'class_count':len(classes),'embedded_files':len(manifest['files']),
        'recipes':sum('/recipe/' in n for n in packed),'tags':sum('/tags/' in n for n in packed),'advancements':sum('/advancement/' in n for n in packed),
        'raw_batches':len(definitions),'approved_raw_assets_preserved':6,'required_providers':len(deps['required_providers']),
        'runtime_verified':bool(runtime),'runtime':runtime}
evidence=ROOT/'validation/2026-10-16';evidence.mkdir(parents=True,exist_ok=True)
(evidence/f'artifact-{VERSION}.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(f'Verified {JAR.name}: {len(packed)} embedded files, {len(classes)} Java21 classes, {len(definitions)} raw batches; exact source equality and six approved assets')
print('Runtime evidence:',bool(runtime));print('SHA256:',result['sha256'])
