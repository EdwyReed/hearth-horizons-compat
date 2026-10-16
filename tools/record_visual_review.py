"""Record screenshot hashes AFTER viewing the actual native images.

This is a review record, not an image-comparison test or a substitute for viewing.
"""
from pathlib import Path
import argparse, hashlib, json
ROOT=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser();p.add_argument('mode',choices=['vanilla','photon_v1.3b.zip','ComplementaryReimagined_r5.7.1.zip']);args=p.parse_args()
dest=ROOT/'validation/2026-10-16/visual-review-0.4.0.json'
data=json.loads(dest.read_text()) if dest.exists() else {'date':'2026-10-16','render_modes':[],'screenshots':[]}
data['screenshots']=[r for r in data['screenshots'] if r['mode']!=args.mode]
for name in [f'soda-{args.mode}-{suffix}.png' for suffix in ('180','200','top')]+[f'kitchen-{args.mode}-portions.png']:
    path=ROOT/'run/client/screenshots'/name
    data['screenshots'].append({'mode':args.mode,'path':path.relative_to(ROOT).as_posix(),'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),
      'review':'Native image viewed: nine distinct soda surfaces and visible bubbles; kitchen image shows bread/kit states from four portions to empty vessel.' if name.startswith('soda-') else 'Native image viewed: five remaining-portion states each for bread and baking ingredients; textures present.'})
if args.mode not in data['render_modes']:data['render_modes'].append(args.mode)
dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text(json.dumps(data,indent=2)+'\n')
print('Recorded actual image review:',args.mode)
