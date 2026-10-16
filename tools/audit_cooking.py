"""Inventory EVERY installed recipe resource, retaining duplicate/conditional provenance.

Static findings are candidates, not proof of active bugs. Runtime registry/recipe
export is the second stage, so generated recipes and resolved tags are not guessed.
"""
from pathlib import Path
from collections import Counter, defaultdict
import hashlib, json, re, zipfile

ROOT = Path(__file__).resolve().parents[1]
PROFILE = Path(r'C:\Users\petyl\AppData\Roaming\ModrinthApp\profiles\NeoForge 1.21.1')
OUT = ROOT / 'validation/cooking-audit'
OUT.mkdir(parents=True, exist_ok=True)

def parse(data):
    # Minecraft accepts lenient Gson JSON. Preserve errors rather than silently omit.
    text = data.decode('utf-8-sig')
    try:
        return json.loads(text), False
    except ValueError:
        text = re.sub(r'/\*.*?\*/|(?m:^\s*//[^\n]*)', '', text, flags=re.S)
        text = re.sub(r',\s*([}\]])', r'\1', text)
        return json.loads(text), True

def ids(node):
    if isinstance(node, str) and ':' in node:
        yield node
    elif isinstance(node, dict):
        for key, value in node.items():
            if key in ('item', 'id', 'fluid', 'tag') and isinstance(value, str):
                yield ('#' if key == 'tag' else '') + value
            elif isinstance(value, (dict, list)):
                yield from ids(value)
    elif isinstance(node, list):
        for value in node:
            yield from ids(value)

records, errors, jars = [], [], []
def add(source, path, data, override=False):
    match = re.fullmatch(r'data/([^/]+)/recipe/(.+)\.json', path)
    if not match:
        return
    rid = match[1] + ':' + match[2]
    try:
        recipe, lenient = parse(data)
    except (ValueError, UnicodeError) as exc:
        errors.append({'id': rid, 'source': source, 'error': str(exc)})
        return
    results = recipe.get('results', recipe.get('result', recipe.get('output', {})))
    ingredients = {k: v for k, v in recipe.items() if k in ('ingredients', 'ingredient', 'key', 'base', 'addition', 'template', 'fluid')}
    outputs, inputs = sorted(set(ids(results))), sorted(set(ids(ingredients)))
    records.append({'id': rid, 'source': source, 'override': override, 'type': recipe.get('type'),
                    'conditions': recipe.get('neoforge:conditions', []), 'lenient': lenient,
                    'inputs': inputs, 'outputs': outputs, 'recipe': recipe})

for jar in sorted((PROFILE / 'mods').glob('*.jar')):
    if jar.name.startswith('hearth-horizons-compat-'):
        continue  # Current source below supersedes the installed previous release.
    jars.append({'file': jar.name, 'sha256': hashlib.sha256(jar.read_bytes()).hexdigest()})
    with zipfile.ZipFile(jar) as z:
        for name in z.namelist():
            if '/recipe/' in name and name.endswith('.json'):
                add(jar.name, name, z.read(name))
pack = ROOT / 'src/main/resources/packs/culinary'
for file in sorted((pack / 'data').rglob('*.json')):
    add('compat-source', file.relative_to(pack).as_posix(), file.read_bytes(), True)

groups = defaultdict(list)
for record in records:
    groups[record['id']].append(record)
effective = {rid: next((r for r in reversed(rs) if r['override']), rs[-1]) for rid, rs in groups.items()}
candidates = []
hot = re.compile(r'(?:baked|roast|fried|cooked|stirfry|stir_fry|pancake|waffle|pizza|breaditem|cookie|muffin|croissant|sodaitem)')
containers = re.compile(r'(?:bottle|bucket|yoghurt|oatmeal|soy_sauce|soysauce|bowl)')
for record in effective.values():
    recipe = record['recipe']
    if record['conditions'] == [{'type': 'neoforge:false'}]:
        continue
    if record['type'] in ('minecraft:crafting_shapeless', 'minecraft:crafting_shaped') and any(hot.search(o) for o in record['outputs']):
        candidates.append({'id': record['id'], 'reason': 'manual-preparation-or-heat-bypass-review', 'outputs': record['outputs'], 'inputs': record['inputs']})
    if any(containers.search(i) for i in record['inputs']):
        candidates.append({'id': record['id'], 'reason': 'container-ledger-review', 'type': record['type'], 'inputs': record['inputs'], 'outputs': record['outputs']})
    if record['type'] == 'minecraft:campfire_cooking' and any('preparation' in i for i in record['inputs']):
        candidates.append({'id': record['id'], 'reason': 'skillet-only-restriction-required'})

summary = {'scope': 'all installed top-level JAR recipe resources + current compat source; no unrelated namespace excluded',
           'limitations': ['JAR ordering is provisional where duplicate IDs lack compat override; live resource manager is authoritative',
                           'Conditions and dynamic recipes require runtime export; heuristic flags require semantic review'],
           'jars': len(jars), 'resources': len(records), 'distinct_ids': len(groups),
           'duplicates': sum(len(rs) > 1 for rs in groups.values()), 'parse_errors': len(errors),
           'types': dict(Counter(r['type'] for r in effective.values())),
           'candidate_counts': dict(Counter(c['reason'] for c in candidates))}
for name, value in [('summary', summary), ('sources', jars), ('resources', records), ('parse-errors', errors), ('candidates', candidates)]:
    (OUT / (name + '.json')).write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps(summary, ensure_ascii=False, indent=2))
