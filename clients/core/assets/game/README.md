# Game assets (original, converted)

Real converted assets from the original game, committed directly — no JAR needed
to build or run. `com.vqsv.core.asset.GameAssets` loads them as plain files.

```
game/
├── png/img/   img_<id>.png    texture atlases (337 PNGs)
├── png/tex/   tex_<id>.png    rebuilt tile textures
├── spr/       spr_<id>.json   345 sprite tables (modules/frames/anims)
├── map/       map_<id>.json   102 tile maps
├── ui/        *.json          44 UI layouts
├── meta/      sprite_table.json, modInfo.json
└── mod/       mod_<t>.json     tileset tile rects
```

## Regenerate (only when the source game changes)

```bash
python3 tools/asset-extractor/extract.py assets/original/vqsv-original.jar clients/core/assets/game
```
