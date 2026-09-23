#!/usr/bin/env python3
"""Author all ten Minecraft-scale wing atlases without filtered legacy crops.

The box UVs below match WingModel.createLayer. Individual feather, membrane,
structural bone, and gossamer layers use their own pixel art. Expansion art
imports build_sheet to keep its wings on the same atlas after regeneration.
Legacy *_base.png and drake_wings.png remain untouched.

Run: python tools/generate_wings.py [--output-root PATH] [--preview PATH]
"""
from pathlib import Path
import argparse
from PIL import Image, ImageDraw

REPO = Path(__file__).resolve().parent.parent
DIR = Path('src/main/resources/assets/runic_races/textures/entity')
SHEET = 64
# name: u, v, width, height, depth. Every box has all six UV faces.
ISLANDS = {
    'membrane_inner': (0, 0, 5, 15, 1),
    'membrane_outer': (14, 0, 6, 17, 1),
    'shoulder': (30, 0, 3, 7, 2),
    'spar': (42, 0, 2, 13, 1),
    'finger': (50, 0, 1, 15, 1),
    'claw': (56, 0, 2, 4, 1),
    'feather_arm': (0, 24, 4, 14, 2),
    'primary_long': (14, 24, 2, 17, 1),
    'primary_mid': (22, 24, 2, 15, 1),
    'primary_short': (30, 24, 2, 12, 1),
    'covert': (38, 24, 2, 7, 1),
    'forewing': (0, 48, 6, 12, 1),
    'fore_tip': (16, 48, 3, 7, 1),
    'hindwing': (26, 48, 5, 8, 1),
    'hind_tip': (40, 48, 2, 5, 1),
    'gossamer_rib': (48, 48, 1, 9, 1),
}
# ink, shadow, body, light, accent, highlight; intentionally small palettes.
PALETTES = {
    'pixie': ('18384C', '36556F', '579B99', '9CDAB7', 'CDA4F0', 'E7FFE1'),
    'faerie': ('463151', '744F87', 'AD78AF', 'E8ACC9', 'BCB8FA', 'FFF0DE'),
    'zephyr': ('244D62', '3F7D91', '66B4BE', 'AFE1DB', 'D6F6EE', 'F1FFFA'),
    'avian': ('30241F', '60422E', '94704A', 'C29E6D', 'DFCAA0', 'F3E4C3'),
    'wyvern': ('20392F', '365D4C', '67886A', 'A3BB88', 'D5CC96', 'F0DFC0'),
    'wyvernkin': ('342F29', '58513D', '817854', 'AAA36A', 'BCDA8C', 'E2D9AC'),
    'fire_drake': ('321D22', '622C2A', 'A1482C', 'D87434', 'FAAC45', 'FFE0A0'),
    'ice_drake': ('20354F', '345F83', '5393B5', '92C7D7', 'CAEAF0', 'F3FFFF'),
    'terra_drake': ('302E25', '56513A', '85785A', 'ADA079', '7D9B59', 'DBC698'),
    'volt_drake': ('28263F', '4A425E', '716173', 'AAA181', 'EDCF55', 'FFF2AE'),
}
VARIANTS = {f'{race}_wings.png': race for race in PALETTES}
GOSSAMER = {'pixie', 'faerie', 'zephyr'}


def color(value):
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4)) + (255,)


def island_rect(u, v, w, h, depth):
    return u, v, u + 2 * (w + depth), v + depth + h


def validate_layout():
    occupied = set()
    for name, island in ISLANDS.items():
        x0, y0, x1, y1 = island_rect(*island)
        assert 0 <= x0 < x1 <= SHEET and 0 <= y0 < y1 <= SHEET, name
        pixels = {(x, y) for x in range(x0, x1) for y in range(y0, y1)}
        assert not pixels & occupied, f'Overlapping wing UV island: {name}'
        occupied |= pixels


