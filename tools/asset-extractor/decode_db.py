#!/usr/bin/env python3
"""
decode_db.py -- Reverse-engineered decoder for the J2ME game's master data file.

Reads data/script/db.mid and data/script/chs.mid straight out of the original
game JAR and emits structured JSON (species / skills / items) so the server can
be reseeded with the REAL game content.

Stdlib only. No network, no third-party deps.

------------------------------------------------------------------------------
BINARY LAYOUT  (all integers big-endian, signed 16-bit shorts unless noted)
------------------------------------------------------------------------------
db.mid  -- parsed by a/b/c.java#b(String):
              c = new short[9][][];
              for (i=0;i<9;i++) c[i] = a.d.a(inputStream);
          i.e. NINE ragged 2D short tables read back-to-back from one stream.
          Each table (a/d.java#a(InputStream)):
              nRows  : short
              for each row:
                  rowLen : short
                  rowLen * short   (the cell values)

          The 9 tables (indices match c[0]..c[8] in the decompiled code):
            c[0] species   (100 rows x 23 cols)
            c[1] skills     (70 rows x 10 cols)   -- 10 skills per element x 7
            c[2] badges      (8 rows x  7 cols)   -- "...Chương" elemental medals
            c[3] materials  (18 rows, ragged)     -- catch/craft materials + key
            c[4] items      (15 rows, ragged)     -- capture balls + food/potions
            c[5] specials   (11 rows x  3 cols)   -- eggs, riding harnesses, books
            c[6] activeFx   (15 rows x  5 cols)   -- active battle effects
            c[7] statusFx   (11 rows x  3 cols)   -- status ailments / debuffs
            c[8] evolUnlock  (4 rows x  5 cols)   -- evolution skill-bracket table

chs.mid -- parsed by a/d.java#c(InputStream) into String[][], then a/b/c.java#c
           concatenates each group into d[i] (a single String per group):
              nGroups : short
              for each group:
                  nStr : short
                  for each string:
                      len : unsigned byte   (0xFF -> escape, real len = next short)
                      len * short           (each char is a UTF-16 code unit)
           d[i] == concat of group i's strings. db.mid name/desc ids index d[].

------------------------------------------------------------------------------
SPECIES COLUMN MAP  (c[0][species][col]) -- FACT, from game/j.java:
------------------------------------------------------------------------------
  col 0  : name string id          (game/j.java drawString c[0][..][0])
  col 1  : element 0..6            (j.java:1225,1757 element / type chart)
  col 2  : world-ability category  (j.java:1615  values 1/2/3)
  col 3  : base grade (star)       (j.java:78  a.d.b(c[0][W][3], c[0][W][3]))
  col 4  : max grade               (always 4 in data; grade roll ceiling)
  col 5,6,7    : HP  base, perLvl, flat   (j.java:90)
  col 8,9,10   : ATK base, perLvl, flat   (j.java:96)
  col 11,12,13 : DEF base, perLvl, flat   (j.java:102  perLvl uses level/10)
  col 14,15,16 : SPD base, perLvl, flat   (j.java:108  perLvl uses level/10)
  col 17 : evolveInto species id          (j.java:122  this.D = c[0][W][17])
  col 18 : evolution-bracket group (index into c[8])  (j.java:1224/1229)
  col 19,20,21 : auxiliary (undecoded; not directly indexed in code)
  col 22 : rarity / catch-class 0..3      (j.java:1759, a.java:2781
           catch factor = {1000,500,1,1000}[col22]/1000  -> 2 == legendary)

  STAT GROWTH (game/j.java#a):
     grade multiplier N = {90,95,100,110,125}   (grades 1..5)
     HP  = (b + p*level         + f) * N[grade-1] / 100
     ATK = (b + p*level         + f) * N[grade-1] / 100
     DEF = (b + p*(level/10)    + f) * N[grade-1] / 100
     SPD = (b + p*(level/10)    + f) * N[grade-1] / 100

------------------------------------------------------------------------------
SKILL COLUMN MAP  (c[1][skill][col]) -- FACT, from game/a.java & j.java:
------------------------------------------------------------------------------
  col 0 : element group (0..6)  -> species learns skills [elem*10 .. elem*10+9]
  col 1 : name string id
  col 2 : description string id
  col 3 : misc coefficient (e.g. learn weight / accuracy); !=0 gates usability
          (a.java:2383 c[1][E][3]==0 check)
  col 4 : required level / unlock (j.java:1229 c[1][n][4] > c[8][s][bracket])
  col 5 : SP / energy cost          (j.java:1249 z[i]=c[1][by][5])
  col 6 : behavior flag (1 = applies the col7 effect)  (a.java:2543/2547)
  col 7 : effect / status id         (a.java:2525 a((byte)c[1][by][7],...))
  col 8 : power %  (dmg/heal = stat * col8 / 100)  (a.java:2509/2531)
  col 9 : target  (0 = self/ally, else enemy)      (a.java:2504/2548)

------------------------------------------------------------------------------
ITEM-LIKE TABLES (FACT for ids, INFERENCE for some effect semantics):
  c[3] materials : col0 name, col1 item id, col2 desc, col3.. effect values
  c[4] items     : col0 name, col1 item id, col2 desc, col3 price,
                   col4 stack/flag, col5 category, col6 catch power / effect,
                   col7.. extra effect values
  c[5] specials  : col0 name, col1 item id, col2 desc
  c[2] badges    : col0 name, col1 (0), col2 desc, col3 icon?, col4 const,
                   col5 unlock count, col6 value
"""

