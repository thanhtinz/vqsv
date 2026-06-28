# Game assets

The converted original-game assets are **baked into this repo** as a base64-chunked
zip bundle at `clients/core/assets/game.pack.NNN.b64`. No original JAR is needed to
build or run — `com.vqsv.core.asset.GameAssets` unpacks it in memory on first use
(base64 → unzip → textures/JSON).

Bundle contents: 337 PNG atlases, 345 sprite tables, 102 tile maps, 44 UI layouts,
tileset metadata.

## Regenerate (only when the source game changes)

```bash
tools/asset-extractor/pack.sh path/to/game.jar
```

This re-extracts and rewrites the `game.pack.*.b64` chunks; commit them.
