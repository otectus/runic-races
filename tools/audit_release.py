"""Validate the assembled distribution, rather than only its source resources."""
import argparse
import hashlib
import json
from pathlib import Path
import zipfile

from expansion_content import RACES


def main():
    properties = (Path(__file__).resolve().parents[1] / "gradle.properties").read_text()
    version = next(line.split("=", 1)[1].strip() for line in properties.splitlines()
                   if line.startswith("mod_version="))
    parser = argparse.ArgumentParser()
    parser.add_argument("jar", nargs="?", default=f"build/libs/runic_races-{version}.jar")
    args = parser.parse_args()
    path = Path(args.jar)
    with zipfile.ZipFile(path) as jar:
        names = set(jar.namelist())
        origins = sorted(n for n in names if n.startswith("data/runic_races/origins/") and n.endswith(".json"))
        powers = sorted(n for n in names if n.startswith("data/runic_races/powers/") and n.endswith(".json"))
        assert len(origins) == 61, f"Expected 54 races + 7 selectors, found {len(origins)}"
        assert len(powers) == 162, f"Expected 162 top-level powers, found {len(powers)}"
        for name in names:
            if name.endswith(".json"):
                json.loads(jar.read(name))
        metadata = jar.read("META-INF/mods.toml").decode()
        assert f'version="{version}"' in metadata or f'version = "{version}"' in metadata
        assert not any("/gametest/" in n.lower() or n.startswith("data/runic_races/structures/") for n in names), "Test fixture leaked into distribution"
        mixins = json.loads(jar.read("runic_races.mixins.json"))
        refmap = json.loads(jar.read(mixins["refmap"]))
        assert refmap.get("mappings"), "Release refmap is empty"
        manifest = jar.read("META-INF/MANIFEST.MF").decode()
        assert "runic_races.mixins.json" in manifest, "Mixin configuration missing from manifest"
        assert all("com/otectus/runic_races/mixin/" + name + ".class" in names for name in mixins["mixins"])
        for race in RACES:
            rid = race["id"]
            origin = json.loads(jar.read(f"data/runic_races/origins/{rid}.json"))
            assert len(origin["powers"]) == 3, f"Visible bundle contract failed: {rid}"
            for asset in (f"textures/item/{rid}.png", f"models/item/{rid}_icon.json",
                          f"textures/gui/ability/{rid}/{race['powers'][0]}.png"):
                assert "assets/runic_races/" + asset in names, f"Missing packaged asset: {asset}"
        for wing in ("zephyr", "wyvernkin"):
            assert f"assets/runic_races/textures/entity/{wing}_wings.png" in names
    report = dict(artifact=str(path), version=version, bytes=path.stat().st_size,
                  sha256=hashlib.sha256(path.read_bytes()).hexdigest(), origins=len(origins),
                  powers=len(powers), expansion_races=len(RACES), mixins=len(mixins["mixins"]),
                  refmap_mappings=len(refmap["mappings"]), passed=True)
    Path("build").mkdir(exist_ok=True)
    Path("build/release-audit.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
