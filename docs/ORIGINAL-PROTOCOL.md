# ORIGINAL CLIENT NETWORK PROTOCOL

Reverse-engineered from the decompiled J2ME JAR
`e325eefd-vqsv360x640.jar` (CFR 0.152 decompile).

Game: **"Sủng vật Vương quốc - Liệt hỏa"** (a Chinese pet/monster RPG,
Vietnamese-localized by **BIGAME**). Build target: 360x640 MIDP.

Source root referenced below:
`/tmp/claude-0/-home-user-vqsv/d072df75-bc53-5059-acac-eff821a5f9f1/scratchpad/decompile/src`
(paths in this doc are relative to that root).

---

## 0. EXECUTIVE SUMMARY (the most important finding)

> **FACT: This client has NO game server. It is a fully client-side,
> single-player J2ME game. The only network I/O in the entire binary is
> outbound SMS, used for in-app billing/unlock.**

Consequences for the "100% match" server effort:

- There is **no TCP/socket game gateway, no HTTP API, no login handshake,
  no opcode/command protocol, no length-framing, no session key**, because
  the client never connects to a backend at all.
- All "save/load", "shop", "bank", "battle", "pet" mechanics run locally
  against MIDP `RecordStore` persistence and in-memory state.
- "Backend mechanics" that need to be matched are therefore **game rules
  and serialization formats**, not a wire protocol. The relevant formats
  are the local save format and the embedded data tables, summarized in §5.

This conclusion was reached by exhaustively scanning every `.java` file
for `Connector.open`, every stream class, every URL scheme, ports, IPs,
HTTP verbs/headers, and `HttpConnection`/`SocketConnection`/`StreamConnection`
imports. See §1 for the proof.

---

## 1. FACTS — Connection inventory (exhaustive)

### 1.1 Every `Connector.open(...)` call in the binary

There are exactly **two**, both returning a `javax.wireless.messaging.MessageConnection`
(SMS). There are **zero** `HttpConnection` / `SocketConnection` /
`StreamConnection` / `ContentConnection` references anywhere.

**(a) `d/a.java:268`** — exposed to the embedded Lua VM as native function `_ss`:
```java
// d/a.java, method a(c,int), case 13  (native name "_ss", see §4)
MessageConnection messageConnection = (MessageConnection)Connector.open((String)((String)c2.a(0)));
c2.a(messageConnection);
```
Opens whatever address string Lua passes. The companion native `_s`
(`d/a.java:249-255`, case 12) sends a `TextMessage`:
```java
MessageConnection messageConnection = (MessageConnection)c2.a(0);
TextMessage textMessage = (TextMessage)messageConnection.newMessage("text");
textMessage.setAddress((String)c2.a(1));
textMessage.setPayloadText((String)c2.a(2));
...MessageConnection.send((Message)textMessage);
messageConnection.close();
```

**(b) `e/b/a/a/b.java:64`** — the Java SMS sender thread (`e.b.a.a.b`,
`Runnable`). This is the path actually used by the in-game purchase UI:
```java
messageConnection = (MessageConnection)Connector.open((String)string);   // line 64
TextMessage textMessage = (TextMessage)messageConnection.newMessage("text");
textMessage.setAddress(string);          // line 66
textMessage.setPayloadText(null);        // line 67  (payload set elsewhere)
messageConnection.send((Message)textMessage);
...
messageConnection.close();
if (this.a != null) { this.a.a(true); }  // success callback
```

### 1.2 The only URL scheme literal in the binary

`a/a.java:353`:
```java
this.u = new e.b.a.a.b(this);
this.u.a("sms://");      // sets e.b.a.a.b.b = "sms://"
```
So the address passed to `Connector.open` in (b) is `"sms://" + <number>`
(see `e/b/a/a/b.java:62`, `string = this.b + string;`).

### 1.3 What is NOT present (verified absent)

- No `"http://"`, `"https://"`, `"ssl://"`, `"socket://"`, `"datagram://"`,
  `"btspp://"`, `"comm:"` literals.
- No IP-address or `:port` literals.
- No `HttpConnection`, `SocketConnection`, `StreamConnection`,
  `ServerSocketConnection`, `ContentConnection` types or imports.
- No `"GET "`, `"POST "`, `"Content-Length"`, `"User-Agent"` strings.
- Every `DataOutputStream`/`DataInputStream` in the tree is wired to a
  `ByteArrayOutputStream`/`ByteArrayInputStream` or a `RecordStore`/resource
  `InputStream` — never to a network connection's stream.

