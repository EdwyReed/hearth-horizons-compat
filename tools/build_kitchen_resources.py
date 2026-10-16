"""Use native Cluttered textures, preserving real crafted contents in serving recipes."""
from pathlib import Path
import copy, json, zipfile
ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / 'src/main/resources/packs/culinary'
PROFILE = Path(r'C:\Users\petyl\AppData\Roaming\ModrinthApp\profiles\NeoForge 1.21.1')
def write(path, value):
    p = PACK / path
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
names = ['croissant', 'chocolate_croissant', 'cinnamon_bun', 'pancake_stack', 'sliced_bread', 'sweetheart_baking_set_ingredients']
for path in (PACK / 'data').rglob('*.json'):
    if '/recipe/' not in path.as_posix(): continue
    r = json.loads(path.read_text(encoding='utf-8'))
    if r.get('type') == 'minecraft:crafting_shapeless' and r.get('result', {}).get('id') in ['cluttered:' + n for n in names]:
        r['type'] = 'hearth_horizons_compat:serving'
        write(path.relative_to(PACK), r)
    # Fixed cutting returns four slices even when a platter contains only one.
    if r.get('type') == 'farmersdelight:cutting' and any(i.get('item') == 'cluttered:sliced_bread' for i in r.get('ingredients', [])):
        write(path.relative_to(PACK), {'neoforge:conditions': [{'type': 'neoforge:false'}]})
with zipfile.ZipFile(next((PROFILE / 'mods').glob('cluttered-*.jar'))) as z:
    for name in ['sliced_bread', 'sweetheart_baking_set_ingredients']:
        model = json.loads(z.read(f'assets/cluttered/models/block/{name}.json'))
        states = json.loads(z.read(f'assets/cluttered/blockstates/{name}.json'))
        variants = {}
        for count in range(5):
            m = copy.deepcopy(model)
            m.pop('groups', None)
            if name == 'sliced_bread':
                # The original model depicts a loaf and two loose slices. Replace
                # the loaf by exactly 1..4 visibly separate slices on its own tray.
                m['elements'] = [copy.deepcopy(model['elements'][0])]
                for i in range(count):
                    e = copy.deepcopy(model['elements'][2])
                    e['from'] = [3 + i*2.5, 1, 5.5]
                    e['to'] = [4 + i*2.5, 5, 10.5]
                    e['rotation'] = {'angle': 0, 'axis': 'y', 'origin': [8,1,8]}
                    m['elements'].append(e)
            else:
                # Removal order is egg, cocoa, sugar, flour; original ceramic bowl
                # persists until the last ingredient is taken, then becomes an item.
                indices = [0,1]
                if count >= 1: indices += [8,9]
                if count >= 2: indices += [10]
                if count >= 3: indices += [6,7]
                if count >= 4: indices += [2,3,4,5]
                m['elements'] = [model['elements'][i] for i in indices]
            path = f'assets/hearth_horizons_compat/models/block/kitchen/{name}_{count}.json'
            write(path,m)
            for selector, variant in states['variants'].items():
                v = copy.deepcopy(variant)
                v['model'] = f'hearth_horizons_compat:block/kitchen/{name}_{count}'
                variants[selector + f',hh_portions={count}'] = v
        write(f'assets/cluttered/blockstates/{name}.json', {'variants': variants})
print('Seven dynamic serving recipes; partial bread cutting bypass disabled; 10 partial kitchen models.')
