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

fetch() {
  local slug="$1" version="$2"
  echo "Fetching $slug $version"
  local url
  url=$(curl -sf "https://api.modrinth.com/v2/project/$slug/version/$version" \
    | python3 -c "import json,sys; fs=json.load(sys.stdin)['files']; print(next((f['url'] for f in fs if f.get('primary')), fs[0]['url']))")
  curl -sfL -o "Dependencies/$slug-$version.jar" "$url"
}

fetch origins-forge 1.20.1-1.10.0.9
fetch curios 5.14.1+1.20.1
fetch ars-nouveau 4.12.7
fetch irons-spells-n-spellbooks 1.20.1-3.15.5.1

# Apoli + Calio (+ additionalentityattributes) ride inside the Origins all-jar.
unzip -o -j "Dependencies/origins-forge-1.20.1-1.10.0.9.jar" 'META-INF/jarjar/*.jar' -d Dependencies/
ls -la Dependencies/*.jar
