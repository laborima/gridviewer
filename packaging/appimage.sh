#!/usr/bin/env bash
# Builds a single-file AppImage from the Linux archive:
#   packaging/package.sh archive && packaging/appimage.sh
set -euo pipefail
unset LD_PRELOAD
cd "$(dirname "$0")/.."
VERSION=$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' pom.xml | head -1)
VERSION=${VERSION%%-*}
ARCH=$(uname -m)
APPDIR=target/AppDir
rm -rf "$APPDIR"
mkdir -p "$APPDIR/opt"
tar -C "$APPDIR/opt" -xzf "dist/gridviewer-$VERSION-linux-x64.tar.gz"
mv "$APPDIR/opt/gridviewer-$VERSION-linux-x64" "$APPDIR/opt/gridviewer"
cp packaging/linux/gridviewer.png "$APPDIR/gridviewer.png"
cp packaging/linux/gridviewer.png "$APPDIR/.DirIcon"
cat > "$APPDIR/AppRun" <<'SH'
#!/bin/sh
HERE="$(dirname "$(readlink -f "$0")")"
exec "$HERE/opt/gridviewer/bin/gridviewer" "$@"
SH
chmod +x "$APPDIR/AppRun"
cat > "$APPDIR/gridviewer.desktop" <<'DESKTOP'
[Desktop Entry]
Name=Grid Viewer
GenericName=NetCDF Viewer
Comment=Fast viewer for large NetCDF ocean and climate model grids
Exec=gridviewer %f
Icon=gridviewer
Terminal=false
Type=Application
Categories=Science;Education;
MimeType=application/x-netcdf;
DESKTOP
TOOL=target/appimagetool-$ARCH.AppImage
if [[ ! -x $TOOL ]]; then
  curl -fsSL -o "$TOOL" "https://github.com/AppImage/appimagetool/releases/download/continuous/appimagetool-$ARCH.AppImage"
  chmod +x "$TOOL"
fi
ARCH=$ARCH "$TOOL" --appimage-extract-and-run --no-appstream "$APPDIR" "dist/GridViewer-$VERSION-$ARCH.AppImage"
