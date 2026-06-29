import 'package:flutter/foundation.dart';

/// A nearby (other) player tracked from PLAYER_NEAR broadcasts.
class NearbyPlayer {
  final int id;
  final String name;
  final int mapId;
  final int x;
  final int y;
  const NearbyPlayer({
    required this.id,
    required this.name,
    required this.mapId,
    required this.x,
    required this.y,
  });
}

/// A single chat line received from CHAT_MSG.
class ChatMessage {
  final String name;
  final String text;
  const ChatMessage(this.name, this.text);
}

/// A pending PvP invite (PVP_INVITE) awaiting accept/decline.
class PvpInvite {
  final int challengerId;
  final String name;
  const PvpInvite(this.challengerId, this.name);
}

class PlayerInfo {
  final int id;
  final String name;
  final int level;
  final int exp;
  final int expNext;
  final int kimTien;
  final int huyChuong;
  final int mapId;
  final int posX;
  final int posY;
  final int hp;
  final int hpMax;

  const PlayerInfo({
    required this.id,
    required this.name,
    required this.level,
    required this.exp,
    required this.expNext,
    required this.kimTien,
    required this.huyChuong,
    required this.mapId,
    required this.posX,
    required this.posY,
    required this.hp,
    required this.hpMax,
  });

  factory PlayerInfo.fromJson(Map<String, dynamic> json) {
    return PlayerInfo(
      id: (json['id'] as num? ?? 0).toInt(),
      name: json['name'] as String? ?? '',
      level: (json['level'] as num? ?? 0).toInt(),
      exp: (json['exp'] as num? ?? 0).toInt(),
      expNext: (json['expNext'] as num? ?? 0).toInt(),
      kimTien: (json['kimTien'] as num? ?? 0).toInt(),
      huyChuong: (json['huyChuong'] as num? ?? 0).toInt(),
      mapId: (json['mapId'] as num? ?? 0).toInt(),
      posX: (json['posX'] as num? ?? 0).toInt(),
      posY: (json['posY'] as num? ?? 0).toInt(),
      hp: (json['hp'] as num? ?? 0).toInt(),
      hpMax: (json['hpMax'] as num? ?? 0).toInt(),
    );
  }

  PlayerInfo copyWith({
    int? id,
    String? name,
    int? level,
    int? exp,
    int? expNext,
    int? kimTien,
    int? huyChuong,
    int? mapId,
    int? posX,
    int? posY,
    int? hp,
    int? hpMax,
  }) {
    return PlayerInfo(
      id: id ?? this.id,
      name: name ?? this.name,
      level: level ?? this.level,
      exp: exp ?? this.exp,
      expNext: expNext ?? this.expNext,
      kimTien: kimTien ?? this.kimTien,
      huyChuong: huyChuong ?? this.huyChuong,
      mapId: mapId ?? this.mapId,
      posX: posX ?? this.posX,
      posY: posY ?? this.posY,
      hp: hp ?? this.hp,
      hpMax: hpMax ?? this.hpMax,
    );
  }
}

class GameState extends ChangeNotifier {
  String token = '';
  PlayerInfo? player;
  String? activeBattleId;
  int battlePlayerHp = 0;
  int battleEnemyHp = 0;
  // Captured once at the start of a battle so the enemy HP bar can drain
  // against a fixed maximum instead of the current HP each turn.
  int battleEnemyMaxHp = 1;
  int battleEnemyLevel = 1;
  String battleEnemyName = '';
  String battleStatus = '';
  List<String> battleLog = [];
  bool catchable = false;

  // Other players currently visible, keyed by playerId (from PLAYER_NEAR).
  final Map<int, NearbyPlayer> nearbyPlayers = {};
  // Rolling chat log (from CHAT_MSG).
  final List<ChatMessage> chatLog = [];
  // Most recent unanswered PvP invite (from PVP_INVITE), if any.
  PvpInvite? pendingPvpInvite;

  void updateFromAuthJson(Map<String, dynamic> json) {
    token = json['token'] as String? ?? '';
    player = PlayerInfo.fromJson(json['player'] as Map<String, dynamic>);
    notifyListeners();
  }

  /// Apply the binary AUTH_OK body. The HTTP login already populated the full
  /// player profile; here we just reconcile the authoritative fields the TCP
  /// server reports (token, level, kimTien, position).
  void applyAuthOk(
    String token,
    int level,
    int kimTien,
    int mapId,
    int posX,
    int posY,
  ) {
    if (token.isNotEmpty) this.token = token;
    player = player?.copyWith(
      level: level,
      kimTien: kimTien,
      mapId: mapId,
      posX: posX,
      posY: posY,
    );
    notifyListeners();
  }

  void updatePlayerNear(NearbyPlayer p, bool present) {
    if (present) {
      nearbyPlayers[p.id] = p;
    } else {
      nearbyPlayers.remove(p.id);
    }
    notifyListeners();
  }

  void addChat(String name, String text) {
    chatLog.add(ChatMessage(name, text));
    // Keep the log bounded.
    if (chatLog.length > 100) chatLog.removeAt(0);
    notifyListeners();
  }

  void setPvpInvite(PvpInvite? invite) {
    pendingPvpInvite = invite;
    notifyListeners();
  }

  /// Set up battle state from a PVP_START frame.
  void setPvpBattle(
    String battleId,
    String oppName,
    int myHp,
    int oppHp,
  ) {
    activeBattleId = battleId;
    battleEnemyName = oppName;
    battleEnemyLevel = 0; // unknown for PvP
    battleEnemyHp = oppHp;
    battleEnemyMaxHp = oppHp > 0 ? oppHp : 1;
    battlePlayerHp = myHp;
    catchable = false;
    battleLog = [];
    battleStatus = '';
    pendingPvpInvite = null;
    notifyListeners();
  }

  void updatePosition(int x, int y) {
    player = player?.copyWith(posX: x, posY: y);
    notifyListeners();
  }

  void setWildEncounter(
    int x,
    int y,
    String battleId,
    String name,
    int level,
    int hp,
    bool isCatchable,
  ) {
    activeBattleId = battleId;
    battleEnemyName = name;
    battleEnemyLevel = level;
    battleEnemyHp = hp;
    battleEnemyMaxHp = hp > 0 ? hp : 1;
    catchable = isCatchable;
    battleLog = [];
    battleStatus = '';
    player = player?.copyWith(posX: x, posY: y);
    notifyListeners();
  }

  void updateBattle(int pHp, int eHp, String status, String log) {
    battlePlayerHp = pHp;
    battleEnemyHp = eHp;
    battleStatus = status;
    battleLog.add(log);
    notifyListeners();
  }

  void clearBattle() {
    activeBattleId = null;
    battleLog.clear();
    battleStatus = '';
    notifyListeners();
  }
}
