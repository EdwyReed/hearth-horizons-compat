"""Cold serving requires already cooked meat; intentional raw dishes stay raw.
Cooked substitutions come from actual decoded native smelting/smoking recipes.
"""
from pathlib import Path
import json,copy,shutil,re
ROOT=Path(__file__).resolve().parents[1]
AUDIT=ROOT/'validation/cooking-audit';PACK=ROOT/'src/main/resources/packs/culinary'
snapshot=AUDIT/'pre-cold-meat-runtime.json'
if not snapshot.exists():shutil.copyfile(AUDIT/'runtime.json',snapshot)
runtime=json.loads(snapshot.read_text(encoding='utf8'))
hot={r['source_recipe'] for r in json.loads((ROOT/'src/main/resources/culinary-preparations.json').read_text(encoding='utf8'))}
known_raw={'minecraft:beef','minecraft:porkchop','minecraft:chicken','minecraft:rabbit','minecraft:mutton','minecraft:cod','minecraft:salmon','minecraft:potato'}
mapping={}
for row in runtime['recipes']:
    if row['type'] not in ['minecraft:smelting','minecraft:smoking'] or row['count']!=1 or not row['food']:continue
    for slot in row['ingredients']:
        for item in slot:
            if item in known_raw or re.search(r':raw|/raw_|:minced_',item):
                mapping.setdefault(item,set()).add(row['result'])
mapping={k:next(iter(v)) for k,v in mapping.items() if len(v)==1}
# Cold assembly needs cooked starch/egg too; raw fish in sushi does not imply
# raw rice. These canonical outputs are present in the native runtime export.
available={r['result'] for r in runtime['recipes']}
assert 'farmersdelight:cooked_rice' in available and 'farmersdelight:fried_egg' in available
for rice in ('farmersdelight:rice','pamhc2crops:riceitem','croptopia:rice','minecolonies:rice'):
    mapping[rice]='farmersdelight:cooked_rice'
mapping['minecraft:egg']='farmersdelight:fried_egg'
exceptions=[];changes=[]
def write(path,obj):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
for row in runtime['recipes']:
    if row['id'] in hot or not row['food'] or row['type']!='minecraft:crafting':continue
    original=row.get('encoded',{})
    if original.get('type') not in ['minecraft:crafting_shapeless','minecraft:crafting_shaped']:continue
    if not any(set(slot).intersection(mapping) for slot in row['ingredients']):continue
    intentional=bool(re.search('ground|raw|sushi|sashimi|poke|mettbrotchen|lox|californiaroll|tartare|soap|batter|dough|seed|mayonaise|mayonnaise|eggnog',row['result']))
    if intentional:
        exceptions.append({'id':row['id'],'result':row['result'],'reason':'Intentional raw dish, raw ingredient preparation or explicitly edible soap gag; no heating added'})
        # Keep raw meats and traditionally raw eggs; sushi/poke rice still cooks.
        local_mapping={k:v for k,v in mapping.items() if k.endswith((':rice',':riceitem')) and not re.search('raw|ground|seed|batter|dough',row['result'])}
    else:local_mapping=mapping
    recipe=copy.deepcopy(original)
    if recipe['type']=='minecraft:crafting_shaped':
        recipe['ingredients']=[recipe['key'][c] for line in recipe.pop('pattern') for c in line if c!=' '];recipe.pop('key')
    ns,path=row['id'].split(':',1)
    substitutions=[]
    for index,(ingredient,candidates) in enumerate(zip(recipe['ingredients'],[s for s in row['ingredients'] if s])):
        substitutions_here={item:local_mapping[item] for item in candidates if item in local_mapping}
        if not substitutions_here:continue
        allowed=sorted({local_mapping.get(item,item) for item in candidates})
        tag=f'cold_ingredients/{ns}/{path}/{index}'
        write(PACK/f'data/hearth_horizons_compat/tags/item/{tag}.json',{'replace':True,'values':allowed})
        recipe['ingredients'][index]={'tag':'hearth_horizons_compat:'+tag}
        substitutions.append({'slot':index,'mapping':substitutions_here,'allowed':allowed})
    if not substitutions:continue
    # Use the same vessel ledger as other cold assembly recipes.
    recipe['type']='hearth_horizons_compat:cold_preparation'
    write(PACK/f'data/{ns}/recipe/{path}.json',recipe)
    changes.append({'id':row['id'],'original':original,'substitutions':substitutions,'patched':recipe})
write(AUDIT/'cold-meat-changes.json',changes);write(AUDIT/'intentional-raw-exceptions.json',exceptions)
print('Cold serving raw->cooked substitutions:',len(changes),'intentional raw preparations/dishes retained:',len(exceptions))
