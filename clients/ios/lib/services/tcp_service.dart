import 'dart:io';
import 'dart:typed_data';
import 'dart:convert';

/// Binary protocol opcodes. Mirrors the server (TcpGateway.kt) and the
/// reference LibGDX core client (Op.kt). All integers are BIG-ENDIAN; every
/// frame on the wire is [2B total length][1B opcode][payload].
class Op {
  // Client -> Server
  static const int login = 0x01;
  static const int move = 0x02;
  static const int battleAct = 0x03;
  static const int chat = 0x04;
  static const int ping = 0x05;
  static const int pvpChallenge = 0x08;
  static const int pvpRespond = 0x09;
  static const int startTrainer = 0x0A;

  // Server -> Client
  static const int authOk = 0x81;
  static const int moveOk = 0x83;
  static const int wildEnc = 0x84;
  static const int battleTurn = 0x85;
  static const int playerNear = 0x86;
  static const int chatMsg = 0x87;
  static const int pong = 0x88;
  static const int pvpInvite = 0x89;
  static const int pvpStart = 0x8A;
  static const int error = 0xFF;
}

/// Parsed AUTH_OK body.
class AuthOk {
  final String token;
  final int level;
  final int kimTien;
  final int mapId;
  final int posX;
  final int posY;
  AuthOk(this.token, this.level, this.kimTien, this.mapId, this.posX,
      this.posY);
}

/// Parsed WILD_ENC body.
class WildEncounter {
  final int x;
  final int y;
  final String battleId;
  final String name;
  final int level;
  final int hp;
  final bool catchable;
  final int spriteId;
  WildEncounter(this.x, this.y, this.battleId, this.name, this.level, this.hp,
      this.catchable, this.spriteId);
}

/// Parsed PLAYER_NEAR body.
class PlayerNear {
  final int playerId;
  final bool present;
  final int mapId;
  final int x;
  final int y;
  final String name;
  PlayerNear(this.playerId, this.present, this.mapId, this.x, this.y, this.name);
}

/// Parsed PVP_START body.
class PvpStart {
  final String battleId;
  final String oppName;
  final int myHp;
  final int oppHp;
  final int oppSpriteId;
  PvpStart(this.battleId, this.oppName, this.myHp, this.oppHp, this.oppSpriteId);
}

/// Small cursor over a payload for big-endian reads.
class _Reader {
  final Uint8List data;
  int pos = 0;
  _Reader(this.data);

  bool has(int n) => pos + n <= data.length;

  int u8() => data[pos++] & 0xFF;

  int u16() {
    final hi = data[pos++] & 0xFF;
    final lo = data[pos++] & 0xFF;
    return (hi << 8) | lo;
  }

  int u32() {
    final b0 = data[pos++] & 0xFF;
    final b1 = data[pos++] & 0xFF;
    final b2 = data[pos++] & 0xFF;
    final b3 = data[pos++] & 0xFF;
    return (b0 << 24) | (b1 << 16) | (b2 << 8) | b3;
  }

  String str(int n) {
    final s = utf8.decode(data.sublist(pos, pos + n));
    pos += n;
    return s;
  }
}

class TcpService {
  Socket? _socket;
  final _buffer = BytesBuilder();

  /// Raw packet callback (opcode, payload-after-opcode). Kept for existing
  /// screens that parse packets themselves (map/battle screens).
  Function(int opcode, Uint8List payload)? onPacket;

  // Typed event callbacks. Any subset may be set; all are optional.
  void Function(AuthOk auth)? onAuthOk;
  void Function(int x, int y)? onMoveOk;
  void Function(WildEncounter enc)? onWildEncounter;
  void Function(int playerHp, int enemyHp, String status, String log)?
      onBattleTurn;
  void Function(PlayerNear p)? onPlayerNear;
  void Function(String name, String text)? onChat;
  void Function()? onPong;
  void Function(int challengerId, String name)? onPvpInvite;
  void Function(PvpStart start)? onPvpStart;
  void Function(String msg)? onError;

  Future<void> connect(String host, int port) async {
    _socket = await Socket.connect(host, port);
    _socket!.listen(
      _onData,
      onError: (Object e) => print('TCP error: $e'),
      onDone: () => print('TCP closed'),
    );
  }

