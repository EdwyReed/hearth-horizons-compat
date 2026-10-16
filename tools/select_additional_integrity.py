"""Reviewed additional heat/fermentation families beyond Pam's tool tags.
Uses authoritative Recipe.CODEC exports, not alphabetical JAR precedence.
"""
from pathlib import Path
import json,re,shutil
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'validation/cooking-audit'
snapshot=OUT/'pre-additional-runtime.json'
if not snapshot.exists():shutil.copyfile(OUT/'runtime.json',snapshot)
runtime=json.loads(snapshot.read_text(encoding='utf8'))
old_extra=OUT/'additional-stages.json'
extra_ids={r['id'] for r in json.loads(old_extra.read_text(encoding='utf8'))} if old_extra.exists() else set()
existing={r['source_recipe'] for r in json.loads((ROOT/'src/main/resources/culinary-preparations.json').read_text(encoding='utf8'))}-extra_ids
mine_oven={'apple_pie','plain_cheesecake','veggie_quiche','cheese_pizza','baked_salmon','mutton_dinner'}
mine_skillet={'fried_rice','mutton_kebab','spicy_grilled_chicken','tortillas','fish_n_chips'}
mine_pot={'cheese_ravioli','veggie_ravioli','meat_ravioli','pasta_tomato','pasta_plain','veggie_soup','pottage','pea_soup','eggdrop_soup','cooked_rice','congee','mint_tea','pepper_hummus','chicken_broth',
          'potato_soup','squash_soup','soy_pea_soup','corn_chowder','soy_corn_chowder','polenta','pierogi'}
reviewed_extra={'croptopia:shaped_beef_stew':'pot','croptopia:pork_and_beans':'pot','croptopia:enchilada':'oven',
                'croptopia:shaped_bacon':'skillet','oceansdelight:stuffed_cod':'oven',
                'pamhc2foodextended:hotwingsitem':'skillet','pamhc2foodextended:suaderoitem':'skillet',
                'pamhc2foodextended:sausageitem':'skillet','pamhc2foodextended:pinkelitem':'skillet',
                'pamhc2foodextended:chorizoitem':'skillet','pamhc2foodextended:imitationcrabsticksitem':'pot',
                'pamhc2foodextended:pepperoniitem':'skillet','pamhc2foodextended:lambkebabitem':'skillet',
                'rusticdelight:bell_pepper_soup':'pot','aquaculture:turtle_soup':'pot',
                'croptopia:shaped_nether_wart_stew':'pot','culturalrecipes:empanada':'oven',
                'pamhc2foodextended:meringueitem':'oven','pamhc2foodextended:dangoitem':'pot',
                'pamhc2foodextended:mochiitem':'pot','pamhc2foodextended:couscousitem':'pot',
                'pamhc2foodextended:misopasteitem':'ferment','minecolonies:soy_yogurt':'ferment',
                'pamhc2foodextended:bamboosteamedriceitem':'pot','pamhc2foodextended:manjuuitem':'pot',
                'pamhc2foodextended:grilledskewersitem':'skillet','pamhc2foodextended:bolognaitem':'pot',
                'pamhc2foodextended:gourmetbeefpattyitem':'skillet','pamhc2foodextended:gourmetporkpattyitem':'skillet',
                'pamhc2foodextended:gourmetmuttonpattyitem':'skillet',
                'pamhc2foodextended:cornflakesitem':'oven','pamhc2foodextended:crispyricepuffcerealitem':'oven',
                'croptopia:nougat':'pot','croptopia:shaped_trifle':'pot',
                'croptopia:mead':'ferment','croptopia:beer':'ferment','croptopia:wine':'ferment',
                'croptopia:coffee':'pot','croptopia:sausage':'pot','croptopia:dragon_egg_omelette':'skillet',
                'croptopia:candied_nuts':'pot','croptopia:almond_brittle':'pot','croptopia:candied_kumquats':'pot',
                'pamhc2foodextended:saltedcaramelitem':'pot'}
reviewed_oven={'minecraft:cookie','minecraft:pumpkin_pie','minecraft:cake','expandeddelight:sugar_cookie','expandeddelight:chocolate_cookie',
               'farmersdelight_extended:shapeless/chorus_cookie','farmersdelight_extended:shapeless/glow_berry_cookie',
               'rusticdelight:coffee_cookie','rusticdelight:syrup_cookie','rusticdelight:cherry_blossom_cookie',
               'croptopia:nutty_cookie','croptopia:banana_nut_bread','croptopia:corn_bread',
               'croptopia:shaped_fruit_cake','croptopia:nether_star_cake','croptopia:tres_leche_cake',
               'expandeddelight:sweet_roll','expandeddelight:snickerdoodle',
               'farmersdelight:pumpkin_pie_from_pie_crust','minecraft:bread','pamhc2crops:bread_grain',
               'croptopia:shaped_lemon_coconut_bar','croptopia:shaped_figgy_pudding','croptopia:pumpkin_bars',
               'croptopia:macaron','croptopia:shaped_sticky_toffee_pudding','croptopia:shaped_rhubarb_crisp',
               'croptopia:snicker_doodle','croptopia:shaped_eton_mess','croptopia:shaped_treacle_tart'}
selected=[]
for row in runtime['recipes']:
    if row['id'] in existing or row['type']!='minecraft:crafting':continue
    recipe=row.get('encoded',{})
    if recipe.get('type') not in ['minecraft:crafting_shapeless','minecraft:crafting_shaped']:continue
    stage=None;reason=None
    if row['id'].startswith('croptopia:') and row['food']:
        text=json.dumps(recipe)
        if 'croptopia:cooking_pot' in text: stage='pot';reason='Authored Croptopia cooking pot requires heat'
        elif 'croptopia:frying_pan' in text:
            stage='skillet';reason='Authored Croptopia frying pan requires heat'
            if re.search('pizza|pie$|scone|brownie|cinnamon_roll|quiche|pasty|wellington|roasted_|baked_crepes',row['result']):stage='oven'
            if re.search('goulash|steamed_clams|cabbage_roll',row['result']):stage='pot'
    if row['id'] in reviewed_oven:stage='oven';reason='Reviewed raw dough/grain/egg baking bypass'
    if row['id'] in reviewed_extra:stage=reviewed_extra[row['id']];reason='Reviewed ready meal assembled from uncooked meat/fish'
    if row['food'] and 'jerky' in row['result']:stage='dry';reason='Drying meat must take place on the drying rack, not instantly in a crafting grid'
    if row['id'].startswith('minecolonies:'):
        name=row['id'].split(':')[1]
        if name in mine_oven:stage='oven'
        elif name in mine_skillet:stage='skillet'
        elif name in mine_pot:stage='pot'
        elif name in ['yogurt','kimchi']:stage='ferment'
        if stage:reason='Reviewed MineColonies raw ingredient preparation needs cooking/fermentation'
    if stage:
        copy=dict(row);copy['stage']=stage;copy['reason']=reason;selected.append(copy)
(OUT/'additional-stages.json').write_text(json.dumps(selected,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
from collections import Counter
print('Reviewed additional stages:',len(selected),dict(Counter(r['stage'] for r in selected)))
