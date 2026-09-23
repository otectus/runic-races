# Expansion race icon sources

Original PNGs supplied from `C:\Users\crims\Downloads\Skins\icons`.
`tools/generate_expansion_art.py` resizes these to 64×64 RGBA item/selection icons
using Lanczos filtering. Ability HUD icons are separate assets.

Source filenames normalized to race IDs:

- `collosan.png` → `colossan.png`
- `sauiran.png` → `saurian.png`
- `crystal_dwarf.png` → `crystal_one.png`
- `moss_dwarf.png` → `moss_one.png`
- `mountain_dwarf.png` → `mountain_one.png`

The other twelve names are unchanged. These originals are kept outside the mod's
resources so only the resized textures are packaged in the JAR.