def authored_face(race, name, w, h):
    ink, shadow, body, light, accent, highlight = map(color, PALETTES[race])
    im = Image.new('RGBA', (w, h))
    draw = ImageDraw.Draw(im)
    if name.startswith('membrane'):
        # Finger-to-finger scallops and a bevel along the leading edge.
        ends = ([h-1, h-3, h-5, h-3, h-2, h-1] if w == 6
                else [h-1, h-3, h-2, h-4, h-1])
        for x in range(w):
            for y in range(ends[x] + 1):
                shade = body if (x + y // 3) % 3 else light
                if y == 0 or y == ends[x]:
                    shade = ink if y else accent
                elif (x + 2 * (y // 2)) % 5 == 0:
                    shade = shadow
                im.putpixel((x, y), shade)
        # Each elemental design has its own structure, beyond palette changes.
        if race == 'fire_drake':
            draw.line([(w-1,1),(w-2,4),(w-3,5),(w-2,8),(1,h-5)], fill=accent)
            draw.point((w-2,4), fill=highlight)
        elif race == 'ice_drake':
            draw.line([(w-1,1),(1,5),(w-2,8),(1,h-5)], fill=highlight)
            draw.line([(0,3),(w-2,6)], fill=accent)
        elif race == 'terra_drake':
            for y in (3,7,10):
                draw.line([(0,y),(2,y+1),(w-1,y)], fill=shadow)
            draw.point((1,5), fill=accent)
            draw.point((w-2,9), fill=accent)
        elif race == 'volt_drake':
            draw.line([(w-1,1),(1,5),(w-2,5),(1,10),(2,10),(1,h-4)], fill=accent)
            draw.point((1,5), fill=highlight)
        elif race == 'wyvernkin':
            for y in (3,7,10):
                draw.line([(w-1,y-1),(1,y),(2,y+1)], fill=accent)
        else:
            for y in (4,8):
                draw.line([(w-1,y-2),(1,y)], fill=light)
        for x in range(w):
            for y in range(ends[x] + 1, h):
                im.putpixel((x, y), (0,0,0,0))
    elif name in {'shoulder','spar','finger','claw'}:
        for y in range(h):
            for x in range(w):
                shade = light if x == 0 else shadow
                if y % 4 == 3:
                    shade = ink
                if name == 'shoulder':
                    shade = [shadow,body,light][(x+y//2)%3]
                if name == 'claw':
                    shade = highlight if x == 0 else accent
                im.putpixel((x,y),shade)
        if name == 'claw':
            im.putpixel((w-1,0),(0,0,0,0))
    elif name.startswith('primary') or name in {'feather_arm','covert'}:
        for y in range(h):
            for x in range(w):
                shade = light if x % 2 == 0 else body
                if (y+x) % 4 == 0:
                    shade = shadow
                if y > h-5:
                    shade = accent if (y+x) % 3 else highlight
                if y != h-1 or x % 2 == 0:
                    im.putpixel((x,y),shade)
    else:
        # Darker one-pixel rims and bright branching veins survive low light.
        for y in range(h):
            for x in range(w):
                if w > 2 and ((y == 0 and x < 2) or (y == h-1 and x > w-3)):
                    continue
                edge = x == 0 or x == w-1 or y == 0 or y == h-1
                shade = light if edge else (accent if (x+y)%4 == 0 else body)
                alpha = 225 if edge else 155
                if race == 'zephyr':
                    # Interwoven wind ribbons have open gaps between them.
                    curve = [0,0,1,2,2,1][y%6]
                    if w > 2 and x not in {curve%w,(curve+1)%w,(curve+3)%w}:
                        continue
                    shade, alpha = (highlight if x%2 else light), 210
                im.putpixel((x,y),shade[:3]+(alpha,))
        if race != 'zephyr':
            draw.line([(w-1,1),(w//2,h//2),(0,h-2)], fill=highlight)
            if w > 3:
                draw.line([(w//2,h//2),(0,2)], fill=accent)
                draw.point((w-2,h-3), fill=highlight)
                if race == 'faerie':
                    draw.point((w-2,h//2+1), fill=ink)
        if name == 'gossamer_rib':
            for y in range(h):
                im.putpixel((0,y),highlight if y%3 else accent)
    return im


def shade_edge(im):
    result = im.copy()
    result.putdata([tuple(c*4//5 for c in p[:3])+(p[3],)
                    for y in range(im.height) for x in range(im.width)
                    for p in [im.getpixel((x,y))]])
    return result


def paint_box(sheet, u, v, w, h, depth, art):
    """Vanilla box UVs, with actual edge texels on all four thin faces."""
    nearest = Image.Resampling.NEAREST
    for crop, size, pos in [
        ((0,0,w,1),(w,depth),(u+depth,v)),
        ((0,h-1,w,h),(w,depth),(u+depth+w,v)),
        ((0,0,1,h),(depth,h),(u,v+depth)),
        ((w-1,0,w,h),(depth,h),(u+depth+w,v+depth)),
    ]:
        sheet.paste(shade_edge(art.crop(crop)).resize(size,nearest),pos)
    sheet.paste(art,(u+depth,v+depth))
    sheet.paste(art.transpose(Image.Transpose.FLIP_LEFT_RIGHT),(u+2*depth+w,v+depth))


def build_sheet(race):
    validate_layout()
    sheet = Image.new('RGBA',(SHEET,SHEET))
    for name,(u,v,w,h,depth) in ISLANDS.items():
        paint_box(sheet,u,v,w,h,depth,authored_face(race,name,w,h))
    return sheet


def preview(path):
    """Atlas contact sheet for art QA; not an in-game rendering."""
    sheet = Image.new('RGB',(5*290,2*350),'#17212A')
    draw = ImageDraw.Draw(sheet)
    for i,race in enumerate(PALETTES):
        x,y = i%5*290,i//5*350
        atlas = build_sheet(race).resize((256,256),Image.Resampling.NEAREST)
        sheet.paste(atlas,(x+16,y+36),atlas)
        draw.text((x+16,y+12),race.replace('_',' ').upper(),fill='#E4E6D7')
        draw.text((x+16,y+302),'64px atlas / nearest-neighbor',fill='#A1B3B8')
    path.parent.mkdir(parents=True,exist_ok=True)
    sheet.save(path)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output-root',type=Path,default=REPO)
    parser.add_argument('--preview',type=Path)
    args = parser.parse_args()
    output = args.output_root/DIR
    output.mkdir(parents=True,exist_ok=True)
    for filename,race in VARIANTS.items():
        build_sheet(race).save(output/filename)
        print('wrote',output/filename)
    if args.preview:
        preview(args.preview)


if __name__ == '__main__':
    main()
