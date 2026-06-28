#!/usr/bin/env python3
"""
Asset extractor for the TeaMobi/mGame-style J2ME game in vqsv360x640.jar.

Reverse-engineered from the decompiled loader classes.  Every read sequence
below mirrors the byte order of the original Java DataInputStream / byte[]
parsers.  See docs/ASSET-FORMATS.md for the field-by-field spec and the
decompiled method each format came from.

Usage:
    python3 extract.py <jar> <outdir>

stdlib only (zipfile, struct, zlib, json, os).
"""
import json
import os
import struct
import sys
import zipfile
import zlib


# ---------------------------------------------------------------------------
# Reader helpers (big-endian, matching java DataInputStream / a.d.java)
#   c() -> readByte (signed)   d() -> readShort (signed)   e() -> readInt
# ---------------------------------------------------------------------------
class Reader:
    def __init__(self, buf):
        self.b = buf
        self.p = 0

    def u8(self):
        v = self.b[self.p]
        self.p += 1
        return v

    def c(self):  # signed byte
        v = self.u8()
        return v - 256 if v >= 128 else v

    def u16(self):
        v = (self.b[self.p] << 8) | self.b[self.p + 1]
        self.p += 2
        return v

    def d(self):  # signed short
        v = self.u16()
        return v - 65536 if v >= 32768 else v

    def e(self):  # signed int
        v = (self.b[self.p] << 24) | (self.b[self.p + 1] << 16) | \
            (self.b[self.p + 2] << 8) | self.b[self.p + 3]
        self.p += 4
        return v - (1 << 32) if v >= (1 << 31) else v

    def bytes(self, n):
        v = self.b[self.p:self.p + n]
        self.p += n
        return bytes(v)


# ===========================================================================
# SPRITE  (data/spr/spr_<n>_all(r))      Source: a/b/f.java b()
# ===========================================================================
def read_flat(r):
    """a/d.java a(): rows=u16, cols=u16, then rows*cols signed shorts."""
    rows = r.u16()
    cols = r.u16()
    if rows == 0:
        return None, 0
    return [[r.d() for _ in range(cols)] for _ in range(rows)], cols


def read_ragged(r):
    """a/d.java b(): count=u16, cols=u16, then per row len=u16 + len*cols
       signed shorts."""
    count = r.u16()
    cols = r.u16()
    if count == 0:
        return None, 0
    out = []
    for _ in range(count):
        ln = r.u16()
        out.append([r.d() for _ in range(ln * cols)])
    return out, cols


def bucket_boxes(flat_rows, nframes):
    """a/b/f.java a(short[],int): bucket flat [frame,x,y,w,h] per frame."""
    if flat_rows is None:
        return None
    boxes = [[] for _ in range(nframes)]
    for row in flat_rows:
        fr = row[0]
        if 0 <= fr < nframes:
            boxes[fr].append([row[1], row[2], row[3], row[4]])
    return boxes


def parse_spr(buf):
    r = Reader(buf)
    # b : modules (5 cols : imgIdx, sx, sy, w, h) - clip rects into the atlas
    mod_rows, _ = read_flat(r)
    modules = []
    if mod_rows:
        for row in mod_rows:
            modules.append({"img": row[0], "x": row[1], "y": row[2],
                            "w": row[3], "h": row[4]})
    # e : frames (4 cols per module ref : moduleIdx, dx, dy, flip)
    frame_rows, _ = read_ragged(r)
    frames = []
    if frame_rows:
        for row in frame_rows:
            frames.append([
                {"module": row[i], "dx": row[i + 1], "dy": row[i + 2],
                 "flip": row[i + 3]} for i in range(0, len(row), 4)])
    # f : animations (2 cols per entry : delay, frameIdx)
    anim_rows, _ = read_ragged(r)
    anims = []
    if anim_rows:
        for row in anim_rows:
            anims.append([{"delay": row[i], "frame": row[i + 1]}
                          for i in range(0, len(row), 2)])
    # d : hit/damage boxes ; c : body/collision boxes (flat, per-frame buckets)
    d_flat, _ = read_flat(r)
    hitboxes = bucket_boxes(d_flat, len(frames))
    c_flat, _ = read_flat(r)
    bodyboxes = bucket_boxes(c_flat, len(frames))
    return {
        "modules": modules, "frames": frames, "anims": anims,
        "hitboxes": hitboxes, "bodyboxes": bodyboxes,
        "bytes_consumed": r.p, "bytes_total": len(buf),
    }


