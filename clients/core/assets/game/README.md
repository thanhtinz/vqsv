# Bundled game assets

This folder is populated by the asset pipeline — it is intentionally empty in
version control because the converted assets are **derived** from the original
game JAR (and include binary PNGs that are regenerated, not hand-edited).

## Populate

```bash
# 1. Put the original game JAR here:
#      assets/original/vqsv-original.jar
# 2. Run the extractor, pointing output at this folder:
cd tools/asset-extractor
python3 extract.py ../../assets/original/vqsv-original.jar ../../clients/core/assets/game
```

Result:

```
game/
├── img/   img_N.png    (309 texture atlases)
├── spr/   spr_N.json   (345 sprite tables)
└── map/   map_N.json   (102 tile maps)
```

`com.vqsv.core.asset.GameAssets` loads these at runtime. If the folder is empty,
the clients fall back to placeholder rendering and still run.
