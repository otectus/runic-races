#!/usr/bin/env bash
# Fetches the compileOnly dependency jars a fresh clone lacks (Dependencies/ is
# gitignored). Origins Forge bundles Apoli/Calio via jar-in-jar, so those are
# extracted from its -all jar — the same layout the manual setup documents.
#
# Iron's Spellbooks: the locally-pinned 3.15.4 is not published on Modrinth;
# 3.15.5.1 is the nearest same-line release and is API-compatible for compiling.
set -euo pipefail
cd "$(dirname "$0")/.."
mkdir -p Dependencies

# Each jar is saved under the EXACT filename build.gradle pins in its
# `compileOnly files('Dependencies/...')` list — `files()` on a missing path is
# silently empty, so a name drift here surfaces as "cannot find symbol" walls.
fetch() {
  local slug="$1" version="$2" out="$3"
  echo "Fetching $slug $version -> $out"
  local url
  url=$(curl -sf "https://api.modrinth.com/v2/project/$slug/version/$version" \
    | python3 -c "import json,sys; fs=json.load(sys.stdin)['files']; print(next((f['url'] for f in fs if f.get('primary')), fs[0]['url']))")
  curl -sfL -o "Dependencies/$out" "$url"
}

fetch origins-forge 1.20.1-1.10.0.9 origins-forge-1.20.1-1.10.0.9-all.jar
fetch curios 5.14.1+1.20.1 curios-forge-5.14.1+1.20.1.jar
fetch ars-nouveau 4.12.7 ars_nouveau-1.20.1-4.12.7-all.jar
# 3.15.4 is not on Modrinth (see header note); the API-compatible 3.15.5.1 is
# saved under the filename build.gradle pins so the compile classpath resolves.
fetch irons-spells-n-spellbooks 1.20.1-3.15.5.1 irons_spellbooks-1.20.1-3.15.4.jar

# Apoli + Calio (+ additionalentityattributes) ride inside the Origins all-jar.
unzip -o -j "Dependencies/origins-forge-1.20.1-1.10.0.9-all.jar" 'META-INF/jarjar/*.jar' -d Dependencies/
ls -la Dependencies/*.jar
