# ORIGINAL-DATA.md — db.mid / chs.mid binary layout and decoded content

Reverse-engineered from the original J2ME JAR
(`e325eefd-vqsv360x640.jar`) and its CFR-decompiled sources.

Decoder: `tools/asset-extractor/decode_db.py` (stdlib only).
Output:  `tools/asset-extractor/out/db/{species,skills,items,badges}.json`.

Everything below is split into **FACT** (read directly from the decompiled
loader/consumer code or from the raw bytes) and **INFERENCE** (interpretation
of values whose meaning is not pinned by a code site).

---

## 1. The loader (FACT)

`data/script/db.mid` and `data/script/chs.mid` are loaded by
`a/b/c.java` (the resource-table class). Relevant fields:

```java
public static short[][][] c;   // db.mid  -> c[0..8]
public static String[]   d;    // chs.mid -> string table
```

`a/b/c.java#a()` (the bootstrap) calls:

```java
a.b.c.c("/data/script/chs.mid");   // -> d[]   (string table)
a.b.c.b("/data/script/db.mid");    // -> c[][][] (9 tables)
```

### db.mid (FACT) — `a/b/c.java#b(String)`

```java
private static void b(String object) {
    object = a.d.a(object);          // open InputStream
    c = new short[9][][];
    for (int i = 0; i < 9; ++i)
        c[i] = a.d.a((InputStream) object);   // read one ragged table per i
}
```

So db.mid is **nine ragged 2D `short` tables read back-to-back from one
stream**. Each table is parsed by `a/d.java#a(InputStream)`:

```java
public static short[][] a(InputStream in) {
    DataInputStream dis = new DataInputStream(in);
    int n = dis.readShort();                 // number of rows
    short[][] out = new short[n][];
    for (int i = 0; i < out.length; i++) {
        n = dis.readShort();                 // row length (number of cells)
        out[i] = new short[n];
        for (int j = 0; j < out[i].length; j++)
            out[i][j] = dis.readShort();     // each cell, big-endian short
    }
    return out;
}
```

Byte grammar (all big-endian, signed 16-bit):

```
db.mid := table{9}
table  := nRows:short  row{nRows}
row    := rowLen:short  cell:short{rowLen}
```

The whole 7386-byte file is consumed exactly (verified by the decoder).

Cell accessor (FACT) — `a/b/c.java`:
```java
public static short a(byte t, short row, byte col) { return c[t][row][col]; }
// c[table][row][col]
```

### chs.mid (FACT) — `a/d.java#c(InputStream)` then `a/b/c.java#c(String)`

`a/d.java#c` reads `String[][]`:

```
chs.mid := nGroups:short  group{nGroups}
group   := nStr:short      str{nStr}
str     := len:ubyte                    // if len==0xFF, real len = next short
           char:short{len}              // each char is one UTF-16 code unit
```

`a/b/c.java#c` then **concatenates every string of a group into one string**,
so `d[i]` = the concatenation of group `i`’s strings. Name/description ids
stored in db.mid index directly into `d[]`. Total groups = **636**.

---

## 2. The nine tables (`c[0..8]`)

| idx  | rows | shape        | meaning                                   | FACT basis |
|------|------|--------------|-------------------------------------------|------------|
| c[0] | 100  | 23 cols      | **species** stats / evolution             | game/j.java stat formula |
| c[1] | 70   | 10 cols      | **skills** (10 per element × 7 elements)  | game/j.java:1227 `elem*10..elem*10+9` |
| c[2] | 8    | 7 cols       | **badges / medals** (“…Chương”)           | chs strings 187–194 |
| c[3] | 18   | ragged 5–7   | **materials** / catch reagents (+ Key)    | chs strings 213–229,363 |
| c[4] | 15   | ragged 6–9   | **items**: capture balls + food/potions   | chs strings 261–266 |
| c[5] | 11   | 3 cols       | **special / quest items** (eggs, harness) | chs strings 295–300 |
| c[6] | 15   | 5 cols       | **active battle effects**                 | chs strings 333–338 |
| c[7] | 11   | 3 cols       | **status ailments / debuffs**             | chs strings 311–316 |
| c[8] | 4    | 5 cols       | **evolution skill-bracket unlock table**  | game/j.java:1229 `c[8][group][stage]` |