# ===========================================================================
# Support tables
# ===========================================================================
def parse_short_table(buf):
    """sprite.mid: count=short, per row len=short + shorts.  a/d.java a(IS)."""
    r = Reader(buf)
    n = r.d()
    out = []
    for _ in range(n):
        ln = r.d()
        out.append([r.d() for _ in range(ln)])
    return out


def parse_modinfo(buf):
    """modInfo.mid: count=byte, per row len=byte + shorts.  a/b/c.java a()."""
    r = Reader(buf)
    n = r.u8()
    out = []
    for _ in range(n):
        ln = r.u8()
        out.append([r.d() for _ in range(ln)])
    return out


def parse_mod_tiles(buf):
    """mod_<r>.mid: count=short, rows [imgIdx(byte), x,y,w,h(short)].
       Source: a/b/d.java e()."""
    r = Reader(buf)
    n = r.d()
    return [{"img": r.c(), "x": r.d(), "y": r.d(), "w": r.d(), "h": r.d()}
            for _ in range(n)]


# ===========================================================================
# MAP  (data/map/map_<n>.mid)        Source: a/b/d.java a(int)
# ===========================================================================
def parse_map(buf):
    r = Reader(buf)
    ver = r.u8()            # 1 => byte coords, else short coords
    tileset = r.u8()       # -> mod_<tileset>.mid / modInfo row
    if ver == 1:
        w, h = r.u8(), r.u8()
    else:
        w, h = r.u16(), r.u16()
    tile = r.u8()          # square tile size (o == p)
    nlayers = r.u8()
    layers = []
    for _ in range(nlayers):
        idx = r.u8()
        ltype = r.u8()
        count = r.u16()
        layer = {"index": idx, "type": ltype, "count": count}
        if ltype in (0, 1):
            grid = [[-1] * h for _ in range(w)]
            for _ in range(count):
                x = r.u8() if ver == 1 else r.u16()
                y = r.u8() if ver == 1 else r.u16()
                v = r.u16()
                grid[x][y] = v if ltype == 1 else (v & 0xFFF)
            layer["grid"] = grid
        else:  # object layers 2/3/4
            objs = []
            for _ in range(count):
                x = r.u8() if ver == 1 else r.u16()
                y = r.u8() if ver == 1 else r.u16()
                v = r.u16()
                objs.append({"tile": v & 0xFFF, "x": x, "y": y,
                             "flip": (v & 0x7000) >> 12})
            layer["objects"] = objs
        layers.append(layer)
    return {
        "version": ver, "tileset": tileset, "width": w, "height": h,
        "tile_size": tile, "px_width": w * tile, "px_height": h * tile,
        "layers": layers, "bytes_consumed": r.p, "bytes_total": len(buf),
    }


# ===========================================================================
# UI  (data/ui/*.ui)                 Source: c/c.java a(String,int,boolean)
#   node types: 0 = panel/container, 1 = image/label, 2 = component
# ===========================================================================
def _utf16be(b):
    if len(b) % 2:
        b = b + b"\x00"
    try:
        return b.decode("utf-16-be")
    except Exception:
        return b.hex()


