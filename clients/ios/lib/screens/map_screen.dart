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

  @override
  void initState() {
    super.initState();
    widget.tcpService.onPacket = _handlePacket;
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
      Navigator.push(
        context,
        MaterialPageRoute(
          builder: (_) => BattleScreen(tcpService: widget.tcpService),
        ),
      ).then((_) {
        // Re-register packet handler when returning from battle
        widget.tcpService.onPacket = _handlePacket;
      });
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
            ],
          ),
        );
      },
    );
  }
}

class GameMapPainter extends CustomPainter {
  final int playerX;
  final int playerY;
  final int cols;
  final int rows;

  const GameMapPainter({
    required this.playerX,
    required this.playerY,
    required this.cols,
    required this.rows,
  });

  static const double tileSize = 40.0;

  @override
  void paint(Canvas canvas, Size size) {
    final paintA = Paint()..color = const Color(0xFF228B22);
    final paintB = Paint()..color = const Color(0xFF1A6B1A);
    final playerPaint = Paint()..color = Colors.cyan;

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

    // Draw player
    final px = playerX * tileSize + 4;
    final py = playerY * tileSize + 4;
    canvas.drawRect(
      Rect.fromLTWH(px, py, tileSize - 8, tileSize - 8),
      playerPaint,
    );
  }

  @override
  bool shouldRepaint(GameMapPainter oldDelegate) {
    return oldDelegate.playerX != playerX || oldDelegate.playerY != playerY;
  }
}
