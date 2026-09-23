"""UV/asset regression checks. Run: python -m unittest discover -s tools -p test_wings.py"""
from pathlib import Path
import re
import unittest

from PIL import Image

import generate_wings as wings


class WingArtTest(unittest.TestCase):
    def test_atlas_islands_fit_without_overlap(self):
        wings.validate_layout()

    def test_every_mapped_texture_has_a_reproducible_atlas(self):
        source = (wings.REPO / 'src/main/java/com/otectus/runic_races/client/render/WingType.java').read_text()
        mapped = set(re.findall(r'"textures/entity/([a-z_]+\.png)"', source))
        self.assertEqual(mapped, set(wings.VARIANTS))
        pixels = set()
        for filename, race in wings.VARIANTS.items():
            with self.subTest(race=race):
                with Image.open(wings.REPO / wings.DIR / filename) as stored:
                    self.assertEqual(stored.mode, 'RGBA')
                    self.assertEqual(stored.size, (64, 64))
                    self.assertEqual(stored.tobytes(), wings.build_sheet(race).tobytes())
                    pixels.add(stored.tobytes())
        self.assertEqual(len(pixels), len(mapped), 'Each race needs distinct wing art')

    def test_model_box_uv_dimensions_match_the_art(self):
        source = (wings.REPO / 'src/main/java/com/otectus/runic_races/client/render/WingModel.java').read_text()
        number = r'[-\d.]+F?'
        literal_box = re.compile(r'box\(mirror,\s*(\d+),\s*(\d+),\s*' +
                                 r',\s*'.join([number] * 3) +
                                 r',\s*(\d+),\s*(\d+),\s*(\d+)\)')
        model_uvs = {tuple(map(int, match.groups())) for match in literal_box.finditer(source)}
        # Avian primaries are generated from parallel UV and length arrays.
        lengths = re.search(r'int\[\] lengths = \{([^}]+)\}', source).group(1)
        offsets = re.search(r'int\[\] uv = \{([^}]+)\}', source).group(1)
        for offset, length in zip(map(int, offsets.split(',')), map(int, lengths.split(',')), strict=True):
            model_uvs.add((offset, 24, 2, length, 1))
        self.assertEqual(model_uvs, set(wings.ISLANDS.values()),
                         'Model dimensions or UV offsets diverged from generated art')

    def test_pixel_palette_and_transparency_stay_crisp(self):
        for race in wings.PALETTES:
            with self.subTest(race=race):
                atlas = wings.build_sheet(race)
                colors = atlas.getcolors(64 * 64)
                self.assertLessEqual(len({rgba[:3] for _, rgba in colors if rgba[3]}), 12)
                for name, (u, v, w, h, depth) in wings.ISLANDS.items():
                    front = atlas.crop((u + depth, v + depth, u + depth + w, v + depth + h))
                    self.assertIsNotNone(front.getbbox(), f'Unpainted {race} {name}')
                    if name.startswith('membrane'):
                        self.assertEqual(front.getextrema()[3], (0, 255), 'Scallops must retain cutout pixels')
                    if race in wings.GOSSAMER and name == 'forewing':
                        self.assertTrue(any(0 < rgba[3] < 255 for _, rgba in front.getcolors(w * h)))


if __name__ == '__main__':
    unittest.main()
