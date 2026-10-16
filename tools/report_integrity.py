"""Summarize audit coverage without turning heuristic candidates into defects."""
from pathlib import Path
from collections import Counter
import json
ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'validation/cooking-audit'
read=lambda name:json.loads((OUT/name).read_text(encoding='utf8'))
runtime=read('runtime.json');static=read('summary.json')
definitions=json.loads((ROOT/'src/main/resources/culinary-preparations.json').read_text())
recipes=runtime['recipes'];manual=[r for r in recipes if r['type']=='minecraft:crafting' and r['food']]
report={
 'date':'2026-10-16','static_scope':static['scope'],'installed_jars':static['jars'],
 'static_resources':static['resources'],'distinct_static_ids':static['distinct_ids'],
 'runtime_scope':json.loads((ROOT/'qa-recipe-mods.json').read_text())['scope'],
 'runtime_jars':len(json.loads((ROOT/'qa-recipe-mods.json').read_text())['files']),
 'active_runtime_recipes':len(recipes),'runtime_serializers':dict(sorted(Counter(r['serializer'] for r in recipes).items())),
 'staged_recipes':len(definitions),'stages':dict(Counter(r['stage'] for r in definitions)),
 'cold_meat_changes':len(read('cold-meat-changes.json')),'explicit_raw_exceptions':read('intentional-raw-exceptions.json'),
 'cold_container_wrappers':len(read('container-changes.json')),
 'remaining_manual_food_recipes':len(manual),
 'remaining_manual_by_namespace':dict(Counter(r['id'].split(':')[0] for r in manual)),
 'review_decisions':[
  'Authored hot-tool dishes and reviewed dough/raw-grain/raw-egg dishes receive a separate heating stage; original ingredient sets and yields remain unchanged.',
  'Yoghurt, kimchi, soy yoghurt and miso use fermentation consistently across reviewed Pam/MineColonies variants; manual Croptopia beer, mead and wine also ferment.',
  'Cold salads, sandwiches, mixed drinks, food decoration, cooked-meal plating, storage unpacking and slice recombination retain cold crafting.',
  'Intentional raw fish dishes, tartare, ground/raw ingredients and the upstream edible-soap joke are explicit exceptions.',
  'Caupona native dynamic boiling/frying/cooking/dolium recipes stay in their own machine system; getResultItem being empty is not an absent recipe.',
  'Cooking for Blockheads toaster and Slice and Dice generated cutting recipes stay native; no automatic heating bypass is added.',
  '130 Slice and Dice generated multi-output cutting recipes decode and are active, but their upstream encoder rejects more than one output. This limits JSON re-encoding, not proof of failed cutting.',
  'Strict JSON parser errors are recorded separately; authoritative NeoForge/Gson decoding determines validity.'
 ],
 'limitations':[
  'Static resource coverage includes all installed top-level JARs. Runtime coverage uses the isolated culinary/dependency set, not all 173 gameplay mods.',
  'Preserving original ingredients does not force every upstream grain recipe through an added flour-and-water stage.',
  'Dynamic food/fluid serializers and machine-specific component/remainder semantics retain upstream behaviour except explicitly patched integrations.',
  'No universal chemistry simulation, colony nutrition overhaul, multiplayer acceptance or mass-render FPS benchmark is claimed.'
 ]}
(OUT/'review-summary.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(f"Audit: {static['jars']} installed JARs; {len(recipes)} active recipes; {len(definitions)} staged meals; {len(manual)} remaining cold/storage/raw crafting recipes")