import io
import json
import os
import struct
import sys
import zipfile

DEFAULT_JAR = "/root/.claude/uploads/d072df75-bc53-5059-acac-eff821a5f9f1/e325eefd-vqsv360x640.jar"
OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "out", "db")

# Grade multiplier table  N[]  from game/j.java line 21.
GRADE_MULT = [90, 95, 100, 110, 125]

# Element names. 7 elements (0..6). Vietnamese gloss from the badge names in
# c[2] ("...Chương" = emblem of that element) -- INFERENCE on English labels.
ELEMENTS = {
    0: "Fire",     # Liệt Hỏa
    1: "Wood",     # Mộc Quỳ
    2: "Earth",    # Đại Địa
    3: "Water",    # Thủy Lam
    4: "Wind",     # Hòa Phong
    5: "Ghost",    # U Linh
    6: "Thunder",  # Lôi Điện
}

# rarity / catch class -> catch factor numerator (a.java:2781 {1000,500,1,1000})
RARITY_CATCH_FACTOR = {0: 1000, 1: 500, 2: 1, 3: 1000}
RARITY_LABEL = {0: "common", 1: "uncommon", 2: "legendary", 3: "common"}


# --------------------------------------------------------------------------- #
# Low level readers
# --------------------------------------------------------------------------- #
def read_db_tables(raw):
    """Decode db.mid into 9 ragged 2D short arrays (mirrors a.d.a x9)."""
    pos = [0]

    def rs():
        v = struct.unpack(">h", raw[pos[0]:pos[0] + 2])[0]
        pos[0] += 2
        return v

    tables = []
    for _t in range(9):
        nrows = rs()
        rows = []
        for _r in range(nrows):
            ln = rs()
            rows.append([rs() for _ in range(ln)])
        tables.append(rows)
    if pos[0] != len(raw):
        sys.stderr.write(
            "WARN: db.mid not fully consumed (%d/%d)\n" % (pos[0], len(raw)))
    return tables


def read_strings(raw):
    """Decode chs.mid -> list[str] where index == group id == d[] in c.java."""
    b = io.BytesIO(raw)

    def rs():
        return struct.unpack(">h", b.read(2))[0]

    def rub():
        return b.read(1)[0]

    ngroups = rs()
    out = []
    for _g in range(ngroups):
        nstr = rs()
        parts = []
        for _s in range(nstr):
            ln = rub()
            if ln == 255:
                ln = rs()
            parts.append("".join(chr(rs() & 0xFFFF) for _ in range(ln)))
        out.append("".join(parts))
    return out


def name_of(strings, sid):
    if sid is None or sid < 0 or sid >= len(strings):
        return None
    return strings[sid]


# --------------------------------------------------------------------------- #
# Decoders
# --------------------------------------------------------------------------- #
def decode_species(c0, strings):
    out = []
    for sid, r in enumerate(c0):
        elem = r[1]
        out.append({
            "id": sid,
            "name": name_of(strings, r[0]),
            "nameId": r[0],
            "element": elem,
            "elementName": ELEMENTS.get(elem),
            "worldAbility": r[2],
            "baseGrade": r[3],
            "maxGrade": r[4],
            "hp":  {"base": r[5],  "perLvl": r[6],  "flat": r[7]},
            "atk": {"base": r[8],  "perLvl": r[9],  "flat": r[10]},
            # DEF/SPD perLvl is applied as perLvl*(level/10) in game/j.java
            "def": {"base": r[11], "perLvl": r[12], "flat": r[13], "perLvlDiv": 10},
            "spd": {"base": r[14], "perLvl": r[15], "flat": r[16], "perLvlDiv": 10},
            "evolveInto": r[17] if r[17] != sid else None,
            "evolveBracketGroup": r[18],
            "rarity": r[22],
            "rarityLabel": RARITY_LABEL.get(r[22]),
            "catchFactorNumerator": RARITY_CATCH_FACTOR.get(r[22]),
            # skills this species can learn (game/j.java: elem*10 .. elem*10+9)
            "skillRange": [elem * 10, elem * 10 + 9],
            "raw": r,
        })
    return out


def decode_skills(c1, strings):
    out = []
    for kid, r in enumerate(c1):
        elem = r[0]
        out.append({
            "id": kid,
            "name": name_of(strings, r[1]),
            "nameId": r[1],
            "description": name_of(strings, r[2]),
            "element": elem,
            "elementName": ELEMENTS.get(elem),
            "coeff3": r[3],
            "requiredLevel": r[4],
            "spCost": r[5],
            "behaviorFlag": r[6],
            "effectId": r[7],
            "power": r[8],
            "target": "self" if r[9] == 0 else "enemy",
            "targetCode": r[9],
            "raw": r,
        })
    return out