def parse_ui_lists(r):
    """Per-panel selector 'l' lists.  Source: c/c.java lines 103-148."""
    out = []
    s = r.c()  # number of lists (byte)
    for _ in range(s):
        lid = r.c()
        gflag = r.c() != 0
        rows = r.d()       # l.d (number of item ids)
        la = r.d()         # l.a (items stored)
        ch = r.c()
        ci = r.c()
        items = []
        for _ in range(la):
            item_id = r.d()
            blen = r.d()
            items.append({"id": item_id, "text": _utf16be(r.bytes(blen))})
        states = []
        nstates = r.d()
        for _ in range(nstates):
            st = r.d()
            n8 = r.d()
            rects = [[r.d(), r.d(), r.d(), r.d(), r.d()] for _ in range(n8)]
            states.append({"state": st, "rects": rects})
        out.append({"id": lid, "grid": gflag, "rows": rows, "h": ch, "i": ci,
                    "items": items, "states": states})
    return out


def parse_ui_panel(r):
    """c/c.java a(byte[],int[],b,int,boolean) - recursive."""
    node = {}
    n5 = r.c()                # action table count (byte)
    actions = []
    if n5 > 0:
        for _ in range(n5):
            actions.append([r.c(), r.c(), r.c(), r.c()])
    node["actions"] = actions
    node["lists"] = parse_ui_lists(r)

    children = []
    n4 = r.d()                # child count (short)
    for _ in range(n4):
        t = r.c()
        if t == 0:            # panel
            ch = {"type": "panel", "id": r.d(), "x": r.d(), "y": r.d(),
                  "w": r.d(), "h": r.d()}
            ch.update(parse_ui_panel(r))
            children.append(ch)
        elif t == 1:          # image / label
            ch = {"type": "image", "id": r.d(), "x": r.d(), "y": r.d(),
                  "w": r.d(), "h": r.d()}
            blen = r.d()
            ch["text"] = _utf16be(r.bytes(blen))
            ch["anchor"] = r.c()        # i.b
            ch["pad"] = r.c()           # i.c
            ch["multiline"] = r.c() != 0  # i.d
            ch["bg_focus"] = r.e()      # i.e
            ch["border_focus"] = r.e()  # i.f
            ch["text_focus"] = r.e()    # i.g
            ch["spr_focus"] = {"action": r.d(), "frame": r.c()}  # i.i
            ch["bg"] = r.e()            # i.j
            ch["border"] = r.e()        # i.k
            ch["text_color"] = r.e()    # i.l
            ch["spr"] = {"action": r.d(), "frame": r.c()}        # i.m
            ch["icon"] = r.c()          # i.h
            ch["a"] = r.c()             # h.a
            ch["b"] = r.c()             # h.b
            children.append(ch)
        elif t == 2:          # component (scroll list / grid)
            ch = {"type": "component", "q": r.d(), "x": r.d(), "y": r.d()}
            ch["params"] = [r.c() for _ in range(16)]
            ch["color"] = r.e()         # m.a
            ch["spr_a"] = {"action": r.d(), "frame": r.c()}
            ch["spr_b"] = {"action": r.d(), "frame": r.c()}
            ch["r"] = r.d()
            mode = r.c()
            ch["mode"] = mode
            if mode == 1:
                cells = []
                n9 = r.d()
                for _ in range(n9):
                    slot = r.d()
                    cells.append({"slot": slot, "v0": r.d(), "v1": r.c(),
                                  "v2": r.d(), "v3": r.d(), "v4": r.d(),
                                  "v5": r.d()})
                ch["cells"] = cells
            children.append(ch)
        else:
            raise ValueError("unknown UI node type %d at %d" % (t, r.p))
    node["children"] = children
    return node


def parse_ui(buf):
    r = Reader(buf)
    r.d()  # this.c (read then forced to 0)
    r.d()  # skipped short
    root = {"type": "panel", "root_type": r.c(), "id": r.d(),
            "x": r.d(), "y": r.d(), "w": r.d(), "h": r.d()}
    root.update(parse_ui_panel(r))
    root["bytes_consumed"] = r.p
    root["bytes_total"] = len(buf)
    return root


