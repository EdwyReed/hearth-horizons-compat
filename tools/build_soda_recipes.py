"""Build a balanced nine-flavour mixer -> liquid -> serving chain.

One original batch uses 250 mB water and the original sugar/fruit ingredients.
Glass is paid at serving, returned on drinking, and returned on emptying.
"""
from pathlib import Path
import json, zipfile
ROOT = Path(__file__).resolve().parents[1]
PACK = ROOT / 'src/main/resources/packs/culinary'
PROFILE = Path(r'C:\Users\petyl\AppData\Roaming\ModrinthApp\profiles\NeoForge 1.21.1')
JAR = next((PROFILE / 'mods').glob('pamhc2foodextended-*.jar'))
colours = {'cherry': 'Вишнёвая газировка', 'cola': 'Кола', 'ginger': 'Имбирная газировка',
           'grapefruit': 'Грейпфрутовая газировка', 'grape': 'Виноградная газировка',
           'lemonlime': 'Лимонно-лаймовая газировка', 'orange': 'Апельсиновая газировка',
           'rootbeer': 'Рутбир', 'strawberry': 'Клубничная газировка'}
def write(name, obj):
    path = PACK / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
with zipfile.ZipFile(JAR) as z:
    for flavour in colours:
        oldpath = f'data/pamhc2foodextended/recipe/{flavour}sodaitem.json'
        original = json.loads(z.read(oldpath))
        ingredients = [i for i in original['ingredients'] if i.get('tag') not in ('c:tool_pot', 'c:water')]
        fluid = 'hearth_horizons_compat:' + flavour + '_soda'
        drink = 'pamhc2foodextended:' + flavour + 'sodaitem'
        write(oldpath, {'neoforge:conditions': [{'type': 'neoforge:false'}]})
        write(f'data/hearth_horizons_compat/recipe/soda/mixing/{flavour}.json', {
            'type': 'create:mixing', 'ingredients': ingredients + [{'type': 'neoforge:tag', 'tag': 'hearth_horizons:liquids/clean_water', 'amount': 250}],
            'results': [{'id': fluid, 'amount': 250}], 'processing_time': 200})
        write(f'data/hearth_horizons_compat/recipe/soda/filling/{flavour}.json', {
            'type': 'create:filling', 'ingredients': [{'item': 'minecraft:glass_bottle'}, {'type': 'neoforge:single', 'fluid': fluid, 'amount': 250}],
            'results': [{'id': drink}]})
        write(f'data/hearth_horizons_compat/recipe/soda/emptying/{flavour}.json', {
            'type': 'create:emptying', 'ingredients': [{'item': drink}],
            'results': [{'id': fluid, 'amount': 250}, {'id': 'minecraft:glass_bottle'}]})
for lang in ['ru_ru', 'en_us']:
    path = PACK / f'assets/hearth_horizons_compat/lang/{lang}.json'
    data = json.loads(path.read_text(encoding='utf-8'))
    data.update({'fluid.hearth_horizons_compat.' + name + '_soda': russian if lang == 'ru_ru' else name.replace('lemonlime','Lemon-lime').replace('rootbeer','Root beer').capitalize() + ' soda' for name, russian in colours.items()})
    write(path.relative_to(PACK), data)
print('Nine mixer recipes, nine filling and nine emptying recipes; nine manual bypasses disabled.')
