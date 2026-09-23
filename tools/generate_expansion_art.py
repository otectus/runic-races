#!/usr/bin/env python3
"""Original pixel silhouettes for the established Runic Races icon and articulated-wing system.

Ability motifs are drawn on a 16px logical grid. Race portraits use the supplied
skin-head artwork in tools/art/race_icons; original procedural portraits are a fallback.
"""
from pathlib import Path
import argparse, json
from PIL import Image, ImageDraw
from expansion_content import RACES
from generate_wings import build_sheet as build_wing_sheet

REPO = Path(__file__).resolve().parent.parent
parser=argparse.ArgumentParser()
parser.add_argument('--output-root', type=Path, default=REPO)
args=parser.parse_args()
ROOT=args.output_root/'src/main/resources/assets/runic_races'
PALETTES={
 'colossan':('#656776','#CCA667','#F7E2B0'), 'auroran':('#BA737B','#EDC077','#FFF0D1'),
 'grove_elf':('#3E7557','#ABC479','#E6DAC0'), 'tide_elf':('#286C94','#69D2CA','#DDF6EE'),
 'astral_elf':('#6269A8','#B8A9EE','#EEF0F9'), 'mountain_one':('#566778','#A4BCC4','#E7DAD0'),
 'moss_one':('#526B48','#A8B861','#E2CD96'), 'crystal_one':('#665180','#B996DD','#E5D2FA'),
 'bovine':('#805039','#CF9B5E','#F3D9A4'), 'saurian':('#426D4F','#8FA765','#E6BC67'),
 'chelon':('#326E69','#95AF7A','#DEC18A'), 'zephyr':('#57979F','#AAE1D6','#F6FBF7'),
 'nightborn':('#5F3A58','#B0546B','#F0C4B5'), 'returned':('#595C7A','#A59CC4','#DCE0E3'),
 'wailer':('#6C628D','#B5AECF','#F1ECF6'), 'scaleheir':('#864440','#C59657','#E6D4A5'),
 'wyvernkin':('#61584B','#A59B67','#BAD884')}

