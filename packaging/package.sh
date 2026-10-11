#!/usr/bin/env bash
# Builds a self-contained Grid Viewer: no Java needed on the target machine.
#
#   packaging/package.sh [TYPE...]
#
# TYPE is any jpackage type for the current OS (app-image, deb, rpm, msi, exe, dmg, pkg),
# plus "archive" (app-image as .tar.gz or .zip). Default: archive.
# Output goes to dist/. Requires JDK 25 (jlink, jpackage) on PATH or JAVA_HOME; rpm needs
# rpmbuild, msi needs WiX.
set -euo pipefail
# Works with the bash 3.2 of macOS: empty arrays are expanded as ${A[@]+"${A[@]}"}.
# A preloaded library would leak into the computed package dependencies.
unset LD_PRELOAD

cd "$(dirname "$0")/.."
ROOT=$(pwd)
TYPES=("${@:-archive}")

if [[ -n "${JAVA_HOME:-}" ]]; then
  export PATH="$JAVA_HOME/bin:$PATH"
fi

VERSION=$(sed -n 's:.*<version>\(.*\)</version>.*:\1:p' pom.xml | head -1)
APP_VERSION=${VERSION%%-*}
FX_VERSION=$(sed -n 's:.*<javafx.version>\(.*\)</javafx.version>.*:\1:p' pom.xml)

case "$(uname -s)" in
  Linux*) OS=linux ;;
  Darwin*) OS=mac ;;
  MINGW* | MSYS* | CYGWIN*) OS=windows ;;
  *) echo "Unsupported OS $(uname -s)" >&2; exit 1 ;;
esac
case "$(uname -m)" in
  x86_64 | amd64) ARCH=x64 ;;
  arm64 | aarch64) ARCH=aarch64 ;;
  *) echo "Unsupported architecture $(uname -m)" >&2; exit 1 ;;
esac
FX_OS=$OS; [[ $OS == mac ]] && FX_OS=osx

echo "Grid Viewer $VERSION for $OS-$ARCH (JavaFX $FX_VERSION)"

# 1. Application jar and dependencies.
./mvnw -B -q -DskipTests package

# 2. JavaFX as jmods, so the runtime gets it as real modules with their native libraries.
FX_DIR="target/javafx-jmods-$FX_VERSION-$FX_OS-$ARCH"
if [[ ! -d $FX_DIR ]]; then
  ZIP="target/openjfx-$FX_VERSION-jmods.zip"
  curl -fsSL -o "$ZIP" "https://download2.gluonhq.com/openjfx/$FX_VERSION/openjfx-${FX_VERSION}_${FX_OS}-${ARCH}_bin-jmods.zip"
  unzip -q -o "$ZIP" -d target/javafx-unzip
  mkdir -p "$FX_DIR"
  mv target/javafx-unzip/*/*.jmod "$FX_DIR"/
  rm -rf target/javafx-unzip "$ZIP"
fi

# 3. Minimal Java runtime: the modules the app and netCDF-Java need, plus JavaFX.
rm -rf target/runtime
jlink --module-path "$FX_DIR" \
  --add-modules java.base,java.desktop,java.logging,java.management,java.naming,java.net.http,java.prefs,java.security.jgss,java.sql,java.xml,jdk.charsets,jdk.crypto.ec,jdk.localedata,jdk.unsupported,javafx.base,javafx.graphics,javafx.controls \
  --strip-debug --no-header-files --no-man-pages --compress zip-6 \
  --output target/runtime

# 4. Application files, without the JavaFX jars (they are in the runtime now).
rm -rf target/jpackage-input
mkdir -p target/jpackage-input/lib
cp target/gridviewer.jar target/jpackage-input/
find target/lib -name '*.jar' ! -name 'javafx-*' -exec cp {} target/jpackage-input/lib/ \;

mkdir -p dist
COMMON=(
  --app-version "$APP_VERSION"
  --vendor "Matthieu Laborie"
  --copyright "Copyright 2006-2026 Matthieu Laborie"
  --description "Fast viewer for large NetCDF ocean and climate model grids"
  --input target/jpackage-input
  --main-jar gridviewer.jar
  --main-class io.github.laborima.gridviewer.Main
  --runtime-image target/runtime
  --java-options "--enable-native-access=javafx.graphics,ALL-UNNAMED"
  --java-options "-XX:+UseZGC"
  --dest dist
)
case $OS in
  linux)
    NAME=gridviewer
    PLATFORM=(--icon packaging/linux/gridviewer.png)
    INSTALLER=(--linux-package-name gridviewer --linux-app-category science --linux-menu-group "Science;Education;"
      --linux-shortcut --about-url https://github.com/laborima/gridviewer
      --linux-deb-maintainer "laborima@gmail.com" --linux-rpm-license-type "Apache-2.0"
      --file-associations packaging/linux/netcdf.properties --resource-dir packaging/linux)
    ;;
  mac)
    NAME="Grid Viewer"
    PLATFORM=(--icon packaging/macos/gridviewer.icns --mac-package-identifier io.github.laborima.gridviewer
      --mac-package-name "Grid Viewer")
    INSTALLER=(--file-associations packaging/macos/netcdf.properties)
    ;;
  windows)
    NAME="Grid Viewer"
    PLATFORM=(--icon packaging/windows/gridviewer.ico)
    INSTALLER=(--win-menu --win-menu-group "Grid Viewer" --win-shortcut --win-dir-chooser
      --win-upgrade-uuid 6a3f6a1e-7d43-4c0e-9a51-3e8c2b5d9f10
      --file-associations packaging/windows/netcdf.properties)
    # A console launcher for the command line (info, render), next to the windowed one.
    PLATFORM+=(--add-launcher gridviewer-cli=packaging/windows/cli-launcher.properties)
    ;;
esac

for TYPE in "${TYPES[@]}"; do
  echo "--> $TYPE"
  if [[ $TYPE == archive || $TYPE == app-image ]]; then
    rm -rf "dist/$NAME" "dist/$NAME.app"
    jpackage --type app-image --name "$NAME" "${COMMON[@]}" "${PLATFORM[@]}"
    if [[ $TYPE == archive ]]; then
      BASE="gridviewer-$APP_VERSION-$OS-$ARCH"
      case $OS in
        linux)
          rm -rf "dist/$BASE" && mv "dist/$NAME" "dist/$BASE"
          tar -C dist -czf "dist/$BASE.tar.gz" "$BASE" && rm -rf "dist/$BASE" ;;
        mac)
          (cd dist && zip -qry "$BASE.zip" "$NAME.app") && rm -rf "dist/$NAME.app" ;;
        windows)
          (cd dist && powershell -NoProfile -Command "Compress-Archive -Force -Path '$NAME' -DestinationPath '$BASE.zip'") \
            && rm -rf "dist/$NAME" ;;
      esac
    fi
  else
    EXTRA=()
    if [[ $TYPE == rpm ]]; then
      # Library-level requirements work across Fedora, RHEL and openSUSE package names.
      EXTRA=(--linux-package-deps "libgtk-3.so.0()(64bit), libGL.so.1()(64bit), libasound.so.2()(64bit), libXtst.so.6()(64bit), xdg-utils")
    fi
    jpackage --type "$TYPE" --name "$NAME" "${COMMON[@]}" "${PLATFORM[@]}" "${INSTALLER[@]}" ${EXTRA[@]+"${EXTRA[@]}"} \
      --license-file LICENSE
  fi
done

ls -la dist
