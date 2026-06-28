# VQSV Asset Formats

Byte-by-byte specification of the custom binary asset containers in
`vqsv360x640.jar` (a TeaMobi / mGame-style J2ME engine).

Every field below was reverse-engineered from the decompiled loader classes and
**verified against the real bytes** by `tools/asset-extractor/extract.py`.
Verification result on the shipped JAR:

| format | files | parsed clean (bytes fully consumed) |
|--------|------:|------------------------------------:|
| spr    |   345 | 345 / 345 |
| map    |   102 | 102 / 102 |
| ui     |    44 |  44 /  44 |
| img PNG|   309 | extracted |
| tex PNG|    28 | rebuilt + CRC/IDAT validated |

All multi-byte integers are **big-endian** (Java `DataInputStream`).

Primitive readers (from `a/d.java`):
- `c()` = `readByte` — signed 8-bit
- `d()` = `readShort` — signed 16-bit BE
- `e()` = `readInt` — signed 32-bit BE
- `u8/u16` = unsigned variants
- In `a/d.java a()`/`b()` the count/size headers are read with `f()` which is an
  **unsigned** 16-bit read.

---

## 1. Sprite — `data/spr/spr_<n>_all(r)`

Loader: **`a/b/f.java b(int)`**, using the array readers in **`a/d.java`**
(`a()` = `read_flat`, `b()` = `read_ragged`). Rendering / field semantics
confirmed in **`a/b/g.java`** (see `a(Graphics,int,int,int,byte,int)` and
`d(int)`). The in-memory struct is **`a/b/h.java`** (fields `b,c,d,e,f,g`).

A sprite file is read as **five arrays, in this exact order**:

| order | reader | struct field | meaning |
|------:|--------|--------------|---------|
| 1 | `a()` flat | `h.b` | **modules** (atlas clip rects) |
| 2 | `b()` ragged | `h.e` | **frames** (module placements) |
| 3 | `b()` ragged | `h.f` | **animations** (frame sequences) |
| 4 | `a()` flat | `h.d` | **hit / damage boxes** (per frame) |
| 5 | `a()` flat | `h.c` | **body / collision boxes** (per frame) |

### 1a. `read_flat()` (`a/d.java a()`)
```
u16 rows
u16 cols
rows * cols  × int16   (row-major)
if rows == 0 -> null (no further bytes)
```

### 1b. `read_ragged()` (`a/d.java b()`)
```
u16 count
u16 cols
repeat count:
    u16 len
    len * cols × int16
if count == 0 -> null
```

### 1c. Modules (`h.b`, cols = 5)
Each module is a clip rectangle **into one of the sprite's atlas images**:
```
int16 imgIdx   index into the sprite's image list (see §6)
int16 sx       source x in atlas
int16 sy       source y in atlas
int16 w        width
int16 h        height
```
Drawn by `g.java` as `drawRegion(images[imgIdx], sx, sy, w, h, ...)`
(line `a(Graphics,...)` at `g.java:417`).

### 1d. Frames (`h.e`, cols = 4)
Each frame is a list of module placements (composited to render one frame):
```
int16 module   index into modules
int16 dx       x offset
int16 dy       y offset
int16 flip     transform index 0..7  (maps through g.java tables d/e/f/g;
               flip%2==1 means the module is rotated, swapping w/h)
```
Iterated `+=4` in `g.java a(Graphics,int,int,int,byte,int)`.

### 1e. Animations (`h.f`, cols = 2)
Each animation (a.k.a. "action") is a sequence of timed frames:
```
int16 delay    extra hold ticks for this step
int16 frame    index into frames
```
In `g.java d(int)` the step stride is `<<1` (2 shorts) normally, or `<<2`
(4 shorts) when `h.g` is true. `h.g` is set externally by the caller
(`g.a(int,boolean)`), **not** stored in the file, so the on-disk stride is
always 2.