def motif(kind):
 im=Image.new('RGBA',(16,16));d=ImageDraw.Draw(im)
 dark,mid,light=PALETTES[kind]; ink='#1C2435'
 def poly(points,c=mid):d.polygon(points,fill=c)
 def line(points,c=light,w=1):d.line(points,fill=c,width=w)
 if kind=='colossan':
  poly([(3,6),(3,3),(5,2),(6,4),(7,2),(9,2),(10,4),(12,3),(13,5),(12,10),(10,13),(5,13),(3,10)],ink)
  poly([(4,5),(5,3),(6,6),(8,3),(9,3),(9,6),(11,5),(12,5),(11,10),(9,12),(5,12),(4,9)])
  line([(4,7),(8,7),(10,9)],dark);line([(5,10),(8,11)]);d.rectangle((5,13,10,14),fill=light)
 elif kind=='auroran':
  poly([(7,3),(12,5),(11,11),(7,14),(3,11),(2,5)],ink);poly([(7,4),(11,6),(10,10),(7,12),(4,10),(3,6)])
  line([(7,6),(7,10)]);line([(5,8),(9,8)]);line([(0,5),(1,7),(2,8)]);line([(14,5),(13,7),(12,8)])
  line([(5,1),(9,1)]);line([(7,0),(7,2)])
 elif kind=='grove_elf':
  poly([(2,9),(2,4),(5,2),(7,2),(7,5),(4,8)],dark);poly([(8,11),(10,7),(14,7),(13,11),(10,13)],mid)
  line([(2,14),(12,3)],light);poly([(10,2),(14,1),(13,5)],light);line([(2,11),(5,14)],mid)
 elif kind=='tide_elf':
  line([(1,10),(3,7),(5,7),(7,10),(9,10),(11,7),(14,7)],mid,2)
  line([(2,13),(5,11),(8,13),(12,11)],light);line([(4,4),(12,4)],light,2);poly([(10,1),(15,4),(10,6)],light)
 elif kind=='astral_elf':
  line([(4,10),(11,5)],mid)
  for x,y in [(3,11),(12,4)]:
   poly([(x,y-3),(x+1,y-1),(x+3,y),(x+1,y+1),(x,y+3),(x-1,y+1),(x-3,y),(x-1,y-1)],light)
  d.point((8,2),fill=mid);d.point((13,12),fill=mid)
 elif kind=='mountain_one':
  poly([(0,14),(5,6),(8,11),(11,7),(15,14)],dark);line([(5,5),(11,1),(14,3)],light,2)
  line([(10,3),(4,12)],mid,2);line([(1,14),(14,14)],light)
 elif kind=='moss_one':
  poly([(2,9),(3,5),(6,2),(10,2),(13,5),(14,9)],ink);poly([(3,8),(4,5),(7,3),(9,3),(12,5),(13,8)])
  d.rectangle((6,9,9,13),fill=light);line([(3,14),(12,14)],dark,2)
  for x,y in [(5,6),(8,4),(11,6)]:d.point((x,y),fill=light)
 elif kind=='crystal_one':
  poly([(7,0),(13,5),(12,10),(8,15),(2,10),(1,5)],ink)
  poly([(7,1),(12,5),(8,7),(3,5)],light);poly([(3,6),(7,8),(7,13),(3,10)],mid)
  poly([(9,7),(12,6),(11,10),(9,13)],dark);line([(8,4),(6,7),(9,9),(7,12)],ink)
 elif kind=='bovine':
  poly([(1,1),(2,6),(5,8),(6,5),(9,5),(10,8),(13,6),(14,1),(15,7),(12,11),(9,10),(7,14),(5,10),(2,11),(0,7)],mid)
  line([(1,2),(2,6),(5,8)],light);line([(14,2),(13,6),(10,8)],light)
  d.rectangle((6,7,8,10),fill=dark);d.point((7,12),fill=light)
 elif kind=='saurian':
  poly([(0,8),(4,4),(10,3),(15,7),(12,11),(5,12)],dark)
  poly([(2,8),(6,5),(10,5),(13,7),(11,9),(5,10)],light);line([(8,5),(7,10)],ink,2)
  line([(3,13),(6,14),(10,13),(13,14)],mid)
 elif kind=='chelon':
  poly([(5,1),(10,1),(14,5),(14,10),(10,14),(5,14),(1,10),(1,5)],ink)
  poly([(5,2),(10,2),(13,5),(13,10),(10,13),(5,13),(2,10),(2,5)])
  poly([(6,5),(9,5),(11,8),(9,10),(6,10),(4,8)],dark)
  for a,b in [((6,5),(5,2)),((9,5),(10,2)),((11,8),(13,8)),((9,10),(10,13)),((6,10),(5,13)),((4,8),(2,8))]:line([a,b],light)
 elif kind=='zephyr':
  line([(1,5),(8,5),(10,3),(9,1),(7,1)],light,2);line([(3,8),(12,8),(14,6),(13,4)],mid,2)
  line([(0,11),(7,11),(9,13),(8,15),(6,15)],light);d.point((12,12),fill=mid)
 elif kind=='nightborn':
  d.ellipse((1,1,12,13),fill=light);d.ellipse((5,0,14,10),fill=(0,0,0,0))
  poly([(10,6),(14,11),(14,13),(12,15),(9,14),(8,12)],mid);line([(11,10),(12,12)],light)
 elif kind=='returned':
  line([(9,1),(12,3),(14,7),(12,12),(8,14),(3,12),(1,8),(3,4),(5,3)],mid,2)
  line([(5,2),(5,6),(8,8),(6,11),(8,14)],light);line([(1,8),(4,8)],light)
 elif kind=='wailer':
  poly([(4,1),(11,1),(13,4),(12,11),(8,15),(3,11),(2,4)],mid)
  line([(4,3),(7,2),(11,3)],light);line([(4,5),(6,6)],ink,2);line([(9,6),(11,5)],ink,2)
  d.ellipse((6,8,9,12),fill=ink);line([(0,6),(0,10)],light);line([(15,6),(15,10)],light)
 elif kind=='scaleheir':
  poly([(2,3),(7,1),(13,3),(12,10),(7,15),(3,10)],dark)
  poly([(5,3),(8,2),(10,5),(13,6),(10,8),(9,12),(6,11),(4,7)],mid)
  line([(8,4),(10,5)],light);d.point((9,6),fill=ink);line([(4,5),(2,2)],light);line([(10,3),(12,1)],light)
 elif kind=='wyvernkin':
  poly([(1,13),(3,5),(7,1),(9,7),(14,4),(12,12),(8,9)],dark)
  line([(1,13),(4,6),(7,2),(8,8),(14,4)],light);line([(8,9),(10,13),(13,14)],mid)
  poly([(12,12),(15,14),(12,15)],light)
 return im