# ===========================================================================
# IMAGES
#   img_<n>.mid / menu.mid : already a complete PNG  (a/d.java g())
#   tex/*.mid + others     : stripped PNG, rebuilt by a/d.java f()
# ===========================================================================
PNG_SIG = b"\x89PNG\r\n\x1a\n"


def is_png(buf):
    return buf[:8] == PNG_SIG


def _chunk(typ, data):
    return (struct.pack(">I", len(data)) + typ + data +
            struct.pack(">I", zlib.crc32(typ + data) & 0xFFFFFFFF))


def rebuild_png(buf):
    """Port of a/d.java f().  Rebuilds a standard PNG from the stripped form."""
    r = Reader(buf)
    w = r.e()
    h = r.e()
    bit_depth = r.u8()
    color_type = r.u8()
    out = bytearray(PNG_SIG)
    out += _chunk(b"IHDR",
                  struct.pack(">iiBBBBB", w, h, bit_depth, color_type, 0, 0, 0))
    if color_type == 3:
        plte_len = r.e()
        out += _chunk(b"PLTE", r.bytes(plte_len))
        nxt = r.e()
        if nxt == 1951551059:  # 'tRNS'
            n6 = r.u8()
            if n6 == 0:
                n6 = 256
            transp_idx = r.u8()
            out += _chunk(b"tRNS",
                          bytes(0 if i == transp_idx else 255
                                for i in range(n6)))
            idat_len = r.e()
        else:
            idat_len = nxt
    else:  # color_type 6 (RGBA) and others store IDAT length next
        idat_len = r.e()
    out += _chunk(b"IDAT", r.bytes(idat_len))
    out += _chunk(b"IEND", b"")
    return bytes(out)


