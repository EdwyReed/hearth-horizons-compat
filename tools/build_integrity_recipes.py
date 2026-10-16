"""Stage hot manual dishes using their authored cooking-tool requirements.

Selection uses actual runtime food outputs and original installed recipes. Cold
mixing/cutting/juicing is retained. Each selected source and original recipe is
recorded for review; original ingredients, tools and final yields are preserved.
"""
from pathlib import Path
import json, copy, shutil
ROOT=Path(__file__).resolve().parents[1]
PACK=ROOT/'src/main/resources/packs/culinary'
AUDIT=ROOT/'validation/cooking-audit'
for source,target in [('runtime.json','pre-integrity-runtime.json'),('resources.json','pre-integrity-resources.json')]:
    if not (AUDIT/target).exists(): shutil.copyfile(AUDIT/source,AUDIT/target)
runtime=json.loads((AUDIT/'pre-integrity-runtime.json').read_text(encoding='utf8'))
resources=json.loads((AUDIT/'pre-integrity-resources.json').read_text(encoding='utf8'))
sources={}
for row in resources:
    if row['id'] not in sources or row['override'] or not sources[row['id']]['override']:
        sources[row['id']]=row
tools={'#c:tool_bakeware':'oven','#c:tool_skillet':'skillet','#c:tool_pot':'pot','#c:tool_saucepan':'pot'}
explicit_stages={}
extra= AUDIT/'additional-stages.json'
if extra.exists():
    for row in json.loads(extra.read_text(encoding='utf8')):
        explicit_stages[row['id']]=row['stage']
        sources[row['id']]={'recipe':row['encoded'],'inputs':[],'source':row.get('source_pack','runtime-codec')}
        runtime['recipes']=[r for r in runtime['recipes'] if r['id']!=row['id']]+[row]
definitions=[]
def write(path,data):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
for active in sorted(runtime['recipes'],key=lambda r:r['id']):
    if (not active['food'] and active['id'] not in explicit_stages) or active['type']!='minecraft:crafting':continue
    row=sources.get(active['id'])
    if not row or row['recipe'].get('type') not in ['minecraft:crafting_shapeless','minecraft:crafting_shaped']:continue
    selected=[explicit_stages[active['id']]] if active['id'] in explicit_stages else [kind for tool,kind in tools.items() if tool in row['inputs']]
    if not selected:continue
    if len(set(selected))!=1:raise ValueError('Conflicting cooking tools: '+active['id'])
    stage=selected[0]
    if active['id'] in {'pamhc2foodcore:yogurtitem','pamhc2foodextended:kimchiitem'}:
        stage='ferment'  # Match the same foods' MineColonies fermentation route.
    namespace,name=active['id'].split(':',1)
    raw_path='uncooked/'+namespace+'/'+name
    raw='hearth_horizons_compat:'+raw_path
    original=copy.deepcopy(row['recipe'])
    if original['type']=='minecraft:crafting_shaped':
        original['ingredients']=[copy.deepcopy(original['key'][c]) for line in original.pop('pattern') for c in line if c!=' ']
        original.pop('key')
    original['type']='hearth_horizons_compat:cold_preparation'
    original['result']={'id':raw,'count':1}
    write(PACK/f'data/{namespace}/recipe/{name}.json',original)
    if stage=='oven':
        cooking={'type':'farm_and_charm:stove','ingredients':[{'item':raw}], 'result':{'id':active['result'],'count':active['count']},'experience':0,'requiresLearning':False}
    elif stage=='skillet':
        cooking={'type':'hearth_horizons_compat:skillet','ingredient':{'item':raw},'result':{'id':active['result'],'count':active['count']},'experience':0,'cookingtime':200,'category':'food'}
    elif stage=='ferment':
        cooking={'type':'brewinandchewin:fermenting','ingredients':[{'item':raw}],'result':{'id':active['result'],'count':active['count']},'experience':0,'fermenting_time':12000,'temperature':2,'category':'meals'}
    elif stage=='dry':
        cooking={'type':'farm_and_charm:drying','recipe_type':'MEAT','ingredient':{'item':raw},'result':{'id':active['result'],'count':active['count']}}
    else:
        cooking={'type':'farmersdelight:cooking','ingredients':[{'item':raw}],'result':{'id':active['result'],'count':active['count']},'experience':0,'cookingtime':200}
    write(PACK/f'data/hearth_horizons_compat/recipe/cooking/{namespace}/{name}.json',cooking)
    # Keep familiar source artwork; the explicit raw name and absence of FOOD
    # distinguish these batches. The original three approved raw icons are intact.
    result_ns,result_name=active['result'].split(':',1)
    write(PACK/f'assets/hearth_horizons_compat/models/item/{raw_path}.json',{'parent':f'{result_ns}:item/{result_name}'})
    definitions.append({'source_recipe':active['id'],'source_jar':row['source'],'original':row['recipe'],
                        'raw_path':raw_path,'result':active['result'],'count':active['count'],'stage':stage,
                        'expected_ingredients':active['ingredients']})
write(ROOT/'src/main/resources/culinary-preparations.json',definitions)
for lang in ['en_us','ru_ru']:
    p=PACK/f'assets/hearth_horizons_compat/lang/{lang}.json';data=json.loads(p.read_text(encoding='utf8'))
    data['item.hearth_horizons_compat.uncooked']='Заготовка: %s' if lang=='ru_ru' else 'Preparation: %s'
    write(p,data)
from collections import Counter
print('New independently registered raw batches:',len(definitions),dict(Counter(d['stage'] for d in definitions)))
