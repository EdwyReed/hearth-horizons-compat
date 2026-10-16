"""Fix remaining cold crafting vessel ledgers without adding a heating stage.
Uses native active recipes + actual FOOD/remainder components from runtime.
"""
from pathlib import Path
import json,shutil
ROOT=Path(__file__).resolve().parents[1]
AUDIT=ROOT/'validation/cooking-audit'
PACK=ROOT/'src/main/resources/packs/culinary'
snapshot=AUDIT/'pre-container-runtime.json'
if not snapshot.exists():shutil.copyfile(AUDIT/'runtime.json',snapshot)
runtime=json.loads(snapshot.read_text(encoding='utf8'))
items={r['id']:r for r in runtime['items']}
hot={r['source_recipe'] for r in json.loads((ROOT/'src/main/resources/culinary-preparations.json').read_text(encoding='utf8'))}
changes=[]
for row in runtime['recipes']:
    if row['id'] in hot or row['type']!='minecraft:crafting':continue
    recipe=row.get('encoded',{})
    if recipe.get('type') not in ['minecraft:crafting_shapeless','minecraft:crafting_shaped']:continue
    if not row['food'] and row['id']!='cluttered:seltzer_cans':continue
    if not any(items.get(i,{}).get('eating_container') for slot in row['ingredients'] for i in slot):continue
    original=json.loads(json.dumps(recipe))
    if recipe['type']=='minecraft:crafting_shaped':
        recipe['ingredients']=[recipe['key'][c] for line in recipe.pop('pattern') for c in line if c!=' '];recipe.pop('key')
    recipe['type']='hearth_horizons_compat:cold_preparation'
    ns,path=row['id'].split(':',1)
    target=PACK/f'data/{ns}/recipe/{path}.json';target.parent.mkdir(parents=True,exist_ok=True)
    target.write_text(json.dumps(recipe,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    changes.append({'id':row['id'],'original':original,'patched':recipe,'reason':'Return ingredient eating vessels once; retain vessels already carried into final serving'})
(AUDIT/'container-changes.json').write_text(json.dumps(changes,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print('Cold crafting container ledgers patched:',len(changes))
