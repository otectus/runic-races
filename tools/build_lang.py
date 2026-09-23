#!/usr/bin/env python3
"""Deterministically merge authored race and interface localization."""
import argparse, json
from pathlib import Path
REPO = Path(__file__).resolve().parent.parent
parser=argparse.ArgumentParser()
parser.add_argument('--output-root',type=Path,default=REPO)
parser.add_argument('--check',action='store_true')
args=parser.parse_args()
merged=json.loads((REPO/'tools/ui_lang.json').read_text(encoding='utf-8'))
merged.update(json.loads((args.output_root/'tools/race_lang.json').read_text(encoding='utf-8')))
path=args.output_root/'src/main/resources/assets/runic_races/lang/en_us.json'
if args.check:
    if json.loads(path.read_text(encoding='utf-8')) != merged: raise SystemExit('Language semantic differences')
else:
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(merged,indent=2,ensure_ascii=False,sort_keys=True)+'\n',encoding='utf-8')
print(f'Language: {len(merged)} keys; '+('verified' if args.check else 'written'))