### 1.4 Files the task pointed at — what they actually are

- **`d/a.java`** — NOT game networking. It is the **base library of an
  embedded Lua interpreter** (a custom Lua VM lives in package `d/`). Its
  native functions include `print`, `type`, `tostring`, `setfenv`, `unpack`,
  `next`, and the SMS/messaging shims described in §4. (`d/a.java:42-93`
  registers the names; the big `switch(this.b)` at `d/a.java:95-333` is the
  native dispatch, indexed by an integer id, NOT a network opcode switch.)
- **`e/b/a/a/b.java`** — the **SMS sender background thread** (in-app
  billing). Note the legacy Chinese country-code handling: it strips a
  leading `+86` / `+` from numbers (`e/b/a/a/b.java:58`), evidence the
  original game was Chinese.
- **`game/m.java`** — the main game state class (`extends a.a`). Its many
  `DataOutputStream`/`DataInputStream` blocks (e.g. `m.a(k)` at line 518,
  `m.b(k)` at line 638) are **local save serialization** into a
  `RecordStore` wrapper (`a.b.e`), not network frames. See §5.
- **`a/a.java`** — the SMS/IAP purchase flow controller (the "Kích hoạt",
  "Tất trúc cầu", "Mua sắm kim tiền", etc. purchase dialogs).

---

## 2. FACTS — There is no handshake / login sequence

Because there is no server connection, there is **no login, no handshake,
no version exchange, no session key negotiation over the wire**.

App startup (`game/GameMIDLet.java`) only constructs the UI and shows it;
it performs no I/O:
```java
public GameMIDLet() {
    a = this;
    this.b = Display.getDisplay(this);
    this.c = n.a(this);
    this.b.setCurrent(this.c);
}
```

The closest thing to a "boot sequence with a key" is **local билing-config
decryption** (next section), which never leaves the device.

---

## 3. FACTS + INFERENCE — The only obfuscation step: SMS billing config

This is the one place real byte de-obfuscation happens. It decrypts the
embedded billing table, not network traffic.

### 3.1 Loading & decrypting `/l2.bin`  (`a.java:172-259`, class `a`)

FACTS (read directly):
1. If JAD property `sr` is absent, read the whole resource `/l2.bin` into a
   byte[] (`a.java:185-190`).
2. Hex-decode and reshape: `a.a(byte[])` (`a.java:57-71`) treats input as
   ASCII hex nibbles and packs two chars → one byte
   (`a.a(byte)` at `a.java:53-55` maps `'0'-'9'`→0-9, `'a'-'f'`→10-15).
3. A 6-byte key is taken from the first 3 and last 3 bytes of the raw
   payload (`a.java:204-207`).
4. XOR de-obfuscation `a.a(byte[],byte[])` (`a.java:87-100`): for each byte
   `i`, XOR it `(keyLen - i%3)` times against successive key bytes
   (`byArray[i] ^= byArray2[n6]` looped `n5 = 3 - i%3` times).
5. **Integrity check / checksum** (`a.java:213-228`): a rolling 5-byte
   accumulator seeded with `{1,2,5,7,4}` is XORed across the body
   (`acc[i%5] ^= body[i]`), then compared to the trailing 5 bytes of the
   decrypted blob. Mismatch → abort (billing disabled).
6. The validated body is decoded as UTF-8 (`a.b(byte[])`, `a.java:102-116`)
   and split on `'|'` (`a.java:233`).

### 3.2 Decrypted billing-table layout (`a.java:234-258`)

FACT — pipe-delimited fields, with `N` = count from field[0]:
```
field[0]              = N                              (item count, int)
field[1 .. N]         -> d[0..N-1]   : SMS PAYLOAD TEXT (keyword/content)
field[N+1 .. 2N]      -> f[0..N-1]   : PRICE            (int)
field[2N+1 .. 3N]     -> e[0..N-1]   : SMS DESTINATION  (short-code/address template)
```
So per purchasable item `n`: send SMS with **body = `d[n]`** to
**address = `e[n]`**, costing `f[n]`.

### 3.3 Address template substitution before send (`a.java:25-51`, `a.a(int)`)

FACT — the destination `e[n]` is a template; placeholders are filled from
JAD/app properties:
- `%1`  → app property `uid`   (default `"0"`)
- `%2`  → app property `Term`  (default `""`)
- `%cp` → app property `RefCode` (default `""`)
then trimmed and double-spaces collapsed. Result stored in static `a.b`
(address) and `a.a` (payload `= d[n]`).