### 1f. Hit boxes (`h.d`) and Body boxes (`h.c`) (cols = 5)
Flat 5-tuples, bucketed per frame by `a/b/f.java a(short[],int)`:
```
int16 frame    frame index this box belongs to
int16 x
int16 y
int16 w
int16 h
```
`f.a()` groups them into `boxes[frame] += [x,y,w,h]`. Either array may be null.

**Verified** `spr_0_all(r)`: 48 modules, 36 frames, 13 anims, hitboxes present,
bodyboxes null — 2064/2064 bytes consumed. Across the whole JAR, 99.46% of all
5007 module rects fall within the bounds of their resolved atlas PNG.

---

## 2. Sprite → image mapping — `data/script/sprite.mid`

Loader: **`a/b/c.java a()`** → `a.d.a(InputStream)` (`a/d.java:130`).
```
int16 count
repeat count:
    int16 len
    len × int16
```
Row `spriteId` = `[sprFileId, imgId0, imgId1, ...]`.
`g.java a(int,boolean)` uses `row[0]` to pick the `spr_<row[0]>_all(r)` file and
`row[1..]` as the ordered atlas image list (`img_<id>.mid`). A module's `imgIdx`
indexes into `row[1..]`. **Verified**: 345 rows, e.g. sprite 0 → `[0, 100]`.

---

## 3. Map — `data/map/map_<n>.mid`

Loader: **`a/b/d.java a(int)`**.
```
u8  version        1 => coordinates are bytes; otherwise shorts
u8  tileset        -> mod_<tileset>.mid + modInfo row (§4)
if version == 1: u8 width ;  u8 height
else:            u16 width ; u16 height
u8  tileSize       square tile size (both axes)
u8  layerCount
repeat layerCount:
    u8  layerIndex
    u8  layerType      0,1 = tile grid ; 2,3,4 = object list
    u16 entryCount
    if layerType in {0,1}:           # full width×height grid, default -1
        repeat entryCount:
            x  (u8 if v1 else u16)
            y  (u8 if v1 else u16)
            u16 value
            # type 1: grid[x][y] = value   (low 12 bits = tile, bits12-14 flip)
            # type 0: grid[x][y] = value & 0x0FFF
    else:                            # object layer
        repeat entryCount:
            x  (u8 if v1 else u16)
            y  (u8 if v1 else u16)
            u16 value
            object = { tile = value & 0x0FFF,
                       x, y,
                       flip = (value & 0x7000) >> 12 }
```
Tile/flip extraction matches the draw code in `d.java a(Graphics,int)`:
`tile = value & 0xFFF`, `flip = j[(value & 0x7000) >> 12]` where
`j = {0,5,3,6,2,4,1,7}`.

Pixel size = `width*tileSize × height*tileSize`. **Verified** `map_0.mid`:
v1, tileset 1, 23×23 @16px, 5 layers (types 0,1,2,4,3) — fully consumed.

### Tile rendering note
`drawRegion` reads tile `value` as an index into the tileset definition
(`mod_<tileset>.mid`, §4). For layer type 1 the high bits also select a flip
from `j[]`; type 0 is unflipped.

---

## 4. Tilesets — `data/mod/modInfo.mid` and `data/mod/mod_<r>.mid`

Loaders: **`a/b/c.java a()`** (modInfo) and **`a/b/d.java e()`** (mod tiles).

`modInfo.mid` — tileset → image-id list:
```
u8 count
repeat count:
    u8  len
    len × int16          # img ids -> img_<id>.mid
```
**Verified**: 7 tilesets, e.g. tileset 1 → `[0,3,4,12,13,5,214,10093,209]`.

`mod_<r>.mid` — tile rectangles for tileset `r`:
```
int16 count
repeat count:
    int8  imgIdx         # index into the tileset's image list
    int16 x
    int16 y
    int16 w
    int16 h
```
**Verified** `mod_1.mid`: 167 tiles, all 16×16 — fully consumed.