def portrait(r):
 supplied=REPO/'tools/art/race_icons'/f"{r['id']}.png"
 if supplied.is_file():
  with Image.open(supplied) as image:
   return image.convert('RGBA').resize((64,64),Image.Resampling.LANCZOS)
 kind=r['id'];dark,mid,light=PALETTES[kind]
 im=Image.new('RGBA',(32,32));d=ImageDraw.Draw(im)
 d.polygon([(6,2),(25,2),(30,7),(30,24),(25,29),(6,29),(1,24),(1,7)],fill='#172331')
 d.line([(6,2),(25,2),(30,7)],fill=light,width=1)
 # Shoulders and family anatomy share a grammar, while head shapes remain individually authored.
 d.polygon([(5,27),(8,21),(14,19),(20,20),(25,23),(27,27)],fill=dark)
 heads={
 'colossan':[(9,8),(22,7),(24,15),(21,21),(12,22),(8,17)],
 'auroran':[(11,7),(21,7),(23,13),(20,21),(15,23),(10,18)],
 'grove_elf':[(10,7),(21,6),(23,12),(20,20),(15,23),(11,18)],
 'tide_elf':[(11,7),(21,8),(23,15),(19,22),(14,22),(10,16)],
 'astral_elf':[(12,6),(21,8),(23,14),(18,23),(13,20),(10,12)],
 'mountain_one':[(8,9),(22,8),(24,16),(21,23),(10,23),(7,16)],
 'moss_one':[(9,10),(22,10),(24,17),(20,24),(12,23),(8,18)],
 'crystal_one':[(12,7),(21,9),(24,16),(19,24),(11,22),(8,14)],
 'bovine':[(10,8),(22,8),(23,19),(19,24),(12,23),(8,17)],
 'saurian':[(9,9),(21,7),(25,13),(24,20),(17,23),(10,18)],
 'chelon':[(11,9),(20,8),(24,12),(23,18),(18,22),(12,20),(9,15)],
 'zephyr':[(13,6),(20,8),(22,14),(18,22),(12,19),(10,12)],
 'nightborn':[(11,7),(21,7),(23,14),(19,23),(13,21),(9,14)],
 'returned':[(10,8),(20,6),(23,13),(21,22),(12,23),(8,15)],
 'wailer':[(10,7),(21,7),(24,12),(21,21),(17,26),(11,21),(8,12)],
 'scaleheir':[(10,9),(18,6),(24,10),(26,17),(20,23),(12,21),(8,15)],
 'wyvernkin':[(12,8),(21,7),(25,13),(22,21),(17,23),(10,18)]}
 d.polygon(heads[kind],fill=mid)
 d.line([(11,11),(14,10)],fill=light,width=1);d.line([(19,10),(22,11)],fill=light)
 d.line([(12,14),(14,14)],fill='#192331');d.line([(19,14),(21,14)],fill='#192331')
 d.line([(15,20),(19,20)],fill=dark)
 if r['family']=='elven':
  d.polygon([(10,13),(5,9),(8,17),(12,17)],fill=light);d.polygon([(22,13),(28,9),(25,17),(21,17)],fill=light)
  d.polygon([(10,6),(19,4),(23,8),(22,11),(17,8),(10,12)],fill=dark)
 if r['family']=='dwarven':
  d.polygon([(9,17),(15,20),(20,18),(24,17),(23,24),(16,28),(9,24)],fill=dark)
  d.line([(12,21),(13,25)],fill=light);d.line([(19,21),(18,26)],fill=mid)
 if kind=='colossan': d.rectangle((8,7,22,10),fill=dark);d.line([(8,19),(11,22),(21,22)],fill=light,width=2)
 if kind=='auroran': d.ellipse((10,3,22,6),outline=light);d.line([(15,17),(17,17)],fill=light)
 if kind=='tide_elf': d.line([(9,10),(7,7),(13,9)],fill=light);d.line([(24,12),(27,15),(24,17)],fill=light)
 if kind=='astral_elf': d.polygon([(16,5),(17,8),(20,9),(17,10),(16,13),(15,10),(12,9),(15,8)],fill=light)
 if kind=='moss_one': d.polygon([(6,11),(8,6),(14,3),(21,4),(26,8),(27,11)],fill=dark);d.rectangle((12,6,14,7),fill=light);d.rectangle((21,8,23,9),fill=mid)
 if kind=='crystal_one': d.line([(18,8),(15,13),(18,16),(15,22)],fill=light)
 if kind=='bovine':
  d.line([(10,10),(6,8),(4,3)],fill=light,width=3);d.line([(22,10),(26,8),(28,3)],fill=light,width=3);d.ellipse((10,17,23,23),fill=dark);d.point((13,20),fill=light);d.point((20,20),fill=light)
 if kind=='saurian':
  d.polygon([(10,9),(9,4),(14,7),(17,3),(19,7),(23,5),(23,10)],fill=dark);d.line([(11,14),(15,12)],fill=light);d.line([(20,16),(25,16)],fill=dark)
 if kind=='chelon':
  d.ellipse((4,16,28,30),fill=dark);d.polygon([(13,22),(20,22),(23,26),(19,29),(12,28),(9,25)],fill=mid);d.line([(13,22),(10,18)],fill=light);d.line([(20,22),(24,18)],fill=light)
 if kind=='zephyr':
  d.line([(8,9),(4,7),(8,4),(22,4),(27,7)],fill=light,width=2);d.line([(5,20),(9,24),(24,25),(27,22)],fill=light);d.line([(15,20),(18,18)],fill=light)
 if kind=='nightborn':
  d.polygon([(8,24),(5,18),(12,20),(17,25),(23,20),(28,18),(26,27)],fill=dark);d.line([(14,18),(14,20)],fill=light);d.line([(20,18),(20,20)],fill=light)
 if kind=='returned': d.line([(16,7),(14,12),(17,16),(15,22)],fill=light);d.line([(11,24),(16,27),(23,24)],fill=light)
 if kind=='wailer':
  d.ellipse((14,17,20,23),fill=dark);d.line([(11,15),(13,19)],fill=light);d.line([(23,14),(22,18)],fill=light)
 if kind in ('scaleheir','wyvernkin'):
  d.polygon([(10,10),(6,3),(13,7)],fill=light);d.polygon([(20,8),(26,3),(24,12)],fill=light);d.line([(21,17),(26,16)],fill=dark)
  if kind=='wyvernkin': d.polygon([(5,26),(3,15),(10,21),(9,27)],fill=light);d.polygon([(25,26),(29,14),(24,20)],fill=dark)
 return im