  void _onData(List<int> data) {
    _buffer.add(data);
    _processBuffer();
  }

  void _processBuffer() {
    while (true) {
      final bytes = _buffer.toBytes();
      if (bytes.length < 2) {
        _buffer.clear();
        _buffer.add(bytes);
        return;
      }
      final len = ((bytes[0] << 8) | bytes[1]);
      if (bytes.length < 2 + len) {
        _buffer.clear();
        _buffer.add(bytes);
        return;
      }
      final frame = Uint8List.fromList(bytes.sublist(2, 2 + len));
      _buffer.clear();
      if (bytes.length > 2 + len) {
        _buffer.add(bytes.sublist(2 + len));
      }
      if (frame.isNotEmpty) {
        final opcode = frame[0] & 0xFF;
        final payload = Uint8List.fromList(frame.sublist(1));
        // Raw callback first (existing screens rely on it).
        onPacket?.call(opcode, payload);
        _dispatch(opcode, payload);
      }
    }
  }

  void _dispatch(int opcode, Uint8List payload) {
    final r = _Reader(payload);
    try {
      switch (opcode) {
        case Op.authOk:
          // [2B tokenLen][token][2B level][4B kimTien][2B mapId][1B x][1B y]
          if (!r.has(2)) return;
          final token = r.str(r.u16());
          if (!r.has(2 + 4 + 2 + 1 + 1)) return;
          final level = r.u16();
          final kimTien = r.u32();
          final mapId = r.u16();
          final posX = r.u8();
          final posY = r.u8();
          onAuthOk?.call(AuthOk(token, level, kimTien, mapId, posX, posY));
          break;
        case Op.moveOk:
          // [1B x][1B y]
          if (!r.has(2)) return;
          onMoveOk?.call(r.u8(), r.u8());
          break;
        case Op.wildEnc:
          // [1B x][1B y][2B bidLen][battleId][2B nameLen][name]
          // [1B level][2B hp][1B catchable][2B spriteId]
          if (!r.has(2)) return;
          final x = r.u8();
          final y = r.u8();
          if (!r.has(2)) return;
          final battleId = r.str(r.u16());
          if (!r.has(2)) return;
          final name = r.str(r.u16());
          if (!r.has(1 + 2 + 1 + 2)) return;
          final level = r.u8();
          final hp = r.u16();
          final catchable = r.u8() != 0;
          final spriteId = r.u16();
          onWildEncounter?.call(
              WildEncounter(x, y, battleId, name, level, hp, catchable, spriteId));
          break;
        case Op.battleTurn:
          // [2B playerHp][2B enemyHp][1B statusLen][status][2B logLen][log]
          if (!r.has(2 + 2 + 1)) return;
          final playerHp = r.u16();
          final enemyHp = r.u16();
          final status = r.str(r.u8());
          if (!r.has(2)) return;
          final log = r.str(r.u16());
          onBattleTurn?.call(playerHp, enemyHp, status, log);
          break;
        case Op.playerNear:
          // [4B playerId][1B present][2B mapId][1B x][1B y][2B nameLen][name]
          if (!r.has(4 + 1 + 2 + 1 + 1 + 2)) return;
          final playerId = r.u32();
          final present = r.u8() != 0;
          final mapId = r.u16();
          final px = r.u8();
          final py = r.u8();
          final name = r.str(r.u16());
          onPlayerNear
              ?.call(PlayerNear(playerId, present, mapId, px, py, name));
          break;
        case Op.chatMsg:
          // [2B nameLen][name][2B textLen][text]
          if (!r.has(2)) return;
          final name = r.str(r.u16());
          if (!r.has(2)) return;
          final text = r.str(r.u16());
          onChat?.call(name, text);
          break;
        case Op.pong:
          onPong?.call();
          break;
        case Op.pvpInvite:
          // [4B challengerId][2B nameLen][name]
          if (!r.has(4 + 2)) return;
          final challengerId = r.u32();
          final name = r.str(r.u16());
          onPvpInvite?.call(challengerId, name);
          break;
        case Op.pvpStart:
          // [2B bidLen][battleId][2B oppNameLen][oppName][2B myHp][2B oppHp][2B oppSpriteId]
          if (!r.has(2)) return;
          final battleId = r.str(r.u16());
          if (!r.has(2)) return;
          final oppName = r.str(r.u16());
          if (!r.has(2 + 2 + 2)) return;
          final myHp = r.u16();
          final oppHp = r.u16();
          final oppSpriteId = r.u16();
          onPvpStart
              ?.call(PvpStart(battleId, oppName, myHp, oppHp, oppSpriteId));
          break;
        case Op.error:
          // [2B msgLen][msg]
          if (!r.has(2)) return;
          final msg = r.str(r.u16());
          onError?.call(msg);
          break;
      }
    } catch (e) {
      print('TCP parse error (op 0x${opcode.toRadixString(16)}): $e');
    }
  }