A map tile id indexes this array; `imgIdx` then indexes `modInfo[tileset]`.

---

## 5. UI — `data/ui/*.ui`

Loader: **`c/c.java a(String,int,boolean)`** (header) and the recursive
**`c/c.java a(byte[],int[],b,int,boolean)`** (node tree). Reads use
`a.d.c/d/e` = byte/short/int. Component classes: `c/b.java` (panel),
`c/h.java` + `c/i.java` (image/label), `c/m.java` (component). There is **no
file magic**; `0xFFFF` is only used at runtime as an "empty" sentinel.

### Header (`c/c.java:55-86`)
```
int16  (this.c — read, then discarded / forced to 0)
int16  (skipped)
int8   rootType
int16  rootId
int16  rootX
int16  rootY
int16  rootW
int16  rootH
<panel body>      # root is a panel, parsed by the recursive routine
```

### Panel body (`c/c.java:88-149`)
```
int8 actionCount
repeat actionCount: int8 ×4          # action / key-binding rows [key,cmd,p,p]
<lists>                              # selector lists, see below
int16 childCount
repeat childCount: <node>
```

### Selector lists (`c/c.java:103-148`, class `c/l.java`)
```
int8 listCount
repeat listCount:
    int8  listId
    int8  gridFlag (!=0)
    int16 rows           (l.d  number of item ids)
    int16 itemCount      (l.a)
    int8  h              (l.h)
    int8  i              (l.i)
    repeat itemCount:
        int16 itemId
        int16 strLen
        strLen × int8    -> UTF-16BE chars (a.d.a(byte[]) packs 2 bytes/char)
    int16 stateCount
    repeat stateCount:
        int16 stateIdx
        int16 rectCount
        repeat rectCount: int16 ×5   # [x,y,w,h,arg]
```

### Nodes — first `int8` is the node type
**Type 0 — panel** (`c/c.java:155-182`, class `c/b.java`):
```
int8  type (=0)
int16 id
int16 x
int16 y
int16 w
int16 h
<panel body>            # recurse
```

**Type 1 — image / label** (`c/c.java:184-254`, classes `c/h.java`,`c/i.java`):
```
int8  type (=1)
int16 id
int16 x
int16 y
int16 w
int16 h
int16 textLen
textLen × int8          -> UTF-16BE text (i.a)
int8  anchor            (i.b)
int8  pad               (i.c)
int8  multiline (!=0)   (i.d)
int32 bgFocusColor      (i.e)   ARGB
int32 borderFocusColor  (i.f)
int32 textFocusColor    (i.g)
int16 sprFocus.action   (i.i — sprite ref, action id; <0 => none)
int8  sprFocus.frame    (i.i.a)
int32 bgColor           (i.j)
int32 borderColor       (i.k)
int32 textColor         (i.l)
int16 spr.action        (i.m — sprite ref; <0 => none)
int8  spr.frame         (i.m.a)
int8  icon              (i.h)
int8  a                 (h.a)
int8  b                 (h.b)
```
The two sprite references (`i.i` focused, `i.m` normal) are `c/g.java` handles:
`action` (`int16`, the animation index, negative = absent) and `frame`
(`int8`).

**Type 2 — component / scroll list** (`c/c.java:256-334`, class `c/m.java`):
```
int8  type (=2)
int16 q
int16 x
int16 y
16 × int8               component params (m setters a..p / k..j)
int32 color             (m.a)
int16 sprA.action ; int8 sprA.frame
int16 sprB.action ; int8 sprB.frame
int16 r
int8  mode              0 = computed cells ; 1 = explicit cells
if mode == 1:
    int16 cellCount
    repeat cellCount:   # c/e.java(int,byte,int16,byte,int16,int16)
        int16 slot
        int16 v0
        int8  v1
        int16 v2
        int16 v3
        int16 v4
        int16 v5
```

**Verified**: all 44 `.ui` files parse to a clean end (≤1 trailing byte).