def wing(kind):
 # The shared generator owns the geometry/UV contract for all ten winged races.
 return build_wing_sheet(kind)

for r in RACES:
 race=r['id'];active=r['powers'][0]
 for relative,im in [(f'textures/item/{race}.png',portrait(r).resize((64,64),Image.Resampling.NEAREST)),
                     (f'textures/gui/ability/{race}/{active}.png',motif(race).resize((32,32),Image.Resampling.NEAREST))]:
  path=ROOT/relative;path.parent.mkdir(parents=True,exist_ok=True);im.save(path)
 model=ROOT/f'models/item/{race}_icon.json';model.parent.mkdir(parents=True,exist_ok=True)
 model.write_text(json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':f'runic_races:item/{race}'}},indent=2)+'\n')
for race in ['zephyr','wyvernkin']:
 path=ROOT/f'textures/entity/{race}_wings.png';path.parent.mkdir(parents=True,exist_ok=True);wing(race).save(path)
# The flap has a separate air-ribbon icon, still part of Zephyr's one passive bundle.
motif('zephyr').transpose(Image.Transpose.FLIP_TOP_BOTTOM).resize((32,32),Image.Resampling.NEAREST).save(ROOT/'textures/gui/ability/zephyr/airborne_essence_flap.png')
sheet=Image.new('RGB',(6*160,3*130),'#101923');d=ImageDraw.Draw(sheet)
for i,r in enumerate(RACES):
 x=(i%6)*160;y=(i//6)*130;im=portrait(r).resize((80,80),Image.Resampling.NEAREST);sheet.paste(im,(x+6,y+8),im)
 icon=motif(r['id']).resize((48,48),Image.Resampling.NEAREST);sheet.paste(icon,(x+100,y+24),icon)
 d.text((x+7,y+97),r['display'],fill='#E2E8EC')
preview=args.output_root/'build/art/expansion-contact-sheet.png';preview.parent.mkdir(parents=True,exist_ok=True);sheet.save(preview)
print('Generated 17 portraits, 18 ability icons, 17 models and two original wing sheets.')