def _item(strings, iid, r, table, *, name_col=0, id_col=1, desc_col=2):
    return {
        "id": r[id_col] if id_col < len(r) else iid,
        "rowIndex": iid,
        "table": table,
        "name": name_of(strings, r[name_col]),
        "nameId": r[name_col],
        "description": name_of(strings, r[desc_col]) if desc_col < len(r) else None,
        "raw": r,
    }


def decode_items(tables, strings):
    """Items come from three db tables: c[4] (balls/food), c[3] (materials),
    c[5] (special/quest). Each is tagged with its source table."""
    items = []
    # c[4] -- capture balls + consumables. col3 price, col5 category, col6 power.
    for iid, r in enumerate(tables[4]):
        it = _item(strings, iid, r, "items")
        it["type"] = "consumable"
        it["price"] = r[3] if len(r) > 3 else None
        it["category"] = r[5] if len(r) > 5 else None
        it["effect"] = r[6] if len(r) > 6 else None
        it["effectExtra"] = r[7:] if len(r) > 7 else []
        items.append(it)
    # c[3] -- materials / craft + catch reagents (+ the "Chìa khóa" key).
    for iid, r in enumerate(tables[3]):
        it = _item(strings, iid, r, "materials")
        it["type"] = "material"
        it["effect"] = r[5] if len(r) > 5 else None
        it["effectExtra"] = r[6:] if len(r) > 6 else []
        items.append(it)
    # c[5] -- special / quest items (eggs, riding harnesses, books).
    for iid, r in enumerate(tables[5]):
        it = _item(strings, iid, r, "special")
        it["type"] = "special"
        items.append(it)
    return items


def decode_badges(c2, strings):
    out = []
    for bid, r in enumerate(c2):
        out.append({
            "id": bid,
            "name": name_of(strings, r[0]),
            "description": name_of(strings, r[2]) if len(r) > 2 else None,
            "unlockCount": r[5] if len(r) > 5 else None,
            "value": r[6] if len(r) > 6 else None,
            "raw": r,
        })
    return out


# --------------------------------------------------------------------------- #
def main():
    jar = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_JAR
    z = zipfile.ZipFile(jar)
    db = read_db_tables(z.read("data/script/db.mid"))
    strings = read_strings(z.read("data/script/chs.mid"))

    species = decode_species(db[0], strings)
    skills = decode_skills(db[1], strings)
    items = decode_items(db, strings)
    badges = decode_badges(db[2], strings)

    os.makedirs(OUT_DIR, exist_ok=True)

    def dump(name, obj):
        path = os.path.join(OUT_DIR, name)
        with open(path, "w", encoding="utf-8") as f:
            json.dump(obj, f, ensure_ascii=False, indent=2)
        return path

    dump("species.json", species)
    dump("skills.json", skills)
    dump("items.json", items)
    dump("badges.json", badges)

    # ------------------------------------------------------------------ report
    print("=" * 70)
    print("DB DECODE SUMMARY")
    print("=" * 70)
    print("tables in db.mid : %d" % len(db))
    for i, t in enumerate(db):
        lens = sorted({len(r) for r in t})
        print("  c[%d]: %3d rows, col-lengths=%s" % (i, len(t), lens))
    print("-" * 70)
    print("species : %d" % len(species))
    print("skills  : %d" % len(skills))
    print("items   : %d (balls/food=%d, materials=%d, special=%d)" % (
        len(items), len(db[4]), len(db[3]), len(db[5])))
    print("badges  : %d" % len(badges))
    print("strings : %d" % len(strings))
    print("output  : %s" % OUT_DIR)

    print("-" * 70)
    print("SAMPLE SPECIES:")
    for s in species[:5]:
        print("  #%-3d %-18s elem=%-7s rarity=%-9s HP(%d,%d,%d) ATK(%d,%d,%d) -> evolve=%s" % (
            s["id"], s["name"], s["elementName"], s["rarityLabel"],
            s["hp"]["base"], s["hp"]["perLvl"], s["hp"]["flat"],
            s["atk"]["base"], s["atk"]["perLvl"], s["atk"]["flat"],
            s["evolveInto"]))
    print("SAMPLE SKILLS:")
    for k in skills[:5]:
        print("  #%-3d %-16s elem=%-7s lv>=%d sp=%d power=%s tgt=%s" % (
            k["id"], k["name"], k["elementName"], k["requiredLevel"],
            k["spCost"], k["power"], k["target"]))
    print("SAMPLE ITEMS:")
    for it in items[:5]:
        print("  [%-9s] %-18s id=%s effect=%s" % (
            it["table"], it["name"], it["id"], it.get("effect")))

    # sanity assertions
    assert all(0 <= s["element"] <= 6 for s in species), "bad element id"
    assert all(0 <= s["rarity"] <= 3 for s in species), "bad rarity"
    assert len(skills) == 70, "expected 70 skills (7 elements x 10)"
    print("-" * 70)
    print("sanity checks: OK")


if __name__ == "__main__":
    main()