(Counts are FACT, from the raw byte parse. Labels for c[0],c[1],c[8] are FACT;
labels for c[2]–c[7] are INFERENCE from the strings their col0 points at.)

---

## 3. Species column map — `c[0][species][col]` (FACT)

Every column below is pinned to an indexing site in `game/j.java`
(stat math) or `game/a.java` / `game/k.java` (gameplay).

| col   | meaning                       | code site |
|-------|-------------------------------|-----------|
| 0     | name string id (→ `d[]`)      | `game/a.java:1692` `a.b.c.c[0][..][0]` used as name |
| 1     | element 0–6                   | `j.java:1225,1757` element & type chart |
| 2     | world-ability category (1/2/3)| `j.java:1615-1621` |
| 3     | base grade (star) roll        | `j.java:78` `a.d.b(c[0][W][3], c[0][W][3])` |
| 4     | max grade (=4 in data)        | grade ceiling |
| 5,6,7 | **HP** base, perLvl, flat     | `j.java:90` |
| 8,9,10| **ATK** base, perLvl, flat    | `j.java:96` |
|11,12,13| **DEF** base, perLvl, flat   | `j.java:102` (perLvl × level/10) |
|14,15,16| **SPD** base, perLvl, flat   | `j.java:108` (perLvl × level/10) |
| 17    | evolveInto species id         | `j.java:122` `this.D = c[0][W][17]` |
| 18    | evolution-bracket group (→c[8])| `j.java:1224/1229` |
| 19,20,21| auxiliary (UNDECODED)       | not indexed anywhere in code |
| 22    | rarity / catch-class 0–3      | `j.java:1759`, `a.java:2781` |

### Stat growth formula (FACT) — `game/j.java#a(...)`

Grade multiplier `N = {90,95,100,110,125}` for grades 1..5 (`j.java:21`).

```
HP  = (c5  + c6 *level      + c7 ) * N[grade-1] / 100
ATK = (c8  + c9 *level      + c10) * N[grade-1] / 100
DEF = (c11 + c12*(level/10) + c13) * N[grade-1] / 100
SPD = (c14 + c15*(level/10) + c16) * N[grade-1] / 100
```
(`level/10` is integer division; quotes lines 90/96/102/108.)

### Catch-rate (FACT) — `game/a.java:2781`

```java
objectArray = {1000, 500, 1, 1000};
n4 = n4 * objectArray[c[0][species][22]] / 1000;
```
So rarity col 22: `0/3 → ×1.0`, `1 → ×0.5`, `2 → ×0.001` (effectively
uncatchable = **legendary**).

### Type chart (FACT) — `game/j.java:1775-1778`

Super-effective cycles among the 7 elements:
`0→1→2→3→0` and `5→6→4→5`. col22==2 species are exempt from the chart
(treated specially).

---

## 4. Skill column map — `c[1][skill][col]` (FACT)

Skills are grouped 10-per-element: a species of element `e` can learn skill ids
`e*10 .. e*10+9` (`j.java:1227`). 7 elements × 10 = 70 rows = table size. ✔

| col | meaning                              | code site |
|-----|--------------------------------------|-----------|
| 0   | element group 0–6                    | `j.java:1227` (range derivation) |
| 1   | name string id                       | `j.java:1514` style name lookups |
| 2   | description string id                | chs strings 529+ are skill descs |
| 3   | misc coeff; `!=0` gates usability    | `a.java:2383` `c[1][E][3]==0` |
| 4   | **required level / unlock**          | `j.java:1229` `c[1][n][4] > c[8][grp][stage]` |
| 5   | SP / energy cost                     | `j.java:1249` `z[i]=c[1][by][5]` |
| 6   | behavior flag (1 = apply col7 effect)| `a.java:2543/2547` |
| 7   | effect / status id                   | `a.java:2525` `a((byte)c[1][by][7],…)` |
| 8   | **power %** (dmg/heal = stat×col8/100)| `a.java:2509/2531` |
| 9   | target (0 = self/ally, else enemy)   | `a.java:2504/2548` |

