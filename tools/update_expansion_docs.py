"""Refresh the authored 1.7.0 roster reference from the same inputs as the powers."""
from pathlib import Path
import json, re
from expansion_content import RACES
ROOT = Path(__file__).resolve().parents[1]
NOTES = {
'colossan': ('Committed melee, door holding', 'Fast skirmishes and food-poor travel', 'Valen and Bovine', 'Bait the charged swing; fight around cover.'),
'auroran': ('Short, coordinated defensive exchanges', 'Sustained physical pressure', 'Valen, Moss One and Runic One', 'Spread pressure across time; the 4 HP ward is finite.'),
'grove_elf': ('Forest travel and deliberate ranged openings', 'Close combat and open terrain', 'Canine and High Elf', 'Break sight before launch or block the single empowered arrow.'),
'tide_elf': ('Water routes and ordinary underwater work', 'Long dry overland trips', 'Sea Serpen and Nymph', 'Fight away from water; a bottle is accessible upkeep, not immunity.'),
'astral_elf': ('Prepared retreats across visible open ground', 'Walls, distance and forced displacement', 'Celeron and Wraith', 'Block the anchor sightline; its lifetime and range are short.'),
'mountain_one': ('Normal stone/ore mining and stone footing', 'Pursuit and rapid attack trading', 'Deep One and Iron One', 'Move the fight off stone; quarry speed cannot improve harvest tiers.'),
'moss_one': ('Stationary small-party recovery', 'Fire and moving encounters', 'Dryad, Runic One and Auroran', 'Displace the party or deny the patch; three lifetime recipient slots prevent raid-wide healing.'),
'crystal_one': ('One predicted magical or projectile exchange', 'Explosions and sustained melee', 'Auroran and Iron One', 'Bait the one counter and break reply sight/range.'),
'bovine': ('A clear grounded charge lane', 'Corners, pets/allies in the lane and sustained hunger', 'Colossan, Feline and Valen', 'Sidestep the windup or force a solid obstruction.'),
'saurian': ('Patient melee openings and finite underwater tasks', 'Uninsulated cold and mobile pursuit', 'Serpen and Feline', 'Deny stillness before arming; the prepared strike itself permits movement.'),
'chelon': ('Brief physical pressure while withdrawing', 'Magic, waiting opponents and objective interaction', 'Iron One and Terra Drake', 'Wait out the shell or use magic; the stance blocks offense and item use.'),
'zephyr': ('Gentle aerial repositioning and brief lateral evasion', 'Heavy hits, knockback and long ascents', 'Sprite, Avian and Wind Wyrm', 'Control landing space; low health and bounded lift remain relevant.'),
'nightborn': ('Fed nighttime travel and three deliberate living-target hits', 'Daylight, undead/nonliving targets and empty hunger', 'Blood Elf and Zombie', 'Use shields, spacing or an ineligible target; sunlight and food have accessible remedies.'),
'returned': ('Pursuing a recent visible aggressor', 'Fresh targets, cover and recovery-heavy attrition', 'Reaper and Zombie', 'Break sight or exceed the 16-block mark range; this race has no revival.'),
'wailer': ('An anticipated cone against nearby threats', 'Physical burst, interruptions and cover', 'Wraith and Canine', 'Damage the windup or leave the cone; bosses resist control.'),
'scaleheir': ('A short grounded defensive/control exchange', 'Travel and sustained pressure after the finite guard', 'Terra Drake and Valen', 'Wait out the guard; there are no wings or breath attacks.'),
'wyvernkin': ('Terrain-assisted gliding approaches', 'Projectile exposure and sustained upward travel', 'Serpen, Avian and elemental drakes', 'Use cover, armor and ranged pressure; it cannot flap or spam a breath.'),
}
sizes={'human':6,'elven':8,'dwarven':9,'bestial':9,'faeborne':6,'undead':8,'draconic':8}
p=ROOT/'README.md'; text=p.read_text(encoding='utf-8').replace('37 deeply designed races','54 races').replace('runic_races-1.6.3.jar','runic_races-1.7.0.jar')
for family,count in sizes.items():
    text=re.sub(r'(### '+family.title()+r') \(\d+\)', rf'\1 ({count})', text)
    heading=re.search(r'### '+family.title()+r' \(.*?(?=\n##|\Z)', text, re.S)
    if heading:
        section=heading.group()
        for r in RACES:
            if r['family']==family and f'**{r["display"]}**' not in section:
                section=section.rstrip()+f'\n| **{r["display"]}** | {r["names"][0]} | {r["names"][1]} / {r["names"][2]} |\n\n'
        text=text[:heading.start()]+section+text[heading.end():]
text=text.replace('### Draconic (8) — elemental breath, scales, flight','### Draconic (8) — breath, grounded scales and gliding lineages')
text=text.replace('rune-smith party support / scholarly frailty','self ward & hostile slowing / scholarly frailty')
if 'docs/EXPANSION_1.7.0_REFERENCE.md' not in text:
    text += '\n## Version 1.7.0\n\nSeventeen new races retain the original three-power presentation. Use the Origins primary active key; release and press again for Starbound Thread recall or to leave Shellfast. Zephyr also uses the existing wing controls. Wyvernkin glides without a powered flap.\n\nNew racial cooldowns survive death, race changes and reconnects, and pause while offline. Cooldown debt keeps ticking online even after changing race. Updating both the client and server is required (network protocol 3). The old Reaper revival resource keeps its original identity and is separate from Returned.\n\nSee the [complete new-race reference](docs/EXPANSION_1.7.0_REFERENCE.md), [pack-author guide](docs/PACK_AUTHOR_1.7.0.md), and [implementation and validation record](docs/VALIDATION_1.7.0.md).\n'