INFERENCE: `/l2.bin` is the operator/billing provisioning file. To replicate
the original "unlock" mechanics, a server is not involved — the unlock is
purely client-side state flipped after the SMS "send succeeded" callback
(`a/a.java` `a(boolean)` at lines 276-347 applies the in-game reward, e.g.
gold/level/badge grants, locally).

---

## 4. FACTS — Embedded Lua VM native function ids (NOT network opcodes)

For completeness, since the task asked to map opcode→meaning. These are the
integer-indexed native functions of the embedded Lua interpreter
(`d/a.java`), registered by name in `a(g)` (`d/a.java:42-93`) and dispatched
by `switch(this.b)` in `a(c,int)` (`d/a.java:95-333`). **They are an
in-process scripting ABI, not a client↔server wire protocol.** The only
network-relevant ones are 12 and 13 (SMS).

| id | Lua name | meaning (from case body) | source |
|----|----------|--------------------------|--------|
| 0  | `pcall`  | protected call | d/a.java:97 |
| 1  | `print`  | print to stdout | d/a.java:100 |
| 2  | `select` | (no-op stub returns 0) | d/a.java:133 |
| 3  | `type`   | Lua type name of arg | d/a.java:136 |
| 4  | `tostring` | value → string | d/a.java:143 |
| 5  | `tonumber` | value → number (optional base) | d/a.java:151 |
| 6  | `error`  | (stub) | d/a.java:166 |
| 7  | `unpack` | unpack table | d/a.java:169 |
| 8  | `next`   | table iterator | d/a.java:172 |
| 9  | `setfenv` | set function env table | d/a.java:191 |
| 10 | `getfenv` | get function env table | d/a.java:216 |
| 11 | `rawequal` | identity compare | d/a.java:238 |
| 12 | `_s`     | **SMS send**: open MsgConn, set address+payload, send, close | d/a.java:245-264 |
| 13 | `_ss`    | **Open MessageConnection** from address string | d/a.java:265-279 |
| 14 | `_t`     | `System.currentTimeMillis() % INT_MAX` (timestamp) | d/a.java:280 |
| 15 | `_a`     | `TextMessage.getAddress()` | d/a.java:284 |
| 16 | `rp`     | string replace(s, from, to) | d/a.java:292 |
| 17 | `_cn`    | `obj.getClass()` | d/a.java:296 |
| 18 | `_r`     | random int in [a,b] | d/a.java:300 |
| 19 | `_o`     | first char of string → int (ord) | d/a.java:306 |
| 20 | `_c`     | int args → string (chr/concat) | d/a.java:310 |
| 21 | `_sp`    | `System.getProperty(name).toLowerCase()` | d/a.java:320 |
| 22 | `nc`     | returns `d + 10` (counter; `d` set to -10 in case 12) | d/a.java:328 |

The Connector class is also bound into Lua env as `_co`
(`Class.forName("javax.microedition.io.Connector")`, `d/a.java:82-92`),
so Lua scripts *could* in principle open other connection types — but no
bundled script does (the only schemes used at runtime are `sms://`).

INFERENCE: the Lua-side SMS shims (`_s`, `_ss`, `_a`, `_co`) duplicate the
Java SMS billing path. They appear to be a generic billing helper carried in
the engine; the active in-game purchase flow uses the Java class
`e.b.a.a.b` (§1.1b), not these.

---

## 5. INFERENCE/FACT — Local save serialization (the real "backend format")

Since gameplay state is what a re-host must reproduce, here is the local
save record format. This is **NOT a wire format** — it is written to MIDP
`RecordStore` via wrapper `a.b.e`.

### 5.1 RecordStore wrapper `a/b/e.java`
- A save slot is a `RecordStore` named `"dcn"+...` (see `a/a/c.java:60`
  pattern) or via `a.b.e(name, n)` (`game/m.java:514`, `ay[i]`).
- `a.b.e.a(ByteArrayOutputStream)` (line 89) writes a tagged blob; reader
  `a.b.e.a(int)` (line 57) recognizes a header where bytes `[1]==','(44)` and
  `[2]=='O'(79)`, then reads `byte, short, int length, length bytes` — i.e.
  a small framing of the *local* blob (tag + length), unrelated to sockets.