---

## 5. Item-like tables

### c[4] — items: capture balls + food/potions (FACT for ids; INFERENCE on slots)
`col0` name id, `col1` item id, `col2` desc id, `col3` price, `col4` stack/flag,
`col5` category, `col6` catch power / effect, `col7+` extra effect values.
(`a.java:2770` `c[4][ball][6]` is the ball’s catch power → col6 = effect/power.)
Examples: *Tất Trung Cầu*, *Phong ấn cầu*, *Cao cấp cầu*, *Đại sư cầu*
(capture balls), *Bánh Sandwich*, *Chocolate* (food/heal).

### c[3] — materials / catch reagents (FACT for ids)
`col0` name id, `col1` item id, `col2` desc id, `col3+` effect values.
Some entries double as combat constants (`a.java:2680` `c[3][5][5]`,
`a.java:2778` `c[3][11][5]`). Examples: *Mạn Đà La Thạch*, *Tinh Nguyên Thạch*,
*Hồn Tinh Thạch*, and `c[3][17]` = *Chìa khóa* (Key).

### c[5] — special / quest items (FACT for ids)
`col0` name, `col1` id, `col2` desc. Examples: *Trứng sủng vật* (pet egg),
*Dây cương cưỡi sủng 1–4* (riding harnesses), *Trang sách…* (pet books).

### c[2] — badges / medals (FACT for ids)
`col0` name, `col2` desc, `col5` unlock count, `col6` value. The 8 elemental
“…Chương” emblems (e.g. *Liệt Hỏa Chương*, *Hoàng Kim Chương*).

---

## 6. Recovered counts (FACT)

| entity            | count |
|-------------------|-------|
| db.mid tables     | 9     |
| species (c[0])    | 100   |
| skills  (c[1])    | 70    |
| items: balls/food (c[4]) | 15 |
| items: materials  (c[3]) | 18 |
| items: special    (c[5]) | 11 |
| items total       | 44    |
| badges  (c[2])    | 8     |
| active effects (c[6]) | 15 |
| status ailments (c[7])| 11 |
| evolution brackets (c[8]) | 4 |
| chs.mid string groups | 636 |

All decoded values pass sanity checks: every species element ∈ 0..6, every
rarity ∈ 0..3, skill count == 70 (7×10), db.mid fully consumed (7386/7386).

---

## 7. Undecoded / open items (INFERENCE gaps)

- **Species cols 19, 20, 21**: present in the data (often `-1`, occasionally
  small ints / chained ids — e.g. species 6 has `7,0,1`) but **never indexed**
  in the decompiled code we read. Likely sprite/animation or alt-evolution
  metadata. Kept as `raw[19..21]` in `species.json`; not interpreted.
- **c[6] / c[7] effect numerics**: row→string mapping is decoded (buff/debuff
  names + descriptions), but the per-column numeric semantics (durations,
  magnitudes) are only partially cross-referenced; not emitted as a typed
  schema. `badges.json` covers c[2]; c[6]/c[7] are not separately emitted.
- **Skill col 3**: confirmed to gate usability (`==0` check) and to carry a
  per-skill coefficient, but its exact gameplay role (learn weight vs accuracy)
  is INFERENCE.
- **c[8] stage semantics**: shape (4×5) and use as `c[8][group][stage]` vs the
  skill required-level is FACT; the precise stage→level scaling is INFERENCE.

Raw cell arrays are preserved in every JSON record (`"raw": [...]`) so nothing
is lost and any later re-interpretation can proceed without re-reading the JAR.
