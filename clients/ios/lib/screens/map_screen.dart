import 'dart:typed_data';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/game_state.dart';
import '../services/tcp_service.dart';
import 'battle_screen.dart';

class MapScreen extends StatefulWidget {
  final TcpService tcpService;
  const MapScreen({super.key, required this.tcpService});

  @override
  State<MapScreen> createState() => _MapScreenState();
}

class _MapScreenState extends State<MapScreen> {
  static const _mapCols = 20;
  static const _mapRows = 12;

  final _chatCtrl = TextEditingController();
  bool _chatOpen = false;

  @override
  void dispose() {
    _chatCtrl.dispose();
    super.dispose();
  }

  void _sendChat() {
    final text = _chatCtrl.text.trim();
    if (text.isEmpty) return;
    widget.tcpService.sendChat(text);
    _chatCtrl.clear();
  }

  @override
  void initState() {
    super.initState();
    widget.tcpService.onPacket = _handlePacket;
    _wireEvents();
  }

  /// Register typed event handlers for the social/PvP server messages. These
  /// run in addition to the raw [onPacket] handler used for movement and wild
  /// encounters. Clears the login screen's stale AUTH_OK/error closures.
  void _wireEvents() {
    final tcp = widget.tcpService;
    tcp.onAuthOk = null; // login is done; avoid stale closures firing
    tcp.onPlayerNear = (p) {
      if (!mounted) return;
      context
          .read<GameState>()
          .updatePlayerNear(
            NearbyPlayer(
                id: p.playerId, name: p.name, mapId: p.mapId, x: p.x, y: p.y),
            p.present,
          );
    };
    tcp.onChat = (name, text) {
      if (!mounted) return;
      context.read<GameState>().addChat(name, text);
    };
    tcp.onError = (msg) {
      if (!mounted) return;
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text(msg)));
    };
    tcp.onPvpInvite = (challengerId, name) {
      if (!mounted) return;
      context.read<GameState>().setPvpInvite(PvpInvite(challengerId, name));
      _showPvpInviteDialog(challengerId, name);
    };
    tcp.onPvpStart = (start) {
      if (!mounted) return;
      context.read<GameState>().setPvpBattle(
            start.battleId,
            start.oppName,
            start.myHp,
            start.oppHp,
          );
      _openBattle();
    };
  }

  void _showPvpInviteDialog(int challengerId, String name) {
    showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: const Color(0xFF1A1A3A),
        title: const Text('Lời mời PvP',
            style: TextStyle(color: Colors.white)),
        content: Text('$name muốn thách đấu bạn!',
            style: const TextStyle(color: Colors.white70)),
        actions: [
          TextButton(
            onPressed: () {
              widget.tcpService.sendPvpRespond(challengerId, false);
              context.read<GameState>().setPvpInvite(null);
              Navigator.pop(ctx);
            },
            child: const Text('Từ chối'),
          ),
          ElevatedButton(
            onPressed: () {
              widget.tcpService.sendPvpRespond(challengerId, true);
              Navigator.pop(ctx);
              // PVP_START will follow and open the battle screen.
            },
            child: const Text('Chấp nhận'),
          ),
        ],
      ),
    );
  }

  void _openBattle() {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (_) => BattleScreen(tcpService: widget.tcpService),
      ),
    ).then((_) {
      // Re-register handlers when returning from battle.
      widget.tcpService.onPacket = _handlePacket;
      _wireEvents();
    });
  }

  void _handlePacket(int op, Uint8List payload) {
    if (!mounted) return;
    final gs = context.read<GameState>();
    if (op == 0x83) {
      // MOVE_OK: [1B newX][1B newY]
      if (payload.length >= 2) {
        gs.updatePosition(payload[0], payload[1]);
      }
    } else if (op == 0x84) {
      // WILD_ENC: [1B x][1B y][2B battleIdLen][battleId][2B nameLen][name][1B level][2B hp][1B catchable]
      if (payload.length < 2) return;
      final x = payload[0];
      final y = payload[1];
      int offset = 2;
      if (payload.length < offset + 2) return;
      final bidLen = (payload[offset] << 8) | payload[offset + 1];
      offset += 2;
      if (payload.length < offset + bidLen) return;
      final battleId = utf8.decode(payload.sublist(offset, offset + bidLen));
      offset += bidLen;
      if (payload.length < offset + 2) return;
      final nameLen = (payload[offset] << 8) | payload[offset + 1];
      offset += 2;
      if (payload.length < offset + nameLen) return;
      final name = utf8.decode(payload.sublist(offset, offset + nameLen));
      offset += nameLen;
      if (payload.length < offset + 4) return;
      final level = payload[offset];
      offset += 1;
      final hp = (payload[offset] << 8) | payload[offset + 1];
      offset += 2;
      final isCatchable = payload[offset] != 0;
      gs.setWildEncounter(x, y, battleId, name, level, hp, isCatchable);
      _openBattle();
    }
  }

  void _onSwipeVertical(DragEndDetails details) {
    if (details.primaryVelocity == null) return;
    if (details.primaryVelocity! < 0) {
      widget.tcpService.sendMove(0); // UP
    } else {
      widget.tcpService.sendMove(1); // DOWN
    }
  }

  void _onSwipeHorizontal(DragEndDetails details) {
    if (details.primaryVelocity == null) return;
    if (details.primaryVelocity! < 0) {
      widget.tcpService.sendMove(2); // LEFT
    } else {
      widget.tcpService.sendMove(3); // RIGHT
    }
  }

  @override
  Widget build(BuildContext context) {
    return Consumer<GameState>(
      builder: (context, gs, _) {
        final player = gs.player;
        return Scaffold(
          backgroundColor: const Color(0xFF0A0A25),
          appBar: AppBar(
            backgroundColor: const Color(0xFF1A1A3A),
            title: Text(
              player != null
                  ? '${player.name}  Lv.${player.level}  金: ${player.kimTien}'
                  : 'VQSV',
              style: const TextStyle(fontSize: 14, color: Colors.white),
            ),
            automaticallyImplyLeading: false,
            actions: [
              IconButton(
                tooltip: 'Trò chuyện',
                icon: Stack(
                  clipBehavior: Clip.none,
                  children: [
                    const Icon(Icons.chat_bubble_outline, color: Colors.white),
                    if (gs.chatLog.isNotEmpty && !_chatOpen)
                      const Positioned(
                        right: -2,
                        top: -2,
                        child: Icon(Icons.circle,
                            size: 8, color: Colors.lightBlueAccent),
                      ),
                  ],
                ),
                onPressed: () => setState(() => _chatOpen = !_chatOpen),
              ),
            ],
          ),
          body: Column(
            children: [
              Expanded(
                child: GestureDetector(
                  onVerticalDragEnd: _onSwipeVertical,
                  onHorizontalDragEnd: _onSwipeHorizontal,
                  child: CustomPaint(
                    painter: GameMapPainter(
                      playerX: player?.posX ?? 0,
                      playerY: player?.posY ?? 0,
                      cols: _mapCols,
                      rows: _mapRows,
                      others: gs.nearbyPlayers.values
                          .where((p) =>
                              player == null || p.mapId == player.mapId)
                          .toList(),
                    ),
                    child: const SizedBox.expand(),
                  ),
                ),
              ),
              Container(
                color: const Color(0xFF1A1A3A),
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: Row(
                  children: [
                    const Text(
                      'HP: ',
                      style: TextStyle(color: Colors.white70, fontSize: 13),
                    ),
                    Expanded(
                      child: LinearProgressIndicator(
                        value: (player != null && player.hpMax > 0)
                            ? player.hp / player.hpMax
                            : 1.0,
                        backgroundColor: Colors.white24,
                        color: Colors.green,
                        minHeight: 12,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      player != null ? '${player.hp}/${player.hpMax}' : '-',
                      style:
                          const TextStyle(color: Colors.white70, fontSize: 13),
                    ),
                  ],
                ),
              ),
              if (_chatOpen) _buildChatPanel(gs),
            ],
          ),
        );
      },
    );
  }

  Widget _buildChatPanel(GameState gs) {
    final messages = gs.chatLog;
    return Container(
      height: 200,
      color: const Color(0xFF12122E),
      child: Column(
        children: [
          Expanded(
            child: ListView.builder(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              itemCount: messages.length,
              itemBuilder: (_, i) {
                final m = messages[i];
                return Padding(
                  padding: const EdgeInsets.only(bottom: 2),
                  child: RichText(
                    text: TextSpan(
                      style: const TextStyle(fontSize: 13),
                      children: [
                        TextSpan(
                          text: '${m.name}: ',
                          style: const TextStyle(
                            color: Colors.lightBlueAccent,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        TextSpan(
                          text: m.text,
                          style: const TextStyle(color: Colors.white70),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
          ),
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
            child: Row(
              children: [
                Expanded(
                  child: TextField(
                    controller: _chatCtrl,
                    style: const TextStyle(color: Colors.white, fontSize: 13),
                    onSubmitted: (_) => _sendChat(),
                    decoration: const InputDecoration(
                      isDense: true,
                      hintText: 'Nhập tin nhắn...',
                      hintStyle: TextStyle(color: Colors.white38),
                      enabledBorder: OutlineInputBorder(
                        borderSide: BorderSide(color: Colors.white24),
                      ),
                      focusedBorder: OutlineInputBorder(
                        borderSide: BorderSide(color: Colors.blue),
                      ),
                    ),
                  ),
                ),
                IconButton(
                  icon: const Icon(Icons.send, color: Colors.lightBlueAccent),
                  onPressed: _sendChat,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class GameMapPainter extends CustomPainter {
  final int playerX;
  final int playerY;
  final int cols;
  final int rows;
  final List<NearbyPlayer> others;

  const GameMapPainter({
    required this.playerX,
    required this.playerY,
    required this.cols,
    required this.rows,
    this.others = const [],
  });

  static const double tileSize = 40.0;

  @override
  void paint(Canvas canvas, Size size) {
    final paintA = Paint()..color = const Color(0xFF228B22);
    final paintB = Paint()..color = const Color(0xFF1A6B1A);
    final playerPaint = Paint()..color = Colors.cyan;
    final otherPaint = Paint()..color = Colors.orangeAccent;

    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; col++) {
        final rect = Rect.fromLTWH(
          col * tileSize,
          row * tileSize,
          tileSize,
          tileSize,
        );
        canvas.drawRect(rect, (row + col) % 2 == 0 ? paintA : paintB);
      }
    }

    // Draw other players first so the local player renders on top.
    for (final o in others) {
      canvas.drawRect(
        Rect.fromLTWH(
            o.x * tileSize + 4, o.y * tileSize + 4, tileSize - 8, tileSize - 8),
        otherPaint,
      );
      _drawName(canvas, o.name, o.x * tileSize, o.y * tileSize);
    }

    // Draw local player
    final px = playerX * tileSize + 4;
    final py = playerY * tileSize + 4;
    canvas.drawRect(
      Rect.fromLTWH(px, py, tileSize - 8, tileSize - 8),
      playerPaint,
    );
  }

  void _drawName(Canvas canvas, String name, double tileLeft, double tileTop) {
    if (name.isEmpty) return;
    final tp = TextPainter(
      text: TextSpan(
        text: name,
        style: const TextStyle(color: Colors.white, fontSize: 10),
      ),
      textDirection: TextDirection.ltr,
    )..layout();
    tp.paint(
      canvas,
      Offset(tileLeft + (tileSize - tp.width) / 2, tileTop - 12),
    );
  }

  @override
  bool shouldRepaint(GameMapPainter oldDelegate) {
    return oldDelegate.playerX != playerX ||
        oldDelegate.playerY != playerY ||
        !identical(oldDelegate.others, others);
  }
}