# ===========================================================================
# Driver
# ===========================================================================
def main():
    if len(sys.argv) != 3:
        print("usage: python3 extract.py <jar> <outdir>")
        sys.exit(1)
    jar_path, outdir = sys.argv[1], sys.argv[2]
    z = zipfile.ZipFile(jar_path)
    names = set(z.namelist())

    def out(*parts):
        p = os.path.join(outdir, *parts)
        os.makedirs(os.path.dirname(p), exist_ok=True)
        return p

    stats = {"spr": 0, "spr_clean": 0, "map": 0, "map_clean": 0,
             "ui": 0, "ui_clean": 0, "img_png": 0, "tex_png": 0,
             "img_fail": 0}

    # ---- support tables ----
    sprite_table = None
    if "data/script/sprite.mid" in names:
        sprite_table = parse_short_table(z.read("data/script/sprite.mid"))
        json.dump(sprite_table, open(out("meta", "sprite_table.json"), "w"))
    if "data/mod/modInfo.mid" in names:
        json.dump(parse_modinfo(z.read("data/mod/modInfo.mid")),
                  open(out("meta", "modInfo.json"), "w"))
    for n in sorted(x for x in names if x.startswith("data/mod/mod_")
                    and x.endswith(".mid")):
        base = os.path.splitext(os.path.basename(n))[0]
        json.dump(parse_mod_tiles(z.read(n)),
                  open(out("mod", base + ".json"), "w"))

    # ---- sprites ----
    sample_spr = None
    for n in sorted(x for x in names if x.startswith("data/spr/spr_")):
        try:
            spr = parse_spr(z.read(n))
        except Exception as ex:
            print("SPR FAIL", n, ex)
            continue
        sid = n.split("spr_")[1].split("_")[0]
        if sprite_table and sid.isdigit() and int(sid) < len(sprite_table):
            row = sprite_table[int(sid)]
            spr["spr_file_id"] = row[0]
            spr["images"] = row[1:]
        json.dump(spr, open(out("spr", "spr_%s.json" % sid), "w"))
        stats["spr"] += 1
        if spr["bytes_total"] - spr["bytes_consumed"] <= 1:
            stats["spr_clean"] += 1
        if sample_spr is None and spr["frames"]:
            sample_spr = (sid, spr)

    # ---- maps ----
    sample_map = None
    for n in sorted(x for x in names if x.startswith("data/map/map_")):
        try:
            mp = parse_map(z.read(n))
        except Exception as ex:
            print("MAP FAIL", n, ex)
            continue
        mid = os.path.splitext(os.path.basename(n))[0]
        json.dump(mp, open(out("map", mid + ".json"), "w"))
        stats["map"] += 1
        if mp["bytes_total"] - mp["bytes_consumed"] <= 1:
            stats["map_clean"] += 1
        if sample_map is None:
            sample_map = (mid, mp)

    # ---- ui ----
    sample_ui = None
    for n in sorted(x for x in names if x.startswith("data/ui/")
                    and x.endswith(".ui")):
        try:
            ui = parse_ui(z.read(n))
        except Exception as ex:
            print("UI FAIL", n, ex)
            continue
        base = os.path.splitext(os.path.basename(n))[0]
        json.dump(ui, open(out("ui", base + ".json"), "w"))
        stats["ui"] += 1
        if ui["bytes_total"] - ui["bytes_consumed"] <= 1:
            stats["ui_clean"] += 1
        if sample_ui is None:
            sample_ui = (base, ui)

    # ---- images ----
    for n in sorted(x for x in names if (x.startswith("data/img/")
                    or x.startswith("data/tex/")) and x.endswith(".mid")):
        buf = z.read(n)
        base = os.path.splitext(os.path.basename(n))[0]
        sub = "img" if n.startswith("data/img/") else "tex"
        try:
            if is_png(buf):
                png = buf
                stats["img_png" if sub == "img" else "tex_png"] += 1
            else:
                png = rebuild_png(buf)
                stats["tex_png"] += 1
            open(out("png", sub, base + ".png"), "wb").write(png)
        except Exception as ex:
            stats["img_fail"] += 1
            print("IMG FAIL", n, ex)

    # ---- report ----
    print("\n=== extraction stats ===")
    for k in sorted(stats):
        print("  %-12s %d" % (k, stats[k]))

    if sample_spr:
        sid, spr = sample_spr
        print("\n=== sample sprite spr_%s ===" % sid)
        print("  modules:%d frames:%d anims:%d  images:%s" %
              (len(spr["modules"]), len(spr["frames"]), len(spr["anims"]),
               spr.get("images")))
        print("  module[0]:", spr["modules"][0])
        print("  frame[0] (%d refs):" % len(spr["frames"][0]))
        for ref in spr["frames"][0][:4]:
            m = spr["modules"][ref["module"]] \
                if ref["module"] < len(spr["modules"]) else None
            print("    ref %s -> atlas rect %s" % (ref, m))
    if sample_map:
        mid, mp = sample_map
        print("\n=== sample map %s ===" % mid)
        print("  %dx%d tiles @%dpx (=%dx%d px) tileset %d %d layers" %
              (mp["width"], mp["height"], mp["tile_size"], mp["px_width"],
               mp["px_height"], mp["tileset"], len(mp["layers"])))
        for L in mp["layers"]:
            kind = "grid" if "grid" in L else \
                "objects(%d)" % len(L.get("objects", []))
            print("    layer %d type %d : %s" % (L["index"], L["type"], kind))
    if sample_ui:
        base, ui = sample_ui
        print("\n=== sample ui %s ===" % base)
        print("  root id %d %dx%d children %d" %
              (ui["id"], ui["w"], ui["h"], len(ui["children"])))
        for c in ui["children"][:5]:
            print("    %s id=%s xywh=(%s,%s,%s,%s) text=%r" % (
                c["type"], c.get("id"), c.get("x"), c.get("y"),
                c.get("w"), c.get("h"), c.get("text", "")[:24]))


if __name__ == "__main__":
    main()