### 5.2 Main game-state blob — `game/m.java` `a(k)` writer (`m.java:518-636`)
Written in this exact order with `DataOutputStream` (big-endian, J2ME
`writeShort`/`writeInt`/`writeByte`/`writeBoolean`/`writeLong`/`writeUTF`):
1. `short` x (`k.j` or special-cased 240 / map coords)  — m.java:526/530/534
2. `short` y (`k.k` / 40)                                — m.java:527/531/535
3. `byte`  dir/flag (`k.o` / 2 / 0)                      — m.java:528/532/536
4. `byte[][] k.C` flattened (map A grid)                 — m.java:538-542
5. `byte[]  k.Q`                                          — m.java:543-545
6. `byte[][] k.D` flattened                              — m.java:546-550
7. `byte[]  k.F`                                          — m.java:551-553
8. `byte[][] k.E` flattened (from `this.n.E`)            — m.java:554-558
9. `byte` n.I, `byte` n.H, `byte` n.G, `byte` n.J        — m.java:559-562
10. `byte[] n.S`                                          — m.java:563-565
11. (sub-blobs via F()/Z()/X()/V() — separate records)   — m.java:566-602
12. `int` count + nested `int[]` lists for k.M, k.N, k.O  — m.java:575-598
    (each: `int size`, then per entry `int len` + `int[]` body)
13. `boolean[] k.U`                                        — m.java:599-601
14. `byte` Q.size + per entry `byte a.d.d(string)`        — m.java:608-612
15. `boolean[] this.bb`                                    — m.java:613-615
16. `byte` selected-pet id (`z.a.a`) or `-1`              — m.java:616-620
17. `byte[] n.v` (raw write)                              — m.java:621
18. `int` B                                               — m.java:622
19. `boolean` V                                           — m.java:623
20. `long` (f.o + f.p - f.q) — a time/elapsed value       — m.java:624-625
21. `byte` k.u                                            — m.java:626

The matching reader `b(k)` (`m.java:638-...`) reads the same sequence in the
same order. Other paired writer/reader blocks in `m.java` (lines 755, 866,
957, 997, 1026, 1059, 1103, 1147, ...) serialize the sub-structures
referenced in step 11 (F/Z/X/V) the same way (big-endian Data* into
`ByteArrayOutputStream`, stored as additional RecordStore records).

> NOTE: This section is provided because the task's real goal is to match
> "backend mechanics." If the new server must accept/return save blobs (e.g.
> a cloud-save shim), these are the byte layouts. If the new server is a
> *game* server, none of this is a network protocol and a wire format would
> have to be designed fresh — the original client cannot speak it.

---

## 6. Opcode count recovered

- **Client→Server game opcodes: 0** (no server connection exists).
- **Server→Client game opcodes: 0** (same).
- **Outbound message types: 1** — SMS `TextMessage` (`sms://<number>`,
  payload = billing keyword), used for IAP only.
- **Embedded Lua native function ids: 23** (ids 0–22; only ids 12,13,15 touch
  the messaging API). Documented in §4 for completeness, but these are an
  in-process scripting ABI, not a network protocol.

## 7. Items not fully decoded / open questions

- The exact contents of `/l2.bin` (the encrypted billing table) were not
  dumped here; the decryption algorithm is fully recovered (§3) but the
  resource bytes live in the JAR, not the decompiled `.java` sources. To get
  the concrete keywords/short-codes/prices, run the §3 algorithm against the
  JAR's `/l2.bin` (or read JAD properties `sr`, `sr1..srN`, `uid`, `Term`,
  `RefCode`).
- The semantics of the sub-blobs in `m.java` steps 11 (F/Z/X/V → pet/bag/
  quest/etc. records) are serialized faithfully above but their field
  meanings (which byte is HP vs level, etc.) were not individually decoded;
  they require cross-referencing `game/k.java` (the `k` player class) and
  `game/i.java` (pet class) field-by-field.
- Several CFR-flagged methods ("Unable to fully structure code": `game.a.a()`,
  `game.a.K()`, `game.m.ag()`, `c.c.a(Graphics)`) are rendering/UI, not
  network, so they do not affect this protocol analysis.

---

### Provenance note
All "FACT" lines above were read directly from the cited
`<class>.java:<line>`. "INFERENCE" lines are reasoned conclusions. The
central claim — no game server, SMS-only networking — was independently
verified by a full-tree scan for all connection/stream/URL/HTTP indicators
(zero hits except the two SMS `Connector.open` calls in §1.1).
