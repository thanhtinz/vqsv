import 'package:flutter/foundation.dart';

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
  int battleEnemyLevel = 1;
  String battleEnemyName = '';
  String battleStatus = '';
  List<String> battleLog = [];
  bool catchable = false;

  void updateFromAuthJson(Map<String, dynamic> json) {
    token = json['token'] as String? ?? '';
    player = PlayerInfo.fromJson(json['player'] as Map<String, dynamic>);
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
