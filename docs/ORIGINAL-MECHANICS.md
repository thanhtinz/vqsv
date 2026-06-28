# VQSV — Original Game Mechanics & Screen Flow

Reverse-engineered from the decompiled J2ME JAR (CFR output) to guide a faithful
reproduction. The game is **"Sủng vật Vương quốc - Liệt hỏa"** ("Pet Kingdom -
Blazing Fire"), a single-player pet-collect-and-battle RPG, Vietnamese
localization by **BIGAME** (about-screen string in `game/l.java`).

> **Source of truth:** the decompiled classes (paths relative to the decompile
> `src/` root, e.g. `game/f.java`). The agent that produced this doc read the
> actual bytecode-decompiled Java; line numbers refer to that decompiled source.
> Where the decompiler failed to structure a method (CFR `** GOTO` / `Unable to
> fully structure code`), facts are taken from the readable fragments and marked.

> **FACT vs INFERENCE:** sections explicitly separate what is in the code from
> what is interpreted. A consolidated "Unclear / open questions" list is at the
> end.

---

## 0. Engine architecture (FACT)

| Class | Role |
|-------|------|
| `game/GameMIDLet.java` | MIDlet entry. Creates `game.n`, sets it as the Displayable. |
| `game/n.java` | The `Canvas` + game-loop `Thread`. Full-screen. Delegates everything to `game.f`. |
| `game/f.java` | **Top-level application state machine** (splash → logo → title → connect → world/battle). Field `u` = state byte. |
| `a/a.java`, `a/b.java` | Abstract bases. `a/b.java` holds the **input bitflag model** (key→flag, per-frame edge detection). |
| `game/h.java` | **Title / main menu** (New game, Continue, Options, Help, About, Exit). |
| `game/m.java` | **World / tile-map gameplay** (movement, NPCs, encounters, scene loading). |
| `game/a.java` | **Battle controller** (turn loop, actions, capture, victory/exp). |
| `game/j.java` | **Pet / "sủng vật" entity**: stats, damage, exp, element relations, skills. |
| `game/k.java` | **Tile-map / sprite engine + player object** (`game.k.p()` is the player). |
| `game/i.java` | Moving entity (NPC / wild pet / character sprite). |
| `game/e.java` | Game-logic / scripted-event handler (gold, badges, save I/O via `DataInputStream`). |
| `game/l.java` | **Universal in-game UI/menu controller.** Loads ALL `.ui` files for world *and* battle screens (worldMenu, gamemenu, shop, bag, petstate, battle, battleSkill, levelUp, evolve, …). Largest class (265 KB) because it wires every screen. |
| `game/c.java` | **Developer/debug "Đài điều khiển" (control panel)** — jump to any map/room, toggle encounters. NOT a player screen. |
| `game/b.java` | Intro / splash scrolling-banner animation (top-level state 23). |
| `game/o.java` | **Bitmap-font renderer** loaded from `/font.bin` (used by every screen). |
| `c/j.java` | **UI component manager.** `a(path, flags, listener)` loads/shows a `.ui` file; keeps a stack of live UI trees (`c.c`/`c.h` widgets) and paints them. (Misnamed-looking but is NOT networking.) |
| `c/c.java`, `c/h.java` | UI component tree / individual widget. |
| `a/b/c.java` | **Data tables loader.** Loads the creature/skill/item DB and string table (see §8). |
| `d/a.java` | Native/script function dispatcher (incl. **SMS send** via `MessageConnection`). |

**There is no online server / socket networking.** The only network primitive is
`javax.microedition.io.Connector` opening a **`MessageConnection` to send an SMS**
(`d/a.java` ~line 250-280) for paid activation. The game is offline single-player;
"activation" and several shop perks are SMS-billed (see §7.7).

### Game loop (FACT — `game/n.java`)
```
run(): while (f.A() > 1) { f.a(); repaint(); serviceRepaints(); sleep(frameMs - elapsed); }
```
- `f.a()` = update (tick)  •  `f.a(Graphics)` = paint  •  frame budget `a.a.h()` = **66** (set in `n` ctor `a.a.a(66)`) → ~15 fps target.
- Canvas key events go to `f.j(int)` / `f.k(int)` (pressed/released), pointer to `f.c/d(x,y)`.

---

## 1. Input model (FACT — `a/b.java`)

Every screen reads input through the same bitflag system. `a/b.java::a(int)` maps a
J2ME key code to a bit:

| Key | Code | Bit |
|-----|------|-----|
| `0`..`9` | 48..57 | 1, 2, 4, 8, 16, 32, 64, 128, 256, 512 |
| `*` | 42 | 1024 |
| `#` | 35 | 2048 |
| UP (gameAction) | -1 | 4096 |
| DOWN | -2 | 8192 |
| LEFT | -3 | 16384 |
| RIGHT | -4 | 32768 |
| FIRE | -5 | 65536 |
| Left Softkey | -6 / -21 | 131072 |
| Right Softkey | -7 / -22 | 262144 |

State tracking (per object): `b` = currently held, `c` = pressed-this-poll,
`x()` rolls each frame: `e`=previous-held, `f`=pressed-this-frame, `g`=released.

Helpers:
- `g(flag)` → **true if flag was pressed this frame** (`f & flag`). Used for menu taps/confirm.
- `i(flag)` → **true if held** (`e & flag`). Used for continuous movement.
- `h(flag)` → released this frame.
- `a(x,y,w,h)` → **pointer hit-test** (touch screens) against a rectangle; consumes the touch.

### Common combined flags (these recur everywhere)
Movement/menus accept **both the d-pad and the number keypad**, so the code ORs them:

| Combined value | = | Meaning in most screens |
|----------------|---|--------------------------|
| `4100` | UP(4096) \| '2'(4) | Up / scroll up |
| `8448` | DOWN(8192) \| '8'(256) | Down / scroll down |
| `16400` | LEFT(16384) \| '4'(16) | Left / prev |
| `32832` | RIGHT(32768) \| '6'(64) | Right / next |
| `65568` | FIRE(65536) \| '5'(32) | Action / interact (world) |
| `196640` | LSK(131072) \| FIRE(65536) \| '5'(32) | **Confirm / select** (menus) |
| `262144` | RSK | **Back / cancel** |
| `786432` | LSK \| RSK | Close menu / cancel |
| `61780` | UP\|DOWN\|LEFT\|RIGHT keypad mask | "any movement key" (release check) |

> Note: a few screens (debug panel `game/c.java`, world-map state) swap which axis
> is value-adjust vs cursor-move; per-screen sections call this out.

---

## 2. Top-level state machine — `game/f.java` (FACT)

State = `byte u`, changed by `f.a(byte)`; read by `f.A()`. Update logic in `f.a()`
(switch on `u`); paint in `f.a(Graphics)`. The world/battle/dialog "sub-apps"
(`game.m`, `game.a`, `game.h`, `game.c`) are held in field `y` and ticked when
`u` is one of {8,11,13,20}.

### State table

| `u` | Name | What happens / transition |
|----|------|----------------------------|
| 3 | **LOADING / init** | White screen, runs a timer (`G()`), calls `b()` to load core data; when ready & "done" flag → state **15**. (`f.a()` case 3; `b()` loads `img_22`, logo `/data/logo/0`, builds DB.) |
| 15 | **Publisher logo** | Draws `/data/logo/0` centered ~10 frames, then loads `/data/logo/cwalogo` → state **16**. |
| 16 | **CWA logo** | Draws `cwalogo` ~10 frames → state **6**. |
| 6 | **SMS / activation splash** | Black screen, draws strings `c(8)` (title), `c(9)`, softkey labels `c(4)`/`c(5)`. LSK or top-left tap → state **7** (`s=1`); RSK / bottom-right → state **7** (`s=0`). (`s` carries an activation/skip choice.) |
| 7 | **→ build title** | Tears down current sub-app, creates `game.h` (title), ticks it → state **8**. |
| 8 | **TITLE / main menu** | Runs `game.h` (see §3). |
| 9 / 22 | **CONNECT / enter-world transition** | Spinner timer; builds `game.m` (world) via `m.y(); y.b()` → state **11**. (`game.k.V` reset.) State 22 is a variant entry. |
| 23 | **Intro banner animation** | Runs `game.b` (`F`); `F.a((byte)1/2/3)` drives a sprite that slides in; advances 1→2→3 then → state **11**. |
| 10 | **→ re-enter world** | Rebuild `game.m`, `((m)y).J()` → state **11** (used returning from battle). |
| 11 | **WORLD (running)** | Ticks `game.m` (the map). |
| 12 | **→ build battle** | Creates `game.a` (battle); applies a fade overlay colored by battle-type `m` (0→6, 1→7, 2→8) → state **13**. |
| 13 | **BATTLE (running)** | Ticks `game.a`. |
| 19 | **→ build debug/dialog (`game.c`)** | Creates `game.c`, passes flavor byte (1 if from world, 2 if from battle) → state **20**. |
| 20 | **DEBUG PANEL (running)** | Ticks `game.c` ("Đài điều khiển"). |
| 2 | **Paused** (hideNotify) | "Trò chơi tạm dừng" / "Phản hồi". Resume via RSK or tap. |
| 4 | (transient) | resume/quit helper. |
| 1 | **Quit** | leads to `destroyApp` (loop ends when `A() <= 1`). |
| 21 | (special intro overlay, uses `r` RGB buffer + "Nhấn vào đây để bỏ qua" = "tap to skip") | → state 7. |

### Flow summary
```
LOADING(3) → logo(15) → logo(16) → SMS splash(6) → TITLE(7→8)
   TITLE ──New/Continue──▶ CONNECT(9/22) ─[opt intro(23)]─▶ WORLD(11)
   WORLD ──wild/NPC encounter──▶ build battle(12) ─▶ BATTLE(13) ──end──▶ re-enter world(10→11)
   WORLD/BATTLE ──(debug)──▶ build(19) ─▶ DEBUG(20)
```

### Elemental-counter strings (FACT — `game/f.java` line 39, `E[]`)
Displayed (random one shown on the connect/load screen, state 9):
```
"Hỏa hệ khắc mộc hệ", "Mộc hệ khắc thổ hệ", "Thổ hệ khắc thủy hệ",
"Thủy hệ khắc hỏa hệ", "Quỷ hệ khắc phong hệ", "Phong hệ khắc điện hệ",
"Điện hệ khắc quỷ hệ"
```
(Fire>Wood, Wood>Earth, Earth>Water, Water>Fire; Ghost>Wind, Wind>Electric, Electric>Ghost.)
The authoritative runtime relation is the numeric chain in `game/j.java` (see §6.3).

---

## 3. Title / Main menu — `game/h.java` (FACT)

State = `a` (set via `a(byte)`). Loads background texture `/data/tex/menu`
and decoration sheet `/data/img/img_833`. **No login, no server select, no account.**

Menu items are string-table IDs in `n[]`:
```
n = i ? {504, 503, 505, 506, 507, 508}   // i = "a saved game exists"
      :       {503, 505, 506, 507, 508};
```
- `503` New game, `504` **Continue** (only present when a save exists, flag `a.a.i`),
  `505` Options, `506` Help, `507` About, `508` Exit. (Exact words live in the
  string table `chs.mid`; IDs confirmed by the dispatch below — INFERENCE on the
  English gloss, FACT on the ordering.)

Rendered as a **horizontal carousel** (one item at a time, color-cycling text with
a 4-way outline, animated falling decorations).

### Inputs (case 0)
- LEFT `g(16400)` (or tap 284,205,48,42): `o--`.
- RIGHT `g(32832)` (or tap 496,205,48,42): `o++`.
- Confirm `g(196640)` (or tap 346,208,144,42): select item `o`.

### Selection dispatch
- **Continue** (when `i`, `o==0`): teardown → `f.a((byte)9)` (enter world).
- **New game** (`o==0` when no save, or `o==1` when save exists): `a((byte)5)` →
  confirm "Có chắc chắn xóa dữ liệu cũ để chơi mới không?" (delete old save?). On
  confirm: `game.m.I()` + reset + `f.a((byte)9)`.
- **Options** → state 1 (`d.v()/d.w()` in `game.l`; title "Tùy chọn", adjusts a 0-3
  setting `f.y().s`, likely sound/volume, via `C()/D()`).
- **Help** → state 2 (`d.r()/d.s()`; title "Trợ giúp", 3 pages, uses `help1.ui`/`help.ui`).
- **About** → state 3 (`d.t()/d.u()`; "Quan tại"; text: "Tên trò chơi: Sủng vật
  Vương quốc - Liệt hỏa / Việt hóa: BIGAME").
- **Exit** → state 4: "Bạn có muốn thoát không?" / "Không"; LSK → `f.a((byte)1)` (quit),
  RSK → back.

---

## 4. World / map — `game/m.java` (FACT unless noted)

Singleton `game.m.y()`. Companion UI controller `d = game.l` (loads `world.ui`,
menus). Player object `n = game.k.p()`. NPCs/objects `o[]` (`game.i`). Scene/area
logic `aa = game.e`. UI manager `c = c.j`.

### Map coordinates (FACT)
- `public int q` (region) and `public int r` (area within region) are **map
  position**, not UI state. Scenes are a flattened table indexed `w[q]+r` with
  `w = {0,2,9,17,25,38,45,47,60,67,75,90}` (11 regions). The current area name is
  `v = game.m.c(384 + w[q] + r)` and shown in `world.ui` field 6.

### Runtime UI sub-state (FACT)
Runtime state is the inherited `this.a`, set by `a(byte)` (~line 1596), dispatched
in `a()` (~line 1815). Selected states:

| `a` | Meaning | Notes |
|----|---------|-------|
| 0 | **World / walk mode** | default; movement in `af()`. |
| 4 | **World MAP screen** (`K()`) | pan with arrows; cells ×16 (x) / ×8 (y); "Khu này không có bản đồ" if no map. Legend: "Cửa ra vào" (entrance), "Bến tàu" (dock), "Cửa Đạo quán". |
| 6 | **Bag** | `d.n()/d.o()`. |
| 22 | **Save confirm** | "Có lưu dữ liệu không?" |
| 23 | **NPC dialog** | text from `an[]` (names) / `ab[]` (lines); see below. |
| 33 | **World menu** | opened by Left Softkey; items below. |
| 5 | Pet/ride panel | `d.ah()/ai()`. |
| 9 | Record / pet state | `d.R()/S()`. |
| 10 | Task | `d.V()/W()`. |
| others (1,2,3,8,11-21,24-32,100-104) | sub-panels (shop route, skill, evolve, etc.) routed through `game.l`. |

### Movement (FACT — `af()`, ~line 2323)
```
if (movement allowed) {
  if (i(4100))  n.a((byte)1,(byte)2);   // UP    → dir 2
  else if (i(8448))  n.a((byte)1,(byte)0); // DOWN → dir 0
  else if (i(16400)) n.a((byte)1,(byte)3); // LEFT → dir 3
  else if (i(32832)) n.a((byte)1,(byte)1); // RIGHT→ dir 1
}
if (h(61780)) n.a((byte)0, n.o);          // release all → idle, keep facing
```
- Direction encoding: **0=down, 1=right, 2=up, 3=left**.
- "Movement allowed" guard: `!aa.C() && n.i() < 5 && !d.m() && d.K()` (not in a
  cutscene / not paging / no modal up).
- **Action** `g(65568)` (FIRE|'5'): if an NPC is in range (`F != -1`) → `ae()`
  (interact); else pet-pickup / `n.x()`.
- **Tap-to-move:** `c(x,y)` converts screen→world (subtract camera `a.b.d.a().g/.h`,
  add `a.b.d.a().a/.b`), finds the tile, validates walkable via `n.g(tx,ty)`.

### Tiles / collision / camera (FACT)
- Tile cursor highlight is drawn **16×16** (`drawRect(bg/16<<4, bh/16<<4,16,16)`).
- Per-sprite tile metrics come from the sprite descriptor array (`a/b/a.java::b(byte)`
  returns `d[by]`), i.e. **tile/cell size is data-driven** (see ASSET-FORMATS:
  maps are 23×23 grids).
- Collision: `a.b.d.a().b(layer,x,y)` returns a terrain byte; `0` = passable.
- Camera = `a.b.d.a().a` (x) / `.b` (y); off-screen buffer blitted at `.g/.h`.

### World menu (state 33) items (FACT — `game/l.java::h()`)
Opened by **Left Softkey** (`g(131072)` in world). Items (switch on `c`):
0 = talk/pet-talk (`o.a(13)`), 1 = **Map** (`((m)o).K()`), 2 = Task (`o.a(10)`),
3 = **Bag** (`o.a(6)`), 4 = locked feature (`o.a(14)` or "Chức năng này chưa mở"),
5 = **Save** (`o.a(22)`). Close: LSK\|RSK (`786432`) or `e(7)`.

`gamemenu.ui` (a different menu) shows gold `q.G()` (field 18) and a second stat
`q.F()` (field 19); confirm `g(196640)`/`e(6)`, back `g(262144)`/`e(7)`.

### NPC interaction (FACT)
- `F` (static short, init -1) = the in-range NPC index into `o[]`.
- FIRE near NPC → `ae()`: stops player, turns NPC to face player, enters dialog
  state 23. Dialog text built in `a((byte)23)` from `an[name]`/`ab[line]`.
- Dialog advance (`ag()`, confirm `g(196640)`/`e(19)`) routes by NPC type `o[F].a.a`:
  `==24`/`==20` → shop (state 1), `==25` → evolve (state 16), `==68` → boat/wharf
  ("Muốn lên thuyền đi đâu?", state 28), else generic bubble → back to world (0).

### HUD drawn in `m.a(Graphics)` (FACT)
- Background tile/fill + full sprite layer (map, player, NPCs).
- **Floating "+N gold"**: iterates `n.W`, draws `"+"+amount` (color `16704699`)
  with gold icon `/data/tex/gold`.
- **Race timer** (only on area q==3,r==7): mm'ss"ms via `a(long)` at (10,40).
- Numeric HUD (gold/level/HP digits, names) is NOT primitive-drawn here — it is
  bound into `.ui` widget text by `game.l` and painted via `c.a(graphics)`; the
  `game.e` scene overlay paints on top.

### Wild encounters (FACT)
`L()` reads the terrain byte, picks a random pet from per-terrain spawn pools
(`ap/aq/ar/as` built in the map loader from `/data/script/petArea.mid`), then
`game.a.y().a(...)` starts a battle and the controller transitions
`f.y().a((byte)12)` (build battle).

### Pet/world data files loaded (FACT — `m.b()`)
`/data/event/scene_*.mid`, `/data/script/petArea.mid` (wild spawns),
`/data/script/media.mid` (BGM/ambience), `/data/script/petRide.mid` (mounts),
`/data/script/backPic.mid` (backgrounds); textures `gold`, `key0..2`, `down0..3`.
Save RMS stores `"PK6_RMS_POKPET"` (pets) and `"PK6_RMS_PETBALL"` (balls) —
confirming a Pokémon-style pet/pet-ball model.

---

## 5. Battle — `game/a.java` (FACT unless noted)

Singleton `game.a.y()`. Battle UI is loaded by `game.l` (battle.ui, battleSkill.ui,
levelUp.ui, evolve.ui, choice.ui). Key fields:
- `byte m` = **battle type** (read by `f.java`: 0→fade6, 1→fade7, 2→fade8). m==2 is
  a special "answer"/quiz battle (uses `answer.ui`; disables capture & flee). *(m
  semantics: INFERENCE except the fade mapping which is FACT.)*
- `int l` = **field layout** (0 = 1v1, 1 = multi/2v2; slot count `z={2,4}`, `z[l]`).
- `this.a` = state, `this.s` = acting pet, `this.s.q`/`H` = targets/turn-order,
  `B` = player party (`game.k`), `u`/`J` = exp recipients / participants (static Vectors).

### 5.1 Turn structure (FACT) — turn-based, speed-ordered
- Turn order `O()`: pets sorted **descending by SPD = `d[4]`**; result in `H`
  (actor list) and `p[]` (display order). A pet with priority flag `f(7)` is forced
  first. Player (side `s()==0`) and enemy (`s()==1`) pets are **interleaved by speed
  within a round** — not strict player-phase-then-enemy-phase.
- `this.t` = turn cursor (advanced in `C()`/`J()`); `this.s = H.elementAt(t)`.

### 5.2 Battle state machine (FACT) — `a(byte)` / `a()` / `a(Graphics)`

| `a` | Phase |
|----|-------|
| 0 | **Intro** — walk pets into field-script positions, then → 20. |
| 20 | **Show actor / poll player action menu** (`l.d(s)`). |
| 1 | **Advance turn / pick next actor**; if none alive to act → `I=true`. Enemy w/ skills → 12, else 2; player → 2. |
| 2 | **Resolve action / pick skill+target.** Enemy AI picks skill via `e(s)` + random target, then → 7. |
| 7 | **Animate attack + apply damage** (uses `b(j)`, §6.3). Then `K()` decides 2/9/15/continue. |
| 12 / 13 | **Apply buffs/debuffs (status ticks)** for one side / the other. |
| 15 | **Enemy respawn** (summon reserve when a slot faints and `G[0] < E.length`). |
| 5 / 16 | **Pet-swap** (đổi sủng vật) — `l.aa()/ab()`. |
| 3 / 4 / 11 | **Item / đạo cụ** sub-screens (`l.e/f`, `l.am/an`, `l.a(4,0)`). |
| 6 | **Target select on field** (`I()`): arrows move cursor, FIRE confirm → `D()`. |
| 17 / 21 | **Capture** (bắt) — throw ball, roll (§6.4). |
| 8 | **Victory / EXP distribution** (`l.ap()` animates bars; `U()` restores HP/SP). |
| 22 | **Level-up screen** (`l.aq()` → `j.w()`; levelUp.ui). |
| 9 | **Defeat** (`L()`: revive party to 1 HP if no revive item, exit to state 10). |
| 10 | **Flee resolution** ("Chạy trốn", shake-out anim → exit). |
| 18/19, 23/24, 101/102/104 | confirm-dismiss & post-battle dialog/shop helpers. |

`K()` victory/defeat: all-enemy-dead → victory (`U(); a(8)`); player slots
exhausted → defeat (`a(9)`); else respawn/continue.

### 5.3 Player action menu (FACT) — `game/l.java::d(j)`, battle.ui
Cursor `l.a` (0-5). LEFT `g(16400)` prev, RIGHT `g(32832)` next, FIRE/LSK `g(196640)`
(or full-screen tap) confirm.

| a | Action | Notes |
|---|--------|-------|
| 0 | **Attack / Skill** | → state 3 (skill list `battleSkill.ui`). |
| 1 | **Capture (bắt)** | blocked if `m==2` ("Trận chiến này không cho bắt sủng vật") or party full (`q.z()==2`, "Không gian không đủ"); else state 21. |
| 2 | **Item (đạo cụ)** | → state 4; blocked if bound `q(2)`. |
| 3 | **Pet-swap (đổi sủng vật)** | → state 5; blocked if bound. |
| 4 | item-bag variant | → state 11. |
| 5 | **Flee (chạy trốn)** | blocked if bound or `m>0`/not wild ("Trận chiến này không thể trốn chạy"); roll §6.5. |

### 5.4 Skill menu (FACT) — `game/l.java::f`, battleSkill.ui
- UP `g(4100)` / DOWN `g(8448)` scroll. FIRE/LSK `g(196640)` confirm.
- Cursor `e==5` = "Hủy bỏ" (cancel) → back to state 20.
- If skill SP available (`j.t(e)`): build targets → target-select (state 6) or
  direct `D()`. If insufficient SP: "Kỹ năng giá trị chưa đủ".
- RSK `g(262144)` cancel.
- battleSkill.ui fields: 0 = list; per row name = field `4+i*4`, "cur/max SP" = `5+i*4`.

### 5.5 Battle HUD (FACT) — `a.a(Graphics)` + `game.l`
- Battle scene image `n` as background (else black).
- All on-field pets drawn (`b(graphics)`), each with its HP/info bar sprite
  `an[i]`. `an[count]` = player party bar, `an[count+1]` = enemy bar (multi only).
- **Floating damage/heal numbers** via blood-digit images `blood_0/1/2`; type 0 =
  numeric damage (red), type 1 = text ("Né tránh"=dodge, "Bắt thành công"=caught,
  element/status name).
- **Elemental-advantage % readout** on the bars (battle.ui fields **58** = enemy,
  **59** = player), set by `l.b(j,j)`: advantage side **300%**, disadvantaged side
  **60%**, neutral **100%** (these are display strings of the ×3 / ×0.6 / ×1 damage
  multiplier — see §6.3). Initial values "100%/100%".

---

## 6. Core mechanics & formulas — `game/j.java` (FACT)

`game/j.java` is the pet entity. Stat arrays: `d[]` = base/max, `e[]` = current.

### 6.1 Stat layout (index meaning — FACT for usage, INFERENCE for labels)
| idx | meaning |
|-----|---------|
| `d[0]` | **grade / quality tier** (1-5) → multiplier index `N[d[0]-1]` |
| `d[1]` | **HP (max)**, `e[1]` = current HP |
| `d[2]` | **ATK** |
| `d[3]` | **DEF** |
| `d[4]` | **SPD** (turn order) |
| `U` | **level** (cap 50) |
| `T` | **EXP** |
| `W` | **species id** (indexes DB `c[0]`) |

Grade multiplier (percent): `N = {90, 95, 100, 110, 125}`.

### 6.2 Stat growth (FACT — `j.W()` / static `j.a(species,level,grade,statType)`)
For species `W`, level `U`, grade `d[0]`:
```
HP  = (c[0][W][5]  + c[0][W][6]*U      + c[0][W][7])  * N[d[0]-1] / 100
ATK = (c[0][W][8]  + c[0][W][9]*U      + c[0][W][10]) * N[d[0]-1] / 100
DEF = (c[0][W][11] + c[0][W][12]*U/10  + c[0][W][13]) * N[d[0]-1] / 100
SPD = (c[0][W][14] + c[0][W][15]*U/10  + c[0][W][16]) * N[d[0]-1] / 100
```
(`c[0]` = species table in `/data/script/db.mid`. Note DEF/SPD use `U/10`.)

### 6.3 Damage (FACT — `j.b(j target)` returns `{damage, isCrit, statusId}`)
1. **Base** = `C()` ≈ `attacker.ATK(e[2]) − target.DEF(q.e[3])`, with modifiers:
   foe DEF-buff `f(4)`, own DEF-debuff `f(2)`, low-HP rage `f(0)` (when
   `e[1] ≤ c[3][0][5]·d[1]/100`), boost `f(1)`.
2. **Crit chance** = `5` base (`+30` if max-evolved final stage `D == X[elem]+Y[elem]-1`)
   `+ SPD(e[4])/2` `+ c[3][4][5]` if `f(4)`. If `rand(100) ≤ crit` → **damage ×1.5**
   (`*3/2`), isCrit=1.
3. **Skill multiplier** by skill id `E` (big switch): most → `dmg * c[1][E][3] / 100`
   (skill power %); some add lifesteal `+ dmg / c[1][E][8]`; HP-ratio skills (53/59):
   `dmg * (c[1][E][8] − targetHP%) / 100`; others set a status id `c[1][E][8]`.
4. **Buffs/debuffs**: flat `n(0)`, `+%` `n(1)`/`n(8)`, reduce `q(6)`, proc-mult `n(6)`,
   plus party-wide skills `game.k.p().b(...)`.
5. **ELEMENT multiplier** (the headline mechanic):
```
if (this.a(target) == 0) dmg *= 3;              // super-effective  ×3  ("300%")
else if (this.a(target) == 1) dmg = dmg*60/100; // resisted         ×0.6 ("60%")
// else neutral ×1 ("100%")
```
6. Clamp `≥ 1` with ±1 random jitter.

### 6.3.1 Element relations (FACT — `j.a(j)`, ~line 1756)
Element id = `c[0][species][1]` (0-6). Attacker beats defender (return **0**,
super-effective) on the chain:
```
0→1, 1→2, 2→3, 3→0,   and   5→6, 6→4, 4→5
```
Reverse → return **1** (resisted ×0.6); otherwise **-1** (neutral ×1). A
"group" flag `c[0][species][22]` (value 2) gates whether the matchup applies.

> Naming note: the in-game element name tables (`j.U()`/`z()`) read as
> **0=Mộc(wood), 1=Thổ(earth), 2=Thủy(water), 3=Hỏa(fire), 4=Quỷ(ghost),
> 5=Phong(wind), 6=Điện(electric)** → chain = Mộc>Thổ>Thủy>Hỏa>Mộc (4-cycle) and
> Phong>Điện>Quỷ>Phong (3-cycle). The `f.java::E[]` *display* strings (§2) use a
> different surface ordering ("Hỏa khắc Mộc…"); treat those as UI flavor — the
> **`j.a(j)` numeric chain is authoritative**. (FACT: both code locations; the
> reconciliation is INFERENCE.)

### 6.4 Capture (FACT — `j.m(ballId)` returns rate %)
- `ballId == 0` → **always 100** (tutorial "phong ấn cầu" / guaranteed seal-ball).
- Otherwise multiplicative:
  - **HP factor**: target HP% ≤15 → 85, ≤50 → 45, else → 20.
  - `× c[4][ball][6] / 100` (ball quality).
  - `× {110,100,95,80,70}[grade-1] / 100` (higher grade harder).
  - `× {10,11,12,12,12}[statusIdx] / 10` (sleep=1, bind=2, `n(10)`=3, `f(11)`=4).
  - `× (100 + c[3][11][5])/100` if `f(11)`.
  - `× {1000,500,1,1000}` indexed by `c[0][species][22]` — **value 2 ⇒ ×1/1000
    (effectively uncatchable / legendary)**.
  - high-level cap: target level ≥20 → capped by `{0,15,35,65}[ball]`.
  - clamp 1-100.
- Roll: `am = rand(100) < rate`. Success result: add to bag ("Bắt thành công"); if
  bag full → bank; if both full → released ("…đã phóng sinh").
- Premium ball "Tất trùng cầu" ("loại xịn") referenced in tutorial; basic = "phong ấn cầu".

### 6.5 Flee (FACT — `game/l.java::d`, action 5)
- Faster than enemy (`s.t() > o[0].t()`) → auto-success.
- Equal level → 95%.
- Slower → `chance = max(15, 95 − levelDiff·10)` %.
- Success → exit (`f.y().a(10)`); fail → "Chạy trốn thất bại", turn consumed.

### 6.6 EXP & leveling (FACT)
- **EXP-to-next-level**: `j.B(level) = 15·level² − 200` (capped at `B(50)=37300`).
  Level cap **50** (`j.u()`).
- **EXP gained on enemy faint** (`h(enemy)`):
```
pool = ((enemyLevel<<1)*enemyLevel + 50) * aN[grade-1] / 10 + 400   // aN={10,11,12,13,15}
perPet = pool/participantCount * aO[count-1] * levelFactor / 1000     // aO={10,12,13,14,15,16}
                                                                       // levelFactor from aP={105,100,80,60,40,20,5}
```
  `+50%` if exp-boost flag `f(5)`. Non-fighting party members get a small share
  (`/3000`) if party skill `b(7)` or pet flag `f(6)`. Accumulated into `j.C`,
  committed in `U()`.
- **Level-up**: state 8 animates bars; when accrued exp ≥ `v()` → state 22 →
  `j.w()` (`++U`; recompute stats via `W()`). levelUp.ui shows old/new stats.
- **New skill on level-up**: if `j.F() < 5 && j.F() < level/10 + 1`, the pet "Có
  thể học tập kỹ năng mới" (can learn a new skill) → choiceskill.ui. Learnable
  skills (`j.G()`): for species element `s2`, skill ids `s2*10 .. s2*10+9` (10 per
  element) whose required level `c[1][n][4] ≤ unlock-bracket c[8][form][rank]`,
  where rank brackets `X() = {5,10,20,30,40}`.

### 6.7 Evolve / mutate (tiến hóa / dị hoá) (FACT)
- Triggered outside battle (party/pet screen) via evolve.ui (`game.l` ~2770).
- Eligibility: "Có thể tiến hóa"; failures: "Sủng vật này không thể tiến hóa",
  "Tài liệu chưa đủ, không thể tiến hóa" (materials needed), "Không thể lại tiến
  hóa hoặc dị hoá" (already max). Final evolution stage detected as
  `D == X[elem]+Y[elem]-1` (also grants +30 crit, §6.3).
- A long help string (`game/m.java`) explains evolution vs mutation (dị hoá).

---

## 7. Shop, inventory, currency, items — `game/l.java` (FACT)

`game/l.java` is the universal `.ui`-driven screen controller; `this.p = c.j` is the
UI manager. The standard menu navigation is UP `g(4100)`, DOWN `g(8448)`, confirm
`g(196640)`/`e(6)`, back `g(262144)`/`e(7)`.

### 7.1 Currency
- **Kim tiền** (gold) = `q.G()` (player `game.k`), shown in gamemenu.ui field 18.
- **Huy hiệu** (badges) — secondary currency (`f.java` SMS offers; `game.l` badge.ui).
- Second stat in gamemenu field 19 = `q.F()`.

### 7.2 Shop (`shop.ui`, ~line 1081)
"Ngân hàng Sủng vật" (Pet Bank) menu with options:
0 = (talk/route to state 7), "Gởi lại" (deposit), "Lấy ra" (withdraw),
"Phóng sinh" (release). Sub-screens: `shopbuy.ui` (buy, ~1246/1635),
`shopsale.ui`, `bodyShop.ui`, `msgyn.ui` (yes/no confirm), `msgRecover.ui`,
`msgwarm.ui` (warning), `msgconfirm.ui`.

### 7.3 Bag (`bag.ui`, ~line 3090) & pet storage
- `bag.ui` = item inventory. `petstate.ui` (~899) = pet party / bank ("Ngân hàng
  Sủng vật"), shows up to 6 in party (`q.P` list; scroll when >6).
- `petsetting.ui`, `petmap.ui`, `choice.ui`, `skill.ui`, `ride.ui` (mounts),
  `record.ui` (records), `task.ui`/`taskOption.ui`/`taskTip.ui` (quests),
  `badge.ui` (badges/arena), `transmit.ui` (teleport), `wharf1.ui`/`wharf2.ui`
  (boat/dock), `openbox.ui` (loot box), `levelUp.ui`, `evolve.ui`, `choiceskill.ui`,
  `worldMenu.ui`, `gamemenu.ui`, `gamesystem.ui`, `option.ui`, `help.ui`/`help1.ui`,
  `menu.ui`/`menu1.ui`, `dialog.ui`, `answer.ui`, `npcEnemy.ui`, `msgtip.ui`,
  `smsTip.ui`, `smsInfo.ui`.

### 7.4 Item/box rewards (FACT — `game/l.java` ctor `R[]`/`S[]`)
Loot-box / quest reward text list `R[]`:
```
"Đạt được 2000 kim tiền", "Đạt được 5 Phong ấn cầu", "Đạt được 5 Bánh Sandwich",
"Đạt được 2 Sinh mệnh thạch", "Đạt được 2 huy hiệu"
```
So items include: **Phong ấn cầu** (seal-ball), **Tất trùng cầu** (premium ball),
**Bánh Sandwich** (food/heal), **Sinh mệnh thạch** (life stone / revive), badges,
gold. Reward icon ids `S[] = {{621,622},{623,624},{625,626},{627,628},{629,630,631,632}}`.

### 7.5 World-menu item list (FACT — ctor `W[]`)
`W = {"Dẫn thưởng", "Tiến hóa", "Dị hóa", "Tài liệu", "Cách mở"}` (claim reward,
evolve, mutate, materials, how-to-unlock).

### 7.6 UI `.ui` ↔ screen map (FACT — which controller loads what)
| Screen | `.ui` file(s) | Loaded by |
|--------|---------------|-----------|
| Title submenus | `help.ui`,`help1.ui`,`option.ui` | `game.l` via `game.h` |
| World HUD | `world.ui` (field 6 = area name) | `game.l::c()` |
| World menu | `worldMenu.ui` | `game.l::g()/h()` |
| Game menu | `gamemenu.ui`,`gamesystem.ui` | `game.l::n()/o()/p()/q()` |
| Shop | `shop.ui`,`shopbuy.ui`,`shopsale.ui`,`bodyShop.ui` | `game.l` |
| Bag | `bag.ui` | `game.l` (~3090) |
| Pets | `petstate.ui`,`petsetting.ui`,`petmap.ui`,`skill.ui`,`choice.ui`,`choiceskill.ui`,`evolve.ui`,`ride.ui` | `game.l` |
| Battle | `battle.ui` (action menu, fields 58/59 = element %),`battleSkill.ui` (skills),`levelUp.ui`,`answer.ui` (m==2 quiz) | `game.l` |
| Tasks/badges | `task.ui`,`taskOption.ui`,`taskTip.ui`,`badge.ui`,`record.ui` | `game.l` |
| Travel | `transmit.ui`,`wharf1.ui`,`wharf2.ui` | `game.l` |
| Loot | `openbox.ui` | `game.l` |
| Messages | `msgtip.ui`,`msgyn.ui`,`msgwarm.ui`,`msgconfirm.ui`,`msgRecover.ui` | `game.l` |
| SMS | `smsTip.ui`,`smsInfo.ui` | `game.l` |
| NPC | `dialog.ui`,`answer.ui`,`npcEnemy.ui` | `game.l` / `a.a.f` |

> The `.ui` files themselves are custom binary component trees (44 of them under
> `data/ui/`, see `docs/ASSET-FORMATS.md`). They define widget rects/ids; the
> `field index` numbers above (e.g. battle.ui 58/59, gamemenu 18/19, levelUp
> 12/13/19-22/31-34/38/40/51) are the widget indices the code writes text into.
> The extractor's JSON dumps (`tools/asset-extractor/out/ui/*.json`) are currently
> only partial short-arrays, not fully-parsed trees.

### 7.7 SMS activation / monetization (FACT — `a/a.java::A[][]`, `d/a.java`)
Paid features are SMS-billed (`MessageConnection.send`):
- **Kích hoạt** (activate game): 15000đ, one SMS for all plays.
- **Tất trùng cầu** (100%-catch ball): 10000đ.
- **Mua sắm kim tiền** (buy gold): 10000đ → 10000 kim tiền.
- **Mua đẳng cấp** (buy levels): 10000đ → all bag pets +5 levels.
- **Mua sắm huy hiệu** (buy badges): 10000đ → 10 huy hiệu.

---

## 8. Data files & tables (FACT — `a/b/c.java`)

`a.b.c` loads, on init:
- `/data/script/chs.mid` → **string table** `a.b.c.d[]` (all localized UI text;
  accessed by `a.a.c(int id)`).
- `/data/script/npcDialog.mid` → NPC dialog text (`m.ab[]`).
- `/data/event/worldEvt.mid` → world events / NPC defs (`a.b.c.g[]`).
- `/data/script/db.mid` → **`a.b.c.c[0..8]`**, nine `short[][]` sub-tables:
  - `c[0]` = **species** stats (col 1 = element, 5-16 = stat formula coeffs,
    18 = skill set, 22 = group/legendary flag, 0 = name string id).
  - `c[1]` = **skills** (col 3 = power %, 4 = required level, 7/8 = effect params).
  - `c[3]` = global combat constants (rage threshold, crit/exp/etc.).
  - `c[4]` = **balls** (col 6 = catch quality).
  - `c[6]` = item/buff effects.
  - `c[8]` = per-form skill-unlock brackets.
  - (`c[2],c[5],c[7]` present; columns not fully recovered.)
- `/data/tex/tex_0..3`, `/data/tex/bk` → textures.
- Other art under `data/img/`, `data/spr/`, `data/map/`, `data/tex/` (see
  `docs/ASSET-FORMATS.md`).

`/font.bin` → custom bitmap font (`game/o.java`); two sizes via `a.a.j()` (small,
size 8) and `a.a.k()` (large, size 16) for the system font, plus the bitmap font
"nhung1".

Save: RMS records `"PK6_RMS_POKPET"` (party/pets) and `"PK6_RMS_PETBALL"` (balls);
`game.e` reads/writes via `DataInputStream`.

---

## 9. Per-screen UI layout cheat-sheet (FACT for fields cited)

- **Title (`game.h`)**: full-screen `menu` texture; one menu label centered with
  outline + color cycle; floating decorations from `img_833`. Softkey labels via
  string table.
- **SMS splash (`f` state 6)**: black bg; title `c(8)` at (w/2,144); subtitle
  `c(9)` (orange) at (w/2,166); LSK label `c(4)` bottom-left; RSK label `c(5)`
  bottom-right.
- **World (`m` + world.ui)**: tile map + sprites fill screen; area name in world.ui
  field 6 (top); floating "+gold" near pickups; gold/level via gamemenu.ui fields
  18/19 when menu open; bottom 30 px reserved for HUD bar.
- **World map (`m` state 4)**: pannable region map; bottom legend bar (color
  1862801) with swatches "Cửa ra vào"/"Bến tàu"/"Cửa Đạo quán".
- **Battle (`a` + battle.ui)**: scene image bg; pets on field; per-pet HP bars
  `an[]`; player party bar + (multi) enemy bar; **element % at fields 58 (enemy) /
  59 (player)**; floating damage numbers (blood digits); action menu 6 items
  (Attack/Capture/Item/Swap/—/Flee).
- **Skill list (battleSkill.ui)**: scrollable; row name at field `4+i*4`, SP
  "cur/max" at `5+i*4`; last entry "Hủy bỏ".
- **Level-up (levelUp.ui)**: pet name field 38, level field 40, 4 stat rows fields
  31-34, "Có thể học tập kỹ năng mới" at field 51 when a skill is available; pet
  sprite at element 10.
- **Pet bank (petstate.ui)**: title "Ngân hàng Sủng vật" field 2; up to 6 party
  slots, scroll for more.
- **Shop (shop.ui)**: "Ngân hàng Sủng vật" + "Gởi lại"/"Lấy ra"/"Phóng sinh".

---

## 10. Unclear / open questions (to resolve before/while reproducing)

1. **`.ui` binary format**: widget tree not fully decoded. The extractor JSONs are
   partial short-arrays. To match exact on-screen positions, the `.ui` parser must
   be completed (the code reads them via `c.c.a(path,…)` in `c/` package — decode
   `c/c.java`/`c/h.java`/`c/d.java` to recover the rect/anchor schema). Field
   indices cited here are FACTs; pixel coords are not yet extracted.
2. **DB column semantics** (`db.mid`, `a.b.c.c[*]`): only the columns touched by
   recovered formulas are known. Full per-table schemas (esp. `c[2]`, `c[5]`,
   `c[7]`, the status-effect encodings in `c[1]`/`c[6]`) need a full pass over
   `game/j.java` + `game/a.java` status code (the buff/debuff `n()/q()/f()` flag
   meanings).
3. **Element id↔name discrepancy**: the `f.java::E[]` display strings vs the
   `j.a(j)` numeric chain order differently. Confirmed the runtime uses the numeric
   chain; the exact Vietnamese label per id should be cross-checked against
   `chs.mid` strings + `j.U()/z()` tables when those tables are dumped.
4. **`m` battle-type values**: 0/1/2 fade mapping is FACT; the gameplay meaning
   (normal / boss / quiz-"answer") is INFERENCE (m==2 disables capture+flee and
   uses answer.ui — strongly implies a quiz/scripted battle).
5. **Movement guard `aa.C()` / `n.i()<5`**: exact semantics of the player "input
   lock" counter `n.i()` (engine sprite) not fully traced.
6. **Tile/cell pixel size**: cursor highlight is 16×16 and world-map cells are
   ×16/×8, but in-world walking uses data-driven sprite metrics (`a/b/a.java::b`).
   Confirm the actual walk-step pixel size from a live map sprite (`data/spr/`).
7. **Save format** (`PK6_RMS_*`): the exact RMS byte layout in `game/e.java`
   (`a(DataInputStream,…)`) was not byte-mapped; needed only if importing original
   saves.
8. **`a.a.f`** (the effect/overlay singleton used for the connect spinner,
   menu1.ui, npcEnemy.ui fades) — its API (`b(color,id)`, `a(...)`) is referenced
   but not fully documented here.

---

### Appendix — quick state/key reference

- **Top-level states** (`game/f.java::u`): 3 load → 15/16 logos → 6 SMS splash →
  7/8 title → 9/22 connect (+23 intro) → 11 world → 12/13 battle → 10 re-enter →
  19/20 debug; 2 pause, 1 quit.
- **Confirm** everywhere = `196640` (LSK/FIRE/'5'); **Back** = `262144` (RSK);
  **Up/Down/Left/Right** = `4100/8448/16400/32832`.
- **World**: LSK = world menu; FIRE(`65568`) = interact; arrows = walk.
- **Battle**: LEFT/RIGHT pick action; FIRE confirm; skill list UP/DOWN; RSK cancel.
- **Damage** = (ATK−DEF) × skillPower% × elementMult(×3 / ×0.6 / ×1) × crit(×1.5),
  min 1. **Catch** = HP/ball/grade/status/rarity product, 100% for ball 0.
  **EXP next** = 15·lvl²−200; **cap** lvl 50.