p.write_text(text,encoding='utf-8')
p=ROOT/'CLAUDE.md';text=p.read_text(encoding='utf-8').replace('**Version**: 1.6.3','**Version**: 1.7.0').replace('37 races','54 races').replace('44 origins, 111 power JSONs','61 origins, 162 power JSONs').replace('37 icon textures','54 icon textures').replace('ALL 37 actives','ALL 54 actives')
text=text.replace('ALL 54 actives route through `SignatureRegistry` (JSON has one','Legacy actives route through `SignatureRegistry` (JSON has one')
if '## Expansion authoring' not in text:
    text += '\n## Expansion authoring\n\nThe 17 new actives call `SignatureRegistry` from the server ability service. `tools/expansion_content.py` emits their tuning into the three visible power bundles. Runtime code reads those configurations; do not add a second per-race TOML tuning table. Cooldown debt alone is persisted in the owned expansion compound. Fields, anchors, preparations, arrows and wards must retain generation/owner validation and bounded lifetimes.\n\nRun `python tools/generate_races.py --check`, `python tools/build_lang.py --check`, `./gradlew test build`, and `./gradlew runGameTestServer`. The GameTest source set is excluded from the release jar. Runtime validation requirements and remaining hands-on gates are recorded in `docs/VALIDATION_1.7.0.md`.\n'
p.write_text(text,encoding='utf-8')
p=ROOT/'CURSEFORGE_DESCRIPTION.md';text=p.read_text(encoding='utf-8').replace('37 races','54 races').replace('**1.6.3**','**1.7.0**').replace('No race is strictly best; every one changes *how* you play, not just *how well*.','Choose strengths and drawbacks that suit the way you play.')
for family in sizes:
    names=', '.join(r['display'] for r in RACES if r['family']==family)
    text=re.sub(r'(^- \*\*'+family.title()+r'\*\*[^\n]+)',lambda m:m.group(1) if names in m.group(1) else m.group(1)+', '+names,text,flags=re.M)
p.write_text(text,encoding='utf-8')
p=ROOT/'gradle.properties';p.write_text(p.read_text().replace('mod_version=1.6.3','mod_version=1.7.0'))
p=ROOT/'tools/ui_lang.json';lang=json.loads(p.read_text(encoding='utf-8'));lang.update({
 'message.runic_races.resize_waiting':'Your new size needs more room. Move into an open space.',
 'message.runic_races.denied.interrupted':'Keening Cry was interrupted.',
});p.write_text(json.dumps(lang,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
lines=['# Runic Races 1.7.0 — expansion reference','',
 'Generated from `tools/expansion_content.py`. Values are HP, blocks and seconds unless explicitly stated. These are final authored values and design comparisons; they are not a claim of measured balance. Runtime validation is recorded separately in [VALIDATION_1.7.0.md](VALIDATION_1.7.0.md).','',
 '| Race | Heritage | Scale | Feathers | Ars mana / cost | ISS damage |', '|---|---|---:|---:|---|---:|']
for r in RACES: lines.append(f'| {r["display"]} | {r["family"].title()} | {r["scale"]:.2f} | {r["feathers"]} | {r["ars"][0]:g}× / {r["ars"][1]:g}× | {r["iss"]:g}× |')
lines+=['','All additions have explicit neutral luck. Curios follows heritage: Elven necklace, Dwarven belt, Faeborne ring and Undead charm. Human, Bestial and Draconic receive no extra slot. Zephyr receives 1.4× incoming knockback; the other additions use 1×. Added size changes geometry without a racial reach increase.','']
for r in RACES:
    strong,weak,nearest,counter=NOTES[r['id']]
    lines += [f'## {r["display"]}', '', f'`runic_races:{r["id"]}` · {r["family"].title()} · impact {r["impact"]}', '',
      f'**{r["names"][0]} — {r["cd"]/20:g}s cooldown.** '+r['active'].format(duration=f'{r["duration"]/20:g}'), '',
      f'**{r["names"][1]}.** '+r['passive'], '', f'**{r["names"][2]}.** '+r['weakness'], '',
      f'Best situations: {strong}. Weak situations: {weak}. Nearest comparisons: {nearest}. Counterplay: {counter}', '',
      'No numerical change from the prompt. Clarifications and shared corrections are listed in the validation record.', '']
(ROOT/'docs/EXPANSION_1.7.0_REFERENCE.md').write_text('\n'.join(lines),encoding='utf-8')
p=ROOT/'BALANCE.md';text=p.read_text(encoding='utf-8').replace('Each race nets **≈ 0** when summing benefits and drawbacks. Every race has exactly','Benefits and drawbacks are a design target, not a literal zero-sum formula or proof of balance. Every race has exactly')
if '## 1.7.0 additions' not in text:
    text += '\n## 1.7.0 additions\n\nThe roster now contains **54 races**, retaining the 37 existing save identities. The [17-race expansion reference](docs/EXPANSION_1.7.0_REFERENCE.md) records every active, passive, weakness, exact cooldown, optional affinity and final comparison note. Shared wards select one strongest applicable prevention per hit after armor and absorption. Nightborn and Moss One cap final healing after ordinary healing modifiers.\n\nFor the original roster, the [1.6.3 source inventory](docs/BASELINE_1.6.3_BALANCE_INVENTORY.md) distinguishes executed data from older shorthand above. Nymph does not provide charm AI, Wraith does not pass through solid blocks, and Runic One provides a self ward with hostile slowing. Unsupported legacy condition wrappers were repaired in 1.7.0 without changing their intended numerical values. Comparative playtesting status is recorded in [the validation record](docs/VALIDATION_1.7.0.md).\n'
p.write_text(text,encoding='utf-8')
print('Updated 1.7.0 roster documentation, metadata and UI language inputs.')
