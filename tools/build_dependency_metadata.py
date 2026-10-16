"""Declare every required culinary provider; retain reviewed API version ranges."""
from pathlib import Path
import json,re
ROOT=Path(__file__).resolve().parents[1]
report=json.loads((ROOT/'validation/cooking-audit/dependencies.json').read_text(encoding='utf8'))
assert not report['unresolved'],report['unresolved']
p=ROOT/'src/main/resources/META-INF/neoforge.mods.toml'
text=p.read_text(encoding='utf8').split('# Generated culinary provider dependencies')[0].rstrip()+'\n'
declared=set(re.findall(r'modId="([^"]+)"',text))
added=[]
for row in report['required_providers']:
    for mod in row['mod_ids']:
        if mod in declared:continue
        added.append(f'[[dependencies.hearth_horizons_compat]]\nmodId="{mod}"\ntype="required"\nversionRange="[0,)"\nordering="AFTER"\nside="BOTH"\n')
        declared.add(mod)
text+='\n# Generated culinary provider dependencies: this companion targets the complete Hearth & Horizons pack.\n'+'\n'.join(added)
p.write_text(text,encoding='utf8')
lines=['# Companion-mod dependencies','',
       'The companion targets the complete Hearth & Horizons culinary stack. Core API ranges',
       'are specified in neoforge.mods.toml; recipe-only providers are required by ID.',
       'JEI is optional; Streams Reflowing is an optional clean-water alternative.',
       'The exact installed JAR hashes and native App project/version links are recorded in',
       'validation/cooking-audit/dependencies.json. No account/session data is included.','',
       '| Mod | NeoForge ID | Modrinth project | Installed source |','| --- | --- | --- | --- |']
for r in report['required_providers']:
    project=f"[{r['slug'] or r['project_id']}](https://modrinth.com/project/{r['project_id']})" if r['project_id'] else 'No matching installed-file link in App cache; provide the installed JAR separately'
    lines.append(f"| {r['name']} | {', '.join(r['mod_ids'])} | {project} | {r['jar']} |")
(ROOT/'DEPENDENCIES.md').write_text('\n'.join(lines)+'\n',encoding='utf8')
print('Required provider JARs:',len(report['required_providers']),'new metadata declarations:',len(added))