---

## 6. Images — `data/img/img_<n>.mid`, `data/tex/*.mid`, `menu.mid`

Loader: **`a/d.java b(String,String)`** → `g(String)`.

Two storage forms, distinguished by the loader:

### 6a. Raw PNG (`a/d.java g()` early return)
If the name contains `"img_"` or ends with `"menu.mid"`, the bytes are **already
a complete PNG** (`89 50 4E 47 …`) and are used verbatim. The extractor copies
them straight to `.png`.

### 6b. Stripped PNG (`a/d.java f()`)
All other `.mid` images (the `tex/` set, `bk`, `tex_0..3`, etc.) are a
**header-stripped PNG** that the engine rebuilds at load time. On-disk layout:
```
int32 width
int32 height
int8  bitDepth
int8  colorType            # 3 = palette, 6 = RGBA (truecolor+alpha)
if colorType == 3:
    int32 plteLen
    plteLen × int8         # raw PLTE palette bytes
    int32 next
    if next == 0x745243...  (0x7452_4E53? see note)  # actually 1951551059
        # tRNS block, compact form:
        int8 trnsCount      # 0 => 256
        int8 transparentIdx
        # expands to trnsCount alpha bytes: 0 at transparentIdx else 255
        int32 idatLen
    else:
        idatLen = next
else:                       # colorType 6 (and any other)
    int32 idatLen
idatLen × int8             # raw zlib IDAT stream
```
The original `f()` writes a fixed PNG signature, an IHDR with
compression/filter/interlace = 0, the PLTE / tRNS / IDAT chunks above, an empty
IEND, and recomputes every chunk CRC. The extractor reproduces this exactly with
standard `zlib.crc32` per chunk.

> Note on the tRNS marker: the decompiled constant is the signed int
> `1951551059`; that is the big-endian value the file stores to flag a tRNS
> block. The extractor compares against this literal.

**Verified**: all 28 `tex/*.mid` rebuild into valid PNGs — every chunk CRC is
correct and the IDAT decompresses to exactly `height × (1 + width × bpp)` bytes
(bpp = 1 for palette, 4 for RGBA). Example: `bk` = 32×36 RGBA, `tex_0` = 16×16
palette.

---

## Extractor output layout

`python3 tools/asset-extractor/extract.py <jar> <outdir>` writes:
```
<outdir>/meta/sprite_table.json     sprite -> [sprFile, img...]
<outdir>/meta/modInfo.json          tileset -> [img...]
<outdir>/mod/mod_<r>.json           tileset tile rects
<outdir>/spr/spr_<n>.json           modules, frames, anims, hit/body boxes
<outdir>/map/map_<n>.json           layered grids + object layers
<outdir>/ui/<name>.json             component tree
<outdir>/png/img/img_<n>.png        raw atlases
<outdir>/png/tex/<name>.png         rebuilt textures
```

## Open / uncertain points (honest)

- **UI component (type 2) params**: the 16 byte fields are read in the exact
  order the loader consumes them, but their individual gameplay meanings
  (`m.a..p`, `m.k..j`) are not all named; they are preserved as `params[0..15]`.
  The `mode==1` cell tuple field names (`v0..v5`) are likewise positional.
- **Sprite `flip` table**: stored value is 0..7; the engine remaps it through
  four orientation tables in `g.java` (`d/e/f/g`) depending on the requested
  draw direction. For faithful re-rendering use `flip%2==1` ⇒ swap w/h + rotate;
  the precise 8-way mapping lives in `g.java`.
- **Map layer types 3 vs 4**: both are object layers parsed identically here;
  the engine treats them differently only at draw/collision time
  (`d.java a(Graphics,int)` cases 2/3/4 share one branch).
- The ~0.5% of sprite module rects that exceed their default atlas bounds
  correspond to sprites whose atlas image is swapped at runtime
  (`g.java a(int,int,boolean)`); the on-disk data is still correct.
