#!/usr/bin/env python3
"""
VQSV Asset Extractor & Converter
=================================
Reads the original J2ME game JAR and converts its custom binary assets into
open, engine-friendly formats (PNG + JSON) that the modern clients
(LibGDX / Flutter) load directly.

Source formats (reverse-engineered from the original TeaMobi-style engine):
  data/img/img_N.mid   -> PNG image (verified: renamed .png)         -> img/img_N.png      [FULL]
  data/spr/spr_N_all(r)-> sprite frame/module table (big-endian)     -> spr/spr_N.json     [PARTIAL]
  data/map/map_N.mid   -> tile grid (w,h header + tile records)      -> map/map_N.json     [PARTIAL]
  data/ui/NAME.ui      -> UI component tree (magic 0xFFFF)           -> ui/NAME.json       [PARTIAL]

[FULL]    = byte-exact, verified conversion.
[PARTIAL] = header fields are decoded; the remaining stream is captured
            losslessly as raw values so nothing is lost and the structure can
            be refined later. See docs/ASSET-FORMATS.md.

Usage:  python3 extract.py <path-to-game.jar> [out_dir]
Re-run with a newer JAR to regenerate all assets after a game update.
"""
import sys, os, json, struct, zipfile

def u8(b, o):  return b[o], o + 1
def u16(b, o): return (b[o] << 8) | b[o+1], o + 2          # big-endian
def s16(b, o):
    v = (b[o] << 8) | b[o+1]
    return (v - 0x10000 if v & 0x8000 else v), o + 2

def convert_img(raw):
    # img_N.mid files are already valid PNG streams; passthrough.
    return raw if raw[:4] == b'\x89PNG' else None

def convert_map(raw):
    """PARTIAL DECODE. Header: [ver][id][w][h][tileset][typeCount], then a w*h
    tile grid followed by an object/warp trailer. The grid is captured as 4-byte
    records and the trailer is preserved as raw bytes for later refinement."""
    o = 0
    ver, o = u8(raw, o)
    mid, o = u8(raw, o)
    w, o   = u8(raw, o)
    h, o   = u8(raw, o)
    tileset, o = u8(raw, o)
    ntype, o   = u8(raw, o)
    n = w * h
    grid = []
    for i in range(n):
        if o + 4 > len(raw): break
        grid.append(list(raw[o:o+4])); o += 4
    trailer = list(raw[o:])
    return {
        "_partial": True,
        "version": ver, "id": mid, "width": w, "height": h,
        "tileset": tileset, "typeCount": ntype,
        "tileCount": len(grid), "tiles": grid,
        "trailerBytes": len(trailer), "trailer": trailer,
    }

def convert_spr(raw):
    """PARTIAL DECODE. First two shorts are the table header; the remainder is
    the frame/module composition stream referencing clip rects in img_N. The
    exact module grouping is engine-specific, so the header + full short stream
    are captured losslessly here. See docs/ASSET-FORMATS.md."""
    o = 0
    a, o = u16(raw, o)
    nframe, o = u16(raw, o)
    shorts = []
    while o + 2 <= len(raw):
        v, o = s16(raw, o)
        shorts.append(v)
    return {"_partial": True, "header": [a, nframe],
            "frameCount": nframe, "shortCount": len(shorts), "stream": shorts}

def convert_ui(raw):
    """PARTIAL DECODE. UI layout: magic 0xFFFF then a flat component stream,
    captured here as raw shorts."""
    if raw[0] != 0xFF or raw[1] != 0xFF:
        return None
    o = 2
    vals = []
    while o + 2 <= len(raw):
        v, o = u16(raw, o)
        vals.append(v)
    return {"_partial": True, "magic": "0xFFFF", "shortCount": len(vals), "shorts": vals}

def main():
    if len(sys.argv) < 2:
        print(__doc__); sys.exit(1)
    jar = sys.argv[1]
    out = sys.argv[2] if len(sys.argv) > 2 else "out"
    stats = {"img": 0, "spr": 0, "map": 0, "ui": 0, "skip": 0}
    with zipfile.ZipFile(jar) as z:
        for name in z.namelist():
            raw = z.read(name)
            base = os.path.basename(name)
            if name.startswith("data/img/") and base:
                png = convert_img(raw)
                if png is None: stats["skip"] += 1; continue
                p = os.path.join(out, "img", base.replace(".mid", ".png"))
                os.makedirs(os.path.dirname(p), exist_ok=True)
                open(p, "wb").write(png); stats["img"] += 1
            elif name.startswith("data/spr/") and base:
                d = convert_spr(raw)
                idx = base.replace("spr_", "").replace("_all(r)", "")
                p = os.path.join(out, "spr", f"spr_{idx}.json")
                os.makedirs(os.path.dirname(p), exist_ok=True)
                json.dump(d, open(p, "w")); stats["spr"] += 1
            elif name.startswith("data/map/") and base:
                d = convert_map(raw)
                p = os.path.join(out, "map", base.replace(".mid", ".json"))
                os.makedirs(os.path.dirname(p), exist_ok=True)
                json.dump(d, open(p, "w")); stats["map"] += 1
            elif name.startswith("data/ui/") and base:
                d = convert_ui(raw)
                if d is None: stats["skip"] += 1; continue
                p = os.path.join(out, "ui", base.replace(".ui", ".json"))
                os.makedirs(os.path.dirname(p), exist_ok=True)
                json.dump(d, open(p, "w")); stats["ui"] += 1
    print("Converted:", json.dumps(stats))
    return stats

if __name__ == "__main__":
    main()
