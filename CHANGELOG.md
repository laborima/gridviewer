# Changelog

## 2.0.0 — unreleased

Complete rewrite for Java 25 and JavaFX 25, on netCDF-Java 5.11.

- Strided reads matched to the screen resolution, with a coarse base layer and a detail layer
  re-read as you zoom; single I/O thread, latest-request-wins loading and an LRU slice cache.
- NetCDF-3, NetCDF-4/HDF5 and OPeNDAP datasets; CF and NEMO conventions, regular and
  curvilinear grids, fill values, valid ranges, packed data, CF time decoding and climatologies.
- Map view with graticule, value read-out, box zoom, point list and NEMO `layout.dat` overlays.
- Vertical sections along rows and columns, by level or at true depth.
- Rotating 3D globe.
- cmocean and matplotlib colour maps next to the 13 original palettes; automatic colour map and
  range per variable.
- `gridviewer info`, `gridviewer render` and `gridviewer palettes` commands.
- Self-contained packages: deb, rpm, AppImage, snap and tar.gz for Linux, msi and zip for
  Windows, dmg and zip for macOS.

## 1.0 — February 2006

The original Grid Viewer, written at Mercator Océan with Java 1.5, SWT, NetCDF-Java 2.2 and
Java3D. Sources in [`legacy/`](legacy/), tag `legacy-2006`.
