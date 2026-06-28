# VQSV Asset Formats

The original J2ME game stores its art and data in a set of custom binary
containers under `data/` inside the game JAR. This document records what each
format is and how `tools/asset-extractor/extract.py` converts them for the
modern clients.

> Source of truth: `assets/original/vqsv-original.jar`.
> Run the extractor after any game update to regenerate all converted assets —
> derived assets are intentionally **not** committed, only the JAR + the tool.

## Inventory (from the shipped JAR)

| Folder        | Count | Original ext | Content                       |
|---------------|-------|--------------|-------------------------------|
| `data/img/`   | 309   | `.mid`       | PNG images (texture atlases)  |
| `data/spr/`   | 345   | `_all(r)`    | Sprite frame/module tables    |
| `data/map/`   | 102   | `.mid`       | Tile maps (23×23 grids)       |
| `data/ui/`    | 44    | `.ui`        | UI layout component trees     |
| `data/tex/`   | 29    | —            | Tile textures                 |
| `data/script/`| 22    | —            | NPC dialog / battle scripts   |
| `data/sound/` | 8     | —            | Sound effects                 |

All multi-byte integers are **big-endian**.

---

## `img/*.mid` — PNG images  ✅ FULL conversion

Despite the `.mid` extension these are **plain PNG streams** (verified: all 309
files start with the PNG magic `89 50 4E 47` and end with an `IEND` chunk,
8-bit colormap). The extractor simply renames them to `img/img_N.png`. These are
the texture atlases that the sprite tables index into.

## `spr/spr_N_all(r)` — sprite tables  ⚠️ PARTIAL

Big-endian. Layout:

```
u16  header[0]    (table size / image hint)
u16  header[1]    (frame count)
...  frame/module composition stream  (clip rects into img_N atlas)
```

The exact module grouping is engine-specific (TeaMobi-style: each *frame* is
composed of one or more *modules*, where a module is a clip rectangle into the
atlas drawn at an offset). The extractor decodes the header and captures the
remaining short stream **losslessly** (`stream` field). Refine the grouping into
explicit `{module, dx, dy}` records once the original reader is fully mapped —
no information is lost in the meantime.

## `map/map_N.mid` — tile maps  ⚠️ PARTIAL

```
u8   version
u8   id
u8   width          (23 in shipped maps)
u8   height         (23 in shipped maps)
u8   tileset
u8   typeCount
...  width*height tile records (4 bytes each)
...  object / warp / spawn trailer (raw, preserved)
```

The `width*height` grid is captured as 4-byte records (`tiles`). The trailer
(NPC spawns, warps, collision metadata) is preserved as raw bytes (`trailer`)
pending a full structural decode.

## `ui/*.ui` — UI layouts  ⚠️ PARTIAL

```
u16  magic = 0xFFFF
...  flat component stream (positions, sizes, child refs)
```

Captured as a raw short array (`shorts`). These drive the original portrait
(360×640) screens; the modern clients reimplement layout natively (Scene2D /
Flutter widgets) and only need these for pixel-faithful recreation.

---

## Orientation note

Converting the game from portrait (360×640) to landscape (e.g. 800×480) does
**not** require redrawing any asset. Sprites and tile textures are
resolution-independent image clips; only the `.ui` layout coordinates and the
map camera/viewport aspect ratio change. The modern clients already use dynamic
screen dimensions and adapt automatically.
