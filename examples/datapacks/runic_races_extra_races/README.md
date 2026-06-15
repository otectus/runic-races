# Example: inject extra origins into a Runic Races family

This datapack adds origin(s) — from another mod or your own — into a Runic Races
**family**, so they appear as selectable "races" once that family is chosen.

It works because `runic_races:race` is just a conditional origin list keyed off the
family pick, and Origins **merges** layer definitions across datapacks when
`"replace": false`. This pack appends to that list rather than replacing it.

## How to use

1. Edit [data/runic_races/origin_layers/race.json](data/runic_races/origin_layers/race.json):
   - Replace `examplemod:angel` with the real origin id(s) you want to add.
   - Change `runic_races:family_fae` to the family you want them under. Valid families:
     `family_mortal`, `family_fae`, `family_beast`, `family_underfolk`, `family_dragon`,
     `family_cursed`. Add more `{ "condition": ..., "origins": [...] }` blocks for other families.
2. Drop this folder into `<world>/datapacks/` (or a global datapack folder) and `/reload`
   (origin-layer membership is picked up on reload; a re-pick may need rejoining the world).

## Adding a brand-new family

Also create `data/runic_races/origin_layers/family.json` with `"replace": false` listing your
new `yourns:family_x` origin, add the family origin file at `data/yourns/origins/family_x.json`,
then gate a race-layer block on it as above.

## Caveat

An injected foreign origin is **not** known to Runic Races internals — `RaceHelper` only
recognizes `runic_races:` origins, so the racial HUD/notifications, Pehkui scaling, and Curios
slot grants won't apply to it. It simply behaves as its own mod's origin shown in the race menu.
Its own powers work normally.
