#!/usr/bin/env bash
# Builds the snap from the Linux archive: packaging/package.sh archive && packaging/snap.sh
set -euo pipefail
unset LD_PRELOAD
cd "$(dirname "$0")/.."
VERSION=$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' pom.xml | head -1)
VERSION=${VERSION%%-*}
cp "dist/gridviewer-$VERSION-linux-x64.tar.gz" snap/local/gridviewer-linux-x64.tar.gz
sed -i "s/^version: .*/version: '$VERSION'/" snap/snapcraft.yaml
snapcraft pack --output "dist/gridviewer_${VERSION}_amd64.snap" "$@"
