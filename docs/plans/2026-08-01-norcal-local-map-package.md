# Northern California Local Map Package Plan

> **For Hermes:** Execute with strict TDD for app behavior and preserve app-owned provider-neutral pacenote models.

**Goal:** Prove a real-world Northern California basemap locally on-device for Replay Alpha, without relying on a tile server or an internet connection at runtime.

**Architecture:** Build a regional OSM vector-tile package off-device from Geofabrik's Northern California PBF extract, retain the source/version/license manifest, and use MapLibre Android to render local tiles/style assets. The map package is imported into app-private storage rather than shipped inside the APK. The existing route canvas remains the fallback if the optional package is absent or invalid.

**Data source:** `https://download.geofabrik.de/north-america/us/california/norcal-latest.osm.pbf`. Record download timestamp, source URL, checksum, package size, ODbL/OSM attribution, and generator version. The source extract was reported at approximately 607 MB during initial research; verify actual bytes/checksum after download.

**Tasks:**
1. Append the local-package decision and add `docs/maps/norcal-package-manifest.json` schema/template; do not track source PBF, MBTiles, fonts, sprites, or generated package artifacts.
2. Create a repeatable host-side tile generation script using a pinned tool/image; write generated MBTiles/style assets to ignored `local-maps/` only. Establish actual disk/RAM requirements from a small fixture before a full region build.
3. Add MapLibre Android behind an app-owned `LocalMapPackage` boundary. Keep the runtime without `INTERNET`; require an explicit imported local package and render a visible unavailable/fallback state when it is missing.
4. Add test-first package-manifest and local-style URI validation. No production remote style URL, provider type, credentials, or map data may enter `core-model` or `pacenotes`.
5. Import a real generated NorCal package on a device/emulator, capture map screenshot, and verify visible attribution. Device evidence is required before calling this a real-map milestone.

**Out of scope:** Online tile server, live navigation, routing, use of public OSM tile servers, automatic map downloads, or silently including a multi-gigabyte package in the APK.