  void _send(Uint8List payload) {
    if (_socket == null) return;
    final header = ByteData(2);
    header.setUint16(0, payload.length, Endian.big);
    _socket!.add(header.buffer.asUint8List());
    _socket!.add(payload);
  }

  // ---- Client -> Server ----

  void sendLogin(String username, String password) {
    final u = utf8.encode(username);
    final p = utf8.encode(password);
    final all = Uint8List(1 + 2 + u.length + 2 + p.length);
    int i = 0;
    all[i++] = Op.login;
    all[i++] = (u.length >> 8) & 0xFF;
    all[i++] = u.length & 0xFF;
    all.setRange(i, i + u.length, u);
    i += u.length;
    all[i++] = (p.length >> 8) & 0xFF;
    all[i++] = p.length & 0xFF;
    all.setRange(i, i + p.length, p);
    _send(all);
  }

  void sendMove(int dir) => _send(Uint8List.fromList([Op.move, dir & 0xFF]));

  void sendBattleAct(String battleId, int action, [int? itemId]) {
    final bid = utf8.encode(battleId);
    final extra = itemId != null ? 2 : 0;
    final all = Uint8List(1 + 2 + bid.length + 1 + extra);
    int i = 0;
    all[i++] = Op.battleAct;
    all[i++] = (bid.length >> 8) & 0xFF;
    all[i++] = bid.length & 0xFF;
    all.setRange(i, i + bid.length, bid);
    i += bid.length;
    all[i++] = action & 0xFF;
    if (itemId != null) {
      all[i++] = (itemId >> 8) & 0xFF;
      all[i] = itemId & 0xFF;
    }
    _send(all);
  }

  void sendChat(String text) {
    // Server truncates to 128 bytes; truncate by encoded bytes to stay aligned.
    var t = utf8.encode(text);
    if (t.length > 128) t = t.sublist(0, 128);
    final all = Uint8List(1 + 2 + t.length);
    all[0] = Op.chat;
    all[1] = (t.length >> 8) & 0xFF;
    all[2] = t.length & 0xFF;
    all.setRange(3, 3 + t.length, t);
    _send(all);
  }

  void sendPing() => _send(Uint8List.fromList([Op.ping]));

  void sendPvpChallenge(int targetPlayerId) {
    final all = Uint8List(1 + 4);
    all[0] = Op.pvpChallenge;
    all[1] = (targetPlayerId >> 24) & 0xFF;
    all[2] = (targetPlayerId >> 16) & 0xFF;
    all[3] = (targetPlayerId >> 8) & 0xFF;
    all[4] = targetPlayerId & 0xFF;
    _send(all);
  }

  void sendPvpRespond(int challengerId, bool accept) {
    final all = Uint8List(1 + 4 + 1);
    all[0] = Op.pvpRespond;
    all[1] = (challengerId >> 24) & 0xFF;
    all[2] = (challengerId >> 16) & 0xFF;
    all[3] = (challengerId >> 8) & 0xFF;
    all[4] = challengerId & 0xFF;
    all[5] = accept ? 1 : 0;
    _send(all);
  }

  void sendStartTrainer([int npcId = 0]) {
    final all = Uint8List(1 + 2);
    all[0] = Op.startTrainer;
    all[1] = (npcId >> 8) & 0xFF;
    all[2] = npcId & 0xFF;
    _send(all);
  }

  void disconnect() {
    _socket?.destroy();
    _socket = null;
  }
}
